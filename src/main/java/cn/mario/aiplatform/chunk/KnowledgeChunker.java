package cn.mario.aiplatform.chunk;

import org.springframework.ai.document.Document;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;

/**
 * documents
 * │
 * ├── Document 1
 * │   ├── text = "第一章 Spring AI 简介..."
 * │   └── metadata = {page=1}
 * │
 * ├── Document 2
 * │   ├── text = "第二章 ChatClient..."
 * │   └── metadata = {page=2}
 * │
 * └── Document 3
 *     ├── text = "第三章 VectorStore..."
 *     └── metadata = {page=3}
 *
 * TokenTextSplitter 是把大的 document 切成小一点的 document
 * Chunk 1
 * metadata:
 * source = spring-ai.pdf
 * page = 10
 *
 * Chunk 2
 * metadata:
 * source = spring-ai.pdf
 * page = 10
 *
 * Chunk 3
 * metadata:
 * source = spring-ai.pdf
 * page = 10
 * @auther: mario
 */
@Component
public class KnowledgeChunker {

    /**
     * 最终 Token Chunk 大小
     */
    private static final int TOKEN_CHUNK_SIZE = 600;

    /**
     * 一个 Section 在字符层面的最大推荐长度。
     *
     * 注意：
     * 这里只是为了快速判断是否需要进一步切分。
     * 真正最终的长度控制交给 TokenTextSplitter。
     */
    private static final int MAX_SECTION_CHARS = 2000;

    /**
     * 段落合并时的最大字符数。
     */
    private static final int MAX_PARAGRAPH_CHARS = 1500;

    /**
     * Markdown 标题：
     *
     * # Redis
     * ## RDB
     * ### AOF
     */
    private static final Pattern MARKDOWN_TITLE_PATTERN = Pattern.compile("\"^#{1,6}\\\\s+.+\"");

    /**
     * 中文标题：
     *
     * 第一章 Redis
     * 第二章 Kafka
     */
    private static final Pattern CHINESE_CHAPTER_PATTERN =
            Pattern.compile("^第[一二三四五六七八九十百千万0-9]+[章节篇部分]\\s*.*");

    /**
     * 数字标题：
     *
     * 1 Redis
     * 1.1 Redis 持久化
     * 1.1.1 RDB
     * 2.3.4 Kafka Consumer
     */
    private static final Pattern NUMBER_TITLE_PATTERN =
            Pattern.compile("^\\d+(\\.\\d+){0,5}[、.\\s]+.+");

    /**
     * Spring AI 官方 TokenTextSplitter
     */
    private final TokenTextSplitter tokenTextSplitter;

    public KnowledgeChunker() {
        this.tokenTextSplitter =
                TokenTextSplitter.builder()
                        .withChunkSize(TOKEN_CHUNK_SIZE)
                        .withMinChunkSizeChars(200)
                        .withMinChunkLengthToEmbed(20)
                        .withMaxNumChunks(5000)
                        .withKeepSeparator(true)
                        // 对中文知识库很重要
                        .withPunctuationMarks(
                                List.of('。', '？', '！', '；', '\n')
                        )
                        .build();
    }


    public List<Document> chunk(List<Document> documents) {

        if (documents == null || documents.isEmpty()) {
            return Collections.emptyList();
        }

        List<Document> result =new ArrayList<>();
        int globalChunkIndex = 0;
        for (Document document : documents) {
            if (document == null) {
                continue;
            }

            String text = document.getText();
            if (text == null || text.isBlank()) {
                continue;
            }

            /**
             * 第一步：
             * 原始 Document->标题切分
             */
            List<Section> sections = splitByTitle(text);

            for(int sectionIndex = 0; sectionIndex < sections.size(); sectionIndex++) {
                Section section = sections.get(sectionIndex);

                /**
                 * 第二步：
                 * section -> 按段落切分或合并
                 */
                List<String> paragraphChunks = splitByParagraph(section.content());
                for (String paragraphChunk : paragraphChunks) {
                    /**
                     * 拼上标题上下文
                     */
                    String contentWithTitle = buildContent(section.title, paragraphChunk);


                    /**
                     * 第三步：
                     * 如果内容已经很短，可以直接生成 Document
                     *
                     */
                    if (contentWithTitle.length() <= MAX_SECTION_CHARS) {
                        Document chunk = createChunkDocument(document, section, sectionIndex,
                                globalChunkIndex, contentWithTitle);
                        result.add(chunk);
                        continue;
                    }

                    /**
                     * 第四步：
                     * 如果内容依然太长，使用 Spring AI TokenTextSplitter 做最终兜底
                     */
                    Document tempDocument = createChunkDocument(document, section, sectionIndex,
                            globalChunkIndex, contentWithTitle);
                    List<Document> tokenChunks = tokenTextSplitter.apply(List.of(tempDocument));


                }
            }





        }


        // 段落切分

        // TokenTextSplitter

        // metadata 补充

        return documents;
    }

