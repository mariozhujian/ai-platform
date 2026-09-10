package cn.mario.aiplatform.service.impl;

import cn.mario.aiplatform.chunk.KnowledgeChunker;
import cn.mario.aiplatform.exception.BizException;
import cn.mario.aiplatform.parser.PdfDocumentParser;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.util.ReflectionTestUtils;

import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class PdfKnowledgeServiceImplTest {
    @Test
    void rejectsSameBytesWithDifferentNameButAcceptsChangedBytes() {
        ChatClient.Builder builder = mock(ChatClient.Builder.class);
        when(builder.build()).thenReturn(mock(ChatClient.class));
        PdfKnowledgeServiceImpl service = new PdfKnowledgeServiceImpl(builder);
        JdbcTemplate jdbc = mock(JdbcTemplate.class);
        PdfDocumentParser parser = mock(PdfDocumentParser.class);
        KnowledgeChunker chunker = mock(KnowledgeChunker.class);
        VectorStore store = mock(VectorStore.class);
        ReflectionTestUtils.setField(service, "jdbcTemplate", jdbc);
        ReflectionTestUtils.setField(service, "pdfDocumentParser", parser);
        ReflectionTestUtils.setField(service, "knowledgeChunker", chunker);
        ReflectionTestUtils.setField(service, "vectorStore", store);
        Set<String> hashes = new HashSet<>();
        when(jdbc.update(anyString(), anyString(), anyString())).thenAnswer(call ->
                hashes.add(call.getArgument(1, String.class)) ? 1 : 0);
        when(parser.parse(any())).thenReturn(List.of(new Document("content")));
        when(chunker.chunk(anyList())).thenReturn(List.of(new Document("chunk")));

        service.importPdf(file("a.pdf", "same bytes"));
        BizException duplicate = assertThrows(BizException.class,
                () -> service.importPdf(file("renamed.pdf", "same bytes")));
        assertEquals(409, duplicate.getCode());
        service.importPdf(file("a.pdf", "changed bytes"));
        verify(parser, times(2)).parse(any());
        verify(store, times(2)).add(anyList());
        assertEquals(2, hashes.size());
    }

    private ByteArrayResource file(String name, String content) {
        return new ByteArrayResource(content.getBytes(StandardCharsets.UTF_8)) {
            @Override
            public String getFilename() {
                return name;
            }
        };
    }
}
