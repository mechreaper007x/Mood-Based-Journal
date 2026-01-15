"""
NLP Sentiment Analysis Microservice
Implements Steps 2-4 of the 7-step NLP pipeline:
- spaCy: Preprocessing (tokenization, lemmatization, NER)
- TextBlob: Polarity and subjectivity
- VADER: Compound sentiment scoring
"""

from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware
from pydantic import BaseModel
from textblob import TextBlob
from vaderSentiment.vaderSentiment import SentimentIntensityAnalyzer
import logging

# Setup logging
logging.basicConfig(level=logging.INFO)
logger = logging.getLogger(__name__)

# Try to import spaCy (optional - works without it)
nlp = None
try:
    import spacy
    nlp = spacy.load("en_core_web_sm")
    logger.info("spaCy loaded successfully")
except Exception as e:
    logger.warning(f"spaCy not available: {e}. Running without preprocessing.")

vader = SentimentIntensityAnalyzer()

# FastAPI app
app = FastAPI(title="NLP Sentiment Analysis", version="1.0.0")

# CORS for local development
app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_methods=["*"],
    allow_headers=["*"],
)


class AnalyzeRequest(BaseModel):
    text: str


class AnalyzeResponse(BaseModel):
    # Step 2: Preprocessing (spaCy)
    token_count: int
    lemmas: list[str]
    entities: list[dict]
    
    # Step 3: Feature Extraction (TextBlob)
    textblob_polarity: float      # -1 to +1
    textblob_subjectivity: float  # 0 to 1
    
    # Step 4: VADER Classification
    vader_positive: float
    vader_negative: float
    vader_neutral: float
    vader_compound: float         # -1 to +1 (main score)
    
    # Ensemble result
    base_sentiment: str           # POSITIVE, NEGATIVE, NEUTRAL
    emotion: str                  # detected emotion keyword
    category: str                 # Mood enum value


# Emotion keyword lexicon
EMOTION_KEYWORDS = {
    "happy": ["happy", "joy", "joyful", "excited", "grateful", "love", "amazing", "blessed", 
              "wonderful", "fantastic", "great", "awesome", "delighted", "thrilled", "elated"],
    "sad": ["sad", "depressed", "lonely", "hurt", "pain", "cry", "miss", "lost", "empty",
            "grief", "sorrow", "unhappy", "miserable", "heartbroken", "devastated"],
    "angry": ["angry", "furious", "rage", "hate", "frustrated", "annoyed", "bitter",
              "irritated", "mad", "outraged", "resentful", "hostile"],
    "anxious": ["worried", "stress", "stressed", "anxious", "nervous", "overwhelmed", 
                "pressure", "panic", "fear", "scared", "terrified", "dread", "tense"],
    "calm": ["calm", "peaceful", "relaxed", "content", "serene", "balanced", "tranquil",
             "comfortable", "at ease", "mellow", "steady"],
    "energetic": ["energetic", "motivated", "determined", "driven", "focused", "powerful",
                  "strong", "vibrant", "dynamic", "active", "pumped"],
}

EMOTION_TO_MOOD = {
    "happy": "HAPPY",
    "sad": "SAD", 
    "angry": "ANGRY",
    "anxious": "ANXIOUS",
    "calm": "CALM",
    "energetic": "ENERGETIC",
}


def detect_emotion(text: str, polarity: float) -> tuple[str, str]:
    """Detect specific emotion from text using keywords"""
    text_lower = text.lower()
    
    # Count keyword matches for each emotion
    emotion_scores = {}
    for emotion, keywords in EMOTION_KEYWORDS.items():
        score = sum(1 for kw in keywords if kw in text_lower)
        if score > 0:
            emotion_scores[emotion] = score
    
    # If we found emotion keywords, use the highest scoring one
    if emotion_scores:
        detected_emotion = max(emotion_scores, key=emotion_scores.get)
        return detected_emotion, EMOTION_TO_MOOD[detected_emotion]
    
    # Fallback: infer from polarity
    if polarity > 0.1:
        return "happy", "HAPPY"
    elif polarity < -0.1:
        return "sad", "SAD"
    else:
        return "neutral", "NEUTRAL"


def classify_sentiment(textblob_polarity: float, vader_compound: float) -> str:
    """Classify overall sentiment using ensemble voting"""
    # Weight: VADER 60%, TextBlob 40% (VADER is better for social text)
    combined = (vader_compound * 0.6) + (textblob_polarity * 0.4)
    
    if combined > 0.05:
        return "POSITIVE"
    elif combined < -0.05:
        return "NEGATIVE"
    else:
        return "NEUTRAL"


@app.post("/analyze", response_model=AnalyzeResponse)
async def analyze(request: AnalyzeRequest):
    """Analyze text sentiment using spaCy + TextBlob + VADER ensemble"""
    text = request.text
    logger.info(f"Analyzing text of length {len(text)}")
    
    # Step 2: spaCy Preprocessing
    lemmas = []
    entities = []
    token_count = 0
    
    if nlp:
        doc = nlp(text)
        token_count = len(doc)
        lemmas = [token.lemma_ for token in doc if not token.is_stop and not token.is_punct][:20]
        entities = [{"text": ent.text, "label": ent.label_} for ent in doc.ents][:10]
    
    # Step 3: TextBlob Feature Extraction
    blob = TextBlob(text)
    textblob_polarity = blob.sentiment.polarity
    textblob_subjectivity = blob.sentiment.subjectivity
    
    # Step 4: VADER Classification
    vader_scores = vader.polarity_scores(text)
    
    # Ensemble voting
    base_sentiment = classify_sentiment(textblob_polarity, vader_scores["compound"])
    
    # Emotion detection
    emotion, category = detect_emotion(text, vader_scores["compound"])
    
    logger.info(f"Result: sentiment={base_sentiment}, emotion={emotion}, category={category}")
    
    return AnalyzeResponse(
        token_count=token_count,
        lemmas=lemmas,
        entities=entities,
        textblob_polarity=round(textblob_polarity, 4),
        textblob_subjectivity=round(textblob_subjectivity, 4),
        vader_positive=round(vader_scores["pos"], 4),
        vader_negative=round(vader_scores["neg"], 4),
        vader_neutral=round(vader_scores["neu"], 4),
        vader_compound=round(vader_scores["compound"], 4),
        base_sentiment=base_sentiment,
        emotion=emotion,
        category=category,
    )


@app.get("/health")
async def health():
    return {"status": "ok", "spacy_loaded": nlp is not None}


if __name__ == "__main__":
    import uvicorn
    uvicorn.run(app, host="0.0.0.0", port=5001)
