package com.example.moodjournal.util;

public final class PromptConstants {

        private PromptConstants() {
        }

        public static final String EMOTION_BREAKDOWN_PROMPT = """
                        Analyze the following text for emotional sentiment. Output a strict JSON object with estimated
                        percentage values for 'Anger', 'Happy', and 'Sadness', and identify the 'DominantEmotion'.
                        Ensure the percentages reflect the intensity and frequency of emotional cues in the text.

                        The percentages should be strings with '%' suffix (e.g. "60%").

                        Reply in STRICT JSON format only. No markdown, no explanation, just pure JSON:
                        {
                          "Anger": "<0-100>%",
                          "Happy": "<0-100>%",
                          "Sadness": "<0-100>%",
                          "DominantEmotion": "<Anger|Happy|Sadness>"
                        }

                        Rules:
                        - Percentages must add up to 100%
                        - DominantEmotion must be the emotion with highest percentage
                        - If text is neutral, distribute evenly but still pick a dominant
                        """;

        public static final String DAILY_QUOTE_PROMPT = "You are a source of wisdom. Provide a single, short, uplifting quote about self-reflection, mindfulness, or personal growth. The quote must be real and attributed to a known person. Format the response as a JSON object with two keys: 'quote' and 'author'. Example: {\"quote\": \"The unexamined life is not worth living.\", \"author\": \"Socrates\"}";

        public static final String SUGGEST_MOOD_PROMPT = """
                        Analyze the user's journal entry and identify the PRIMARY emotion being expressed.

                        Choose the most fitting emotion from this comprehensive list:
                        acceptance, admiration, adoration, affection, afraid, agitation, agony, amazement, amusement, anger,
                        anguish, annoyed, anticipating, anxious, apathy, apprehension, astonished, awe, bewildered, bitter,
                        bliss, bored, calm, carefree, caring, cheerfulness, comfortable, confident, confusion, contempt,
                        contentment, courage, curiosity, cynicism, dejection, delighted, depressed, desire, despair, determined,
                        disappointment, disbelief, discomfort, disgust, disheartened, dismay, distress, doubt, dread, eagerness,
                        ecstasy, elation, embarrassment, empathy, enchanted, enjoyment, enthusiasm, envy, euphoria, excitement,
                        fascination, fear, fondness, friendliness, fright, frustration, fury, glee, gloomy, gratitude, greed,
                        grief, guilt, happiness, hate, hatred, helpless, homesickness, hope, hopelessness, horrified, humiliation,
                        hurt, impatient, indifference, infatuation, insecurity, interest, intrigued, irritable, isolated, jealousy,
                        joy, jubilation, kind, lazy, loathing, lonely, longing, love, lust, melancholy, miserable, modesty,
                        moody, mortified, nauseated, negative, neglect, nervous, nostalgic, numb, offended, optimistic, outrage,
                        overwhelmed, panicked, paranoid, passion, patience, perplexed, pessimism, pity, pleased, pleasure,
                        positive, pride, puzzled, rage, regret, rejected, relaxed, relieved, reluctant, remorse, resentment,
                        resignation, restlessness, sadness, satisfaction, scared, scorn, self-confident, self-conscious,
                        sentimentality, serenity, shame, shocked, sorrow, spite, stressed, strong, stubborn, suffering,
                        surprise, suspense, suspicious, sympathy, tenderness, tension, terror, thankfulness, thrilled, tired,
                        tolerance, torment, triumphant, troubled, trust, uncertainty, uneasiness, unhappy, unnerved, unsettled,
                        upset, vengeful, vulnerable, weak, worried, worthless, worthy, wrath

                        Respond with a JSON object containing:
                        - "emotion": the primary emotion (lowercase, from the list above)
                        - "category": one of HAPPY, SAD, ANXIOUS, ANGRY, CALM, NEUTRAL (for UI color-coding)
                        - "intensity": a number from 1-10 indicating emotion strength

                        Example: {"emotion": "gratitude", "category": "HAPPY", "intensity": 8}
                        """;
}