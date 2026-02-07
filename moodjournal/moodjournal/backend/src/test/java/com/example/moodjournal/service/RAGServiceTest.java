package com.example.moodjournal.service;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.example.moodjournal.model.RAGDocument;

@ExtendWith(MockitoExtension.class)
public class RAGServiceTest {

    private RAGService ragService;

    @BeforeEach
    void setUp() {
        ragService = new RAGService();
    }

    @Test
    void testKeywordSearch() {
        // Manually inject docs to avoid CSV loading logic in unit test
        RAGDocument doc1 = new RAGDocument("1", "Anxious feelings today", "Anxiety", "GAD", "d1", "d2");
        doc1.setTokens(Set.of("anxious", "feelings", "today", "anxiety", "gad"));

        RAGDocument doc2 = new RAGDocument("2", "Feeling very Calm and peaceful", "Peace", "Zen", "d1", "d2");
        doc2.setTokens(Set.of("feeling", "very", "calm", "and", "peaceful", "peace", "zen"));

        // Access private list via Reflection
        @SuppressWarnings("unchecked")
        List<RAGDocument> docs = (List<RAGDocument>) ReflectionTestUtils.getField(ragService, "documents");
        docs.add(doc1);
        docs.add(doc2);

        // Manually trigger IDF cache build (private method)
        ReflectionTestUtils.invokeMethod(ragService, "buildIdfCache");

        // Search for "anxious" -> should match doc1
        List<RAGDocument> results = ragService.findSimilarDocuments("anxious", 2);

        assertEquals(1, results.size());
        assertEquals("Anxious feelings today", results.get(0).getText());
    }
}
