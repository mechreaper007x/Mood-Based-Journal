# NLP Sentiment Analysis Python Microservice

FastAPI service implementing Steps 2-4 of the NLP pipeline:

- **spaCy**: Preprocessing (tokenization, lemmatization)
- **TextBlob**: Polarity and subjectivity scores
- **VADER**: Compound sentiment scoring

## Setup

```bash
cd nlp-service
python -m venv venv
venv\Scripts\activate
pip install -r requirements.txt
python -m spacy download en_core_web_sm
```

## Run

```bash
python app.py
```

Server runs on http://localhost:5001
