package com.example.moodjournal.service;

import org.jsoup.Jsoup;
import org.jsoup.safety.Safelist;
import org.springframework.stereotype.Service;

@Service
public class SanitizationService {

    







    public String sanitize(String input) {
        if (input == null) {
            return null;
        }
        
        
        
        
        return Jsoup.clean(input, Safelist.basic());
    }

    



    public String sanitizeStrict(String input) {
        if (input == null) {
            return null;
        }
        return Jsoup.clean(input, Safelist.none());
    }
}
