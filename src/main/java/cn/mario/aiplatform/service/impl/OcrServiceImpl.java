package cn.mario.aiplatform.service.impl;


import cn.mario.aiplatform.service.OcrService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Base64;
import java.util.List;
import java.util.Map;

/**
 * @description: OCR（Optical Character Recognition，光学字符识别）
 * @author: mario
 * @date: 9/11/26
 */
@Service
public class OcrServiceImpl implements OcrService {

    private final String model;
    private final RestClient client;

    public OcrServiceImpl(@Value("${knowledge.ocr.base-url}") String baseUrl,
                          @Value("${knowledge.ocr.api-key}") String apiKey,
                          @Value("${knowledge.ocr.model:qwen3.5-ocr}") String model) {
        var factory = new JdkClientHttpRequestFactory();
        factory.setReadTimeout(Duration.ofSeconds(60));

        this.model = model;
        this.client = RestClient.builder()
                .baseUrl(baseUrl)
                .defaultHeaders(headers -> headers.setBearerAuth(apiKey))
                .requestFactory(factory)
                .build();
    }

    @Override
    public String recognize(Path image) {
        return recognizeByModel(image);
    }

    private String recognizeByModel(Path image) {
        String name = image.getFileName().toString();
        String mime = switch (name.substring(name.lastIndexOf('.') + 1)) {
            case "png" -> "image/png";
            case "jpg", "jpeg" -> "image/jpeg";
            default -> throw new IllegalArgumentException("OCR 仅支持 PNG/JPEG");
        };

        byte[] bytes;
        try (var input = Files.newInputStream(image)) {
            bytes = input.readNBytes(10 * 1024 * 1024 + 1);
        } catch (IOException e) {
            throw new IllegalStateException("读取 OCR 图片失败", e);
        }

        if (bytes.length == 0 || bytes.length > 10 * 1024 * 1024) {
            throw new IllegalArgumentException("OCR 图片不能为空或超过 10MB");
        }

        String dataUrl = "data:" + mime + ";base64,"
                + Base64.getEncoder().encodeToString(bytes);

        var body = Map.of("model", model,
                "messages", List.of(Map.of(
                        "role", "user",
                        "content", List.of(
                                Map.of(
                                    "type", "image_url",
                                    "image_url", Map.of("url", dataUrl)),
                                Map.of(
                                        "type", "text",
                                        "text", "识别图片中的全部文字，按阅读顺序输出。保留段落和表格结构，不要解释或编造内容。"
                                )
                        ))
                ));

        ChatResponse result = client.post().uri("/chat/completions")
                .contentType(MediaType.APPLICATION_JSON)
                .body(body)
                .retrieve()
                .body(ChatResponse.class);
        if (result == null || result.choices() == null || result.choices().isEmpty()) {
            throw new IllegalStateException("OCR 未返回识别结果");
        }

        Choice choice = result.choices().get(0);
        if (!"stop".equals(choice.finish_reason()) || choice.message() == null
                || choice.message().content() == null || choice.message().content().isBlank()) {
            throw new IllegalStateException("OCR 返回异常或内容未完整生成");
        }
        return choice.message().content();
    }

    private record ChatResponse(List<Choice> choices) {}

    private record Choice(String finish_reason, Message message) {}

    private record Message(String content) {}
}
