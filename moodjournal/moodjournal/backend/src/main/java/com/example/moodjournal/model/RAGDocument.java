package com.example.moodjournal.model;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;





@Data
@NoArgsConstructor
@AllArgsConstructor
public class RAGDocument implements java.io.Serializable {
    private static final long serialVersionUID = 1L;

    private String id;
    private String text; 
    private String category; 
    private String subtype; 
    private String detail1; 
    private String detail2; 

    
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