    private Document createChunkDocument(Document document, Section section, int sectionIndex,
                                         int globalChunkIndex, String contentWithTitle) {

        return null;
    }

    /**
     * 按照标题切分
     * @param text 文本
     * @return
     */
    private List<Section> splitByTitle(String text) {
        List<Section> sections = new ArrayList<>();

        // `\\R` = 匹配任意一种换行（一行结束）
        String[] lines = text.split("\\R");

        String currentTitle = null;
        StringBuilder content = new StringBuilder();
        for (String rawLine : lines) {
            String line = rawLine.trim();
            if(line.isBlank()) {
                /**
                 * 保留段落边界
                 */
                content.append("\n\n");
            }

            // 发现标题
            if (isTitle(line)) {
                /**
                 * 进入下一段，保留上一段 section 进集合中
                 */
                if (!content.isEmpty()) {
                    sections.add(new Section(currentTitle, cleanContent(content.toString())));
                    content = new StringBuilder();
                }
                currentTitle = normalizeTitle(line);
                continue;
            }

            content.append(line).append("\n");

        }

        /**
         * 保存最后一段 section
         */
        if (!content.isEmpty()) {
            sections.add(new Section(currentTitle, cleanContent(content.toString())));

        }

        /**
         * 如果完全没有识别到正文，防止返回空
         */
        if (sections.isEmpty() && !text.isBlank()) {
            sections.add(new Section(null, text.trim()));
        }
        return sections;
    }

    private List<String> splitByParagraph(String content) {
        if (content == null || content.isBlank()) {
            return Collections.emptyList();
        }

        /**
         * 两个以上换行视为一个段落
         */
        String[] paragraphs = content.split("\\n\\s*\\n");

        List<String> result = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        for (String paragraph : paragraphs) {
            if (paragraph == null || paragraph.trim().isBlank()) {
                continue;
            }

            // 段落超长，先保存上一段到集合中
            if (paragraph.length() > MAX_SECTION_CHARS) {
                 if (!current.isEmpty()) {
                     result.add(current.toString());
                     // 新的字符串
                     current = new StringBuilder();
                 }
                /**
                 * 超长段落先直接操作一个元素返回
                 * 后面 TokenTextSplitter 会处理。
                 */
                result.add(paragraph);
            }

            /**
             * 如果本段落不超过限制，那加上之前的呢
             */
            int newLength = current.length() + paragraph.length();
            if (newLength > MAX_PARAGRAPH_CHARS) {
                if (!current.isEmpty()) {
                    result.add(current.toString());
                    // 新的字符串
                    current = new StringBuilder();
                }
                /**
                 * 超长段落先直接操作一个元素返回
                 * 后面 TokenTextSplitter 会处理。
                 */
                result.add(paragraph);
            } else {
                if (!current.isEmpty()) {
                    current.append("\n\n");
                }
                current.append(paragraph);
            }
        }

        // 保存最后一块
        if (!current.isEmpty()) {
            result.add(current.toString());
        }

        return result;
    }

    private String buildContent(String title, String content) {
        if (content == null || content.isBlank()) {
            return content;
        }

        return """
               %s
               
               %s
               """.formatted(title, content).trim();
    }

    /**
     * 判断是否为标题
     * @param line 行
     * @return 是否
     */
    private boolean isTitle(String line) {

        /**
         * Markdown
         */
         if (MARKDOWN_TITLE_PATTERN.matcher(line).matches()) {
             return true;
         }

        /**
         * 第一章 / 第二节
         */
         if(CHINESE_CHAPTER_PATTERN.matcher(line).matches()) {
             return true;
         }

        /**
         * 1 xxx
         * 1.1 xxx
         * 1.1.1 xxx
         */
         if(NUMBER_TITLE_PATTERN.matcher(line).matches()) {
             return true;
         }

         return false;
    }

    /**
     * 清理 Markdown 标题。
     * ## Kafka Consumer -> Kafka Consumer
     * @param title 标题
     * @return 结果
     */
    private String normalizeTitle(String title) {
        if (title == null) {
            return null;
        }
        return title.replaceFirst("^#{1,6}\\s*","");
    }

    /**
     * 清理多余换行
     * @param content 文本
     * @return 结果
     */
    private String cleanContent(String content) {
        return content.replaceFirst("\\n{3,}", "\n\n").trim();
    }

    /**
     * 内部数据结构
     * 关键字 record 专门用来存数据，减少样板代码
     * @param title 标题
     * @param content 内容
     */
    private record Section(String title, String content) {

    }

}
