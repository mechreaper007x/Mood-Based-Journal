package com.example.moodjournal.dto;

import com.example.moodjournal.model.Mood;

public class MoodCount {
    private Mood mood;
    private Long count;

    public MoodCount(Mood mood, Long count) {
        this.mood = mood;
        this.count = count;
    }

    public Mood getMood() {
        return mood;
    }

    public void setMood(Mood mood) {
        this.mood = mood;
    }

    public Long getCount() {
        return count;
    }

    public void setCount(Long count) {
        this.count = count;
    }
}
