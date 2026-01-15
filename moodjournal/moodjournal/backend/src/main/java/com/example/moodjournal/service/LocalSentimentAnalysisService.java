package com.example.moodjournal.service;

import com.example.moodjournal.model.Mood;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

@Service
public class LocalSentimentAnalysisService {

    private static final Set<String> POSITIVE_WORDS = new HashSet<>(Arrays.asList(
            "happy", "joy", "good", "great", "awesome", "excellent", "positive", "love", "like", "wonderful", "calm"
    ));

    private static final Set<String> NEGATIVE_WORDS = new HashSet<>(Arrays.asList(
            "sad", "bad", "terrible", "horrible", "awful", "negative", "hate", "dislike", "cry", "angry"
    ));

    public Mood analyzeSentiment(String text) {
        if (text == null || text.isBlank()) {
            return Mood.NEUTRAL;
        }

        int positiveScore = 0;
        int negativeScore = 0;

        String[] words = text.toLowerCase().split("\\s+");

        for (String word : words) {
            if (POSITIVE_WORDS.contains(word)) {
                positiveScore++;
            } else if (NEGATIVE_WORDS.contains(word)) {
                negativeScore++;
            }
        }

        if (positiveScore > negativeScore) {
            if (Arrays.asList(words).contains("calm")) {
                return Mood.CALM;
            }
            return Mood.HAPPY;
        } else if (negativeScore > positiveScore) {
            if (Arrays.asList(words).contains("angry")) {
                return Mood.ANGRY;
            }
            return Mood.SAD;
        } else {
            return Mood.NEUTRAL;
        }
    }
}
