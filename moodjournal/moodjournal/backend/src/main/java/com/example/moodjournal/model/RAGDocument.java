package com.example.moodjournal.model;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

/**
 * Represents a clinical example from the knowledge base (CSV dataset).
 * Used for RAG (Retrieval-Augmented Generation) to ground AI analysis.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RAGDocument implements java.io.Serializable {
    private static final long serialVersionUID = 1L;

    private String id;
    private String text; // The journal entry content
    private String category; // e.g., Nuanced_Depression
    private String subtype; // e.g., Somatic
    private String detail1; // e.g., Heavy limbs
    private String detail2; // e.g., Messy bed

    // Pre-computed tokens for TF-IDF matching
    private transient java.util.Set<String> tokens;

    public RAGDocument(String id, String text, String category, String subtype, String detail1, String detail2) {
        this.id = id;
        this.text = text;
        this.category = category;
        this.subtype = subtype;
        this.detail1 = detail1;
        this.detail2 = detail2;
    }
}
