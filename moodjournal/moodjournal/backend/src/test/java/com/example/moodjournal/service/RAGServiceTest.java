package com.example.moodjournal.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.example.moodjournal.model.RAGDocument;

@ExtendWith(MockitoExtension.class)
public class RAGServiceTest {

    @Mock
    private GeminiService geminiService;

    private RAGService ragService;

    @BeforeEach
    void setUp() {
        ragService = new RAGService(geminiService);
    }

    @Test
    void testVectorSearch() {
        // Mock embeddings
        float[] queryVec = { 1.0f, 0.0f, 0.0f };
        float[] docVec1 = { 0.9f, 0.1f, 0.0f }; // Similar
        float[] docVec2 = { 0.0f, 1.0f, 0.0f }; // Orthogonal (Dissimilar)

        when(geminiService.getEmbedding(anyString())).thenReturn(queryVec);

        // Manually inject docs to avoid CSV loading logic in unit test
        RAGDocument doc1 = new RAGDocument("1", "Anxious", "Anxiety", "GAD", "d1", "d2");
        doc1.setEmbedding(docVec1);

        RAGDocument doc2 = new RAGDocument("2", "Calm", "Peace", "Zen", "d1", "d2");
        doc2.setEmbedding(docVec2);

        // Access private list via Reflection
        List<RAGDocument> docs = (List<RAGDocument>) ReflectionTestUtils.getField(ragService, "documents");
        docs.add(doc1);
        docs.add(doc2);

        List<RAGDocument> results = ragService.findSimilarDocuments("test query", 2);

        assertEquals(2, results.size());
        assertEquals("Anxious", results.get(0).getText(), "Most similar doc should be first");
    }
}
