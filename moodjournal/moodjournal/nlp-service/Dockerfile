FROM python:3.11-slim

WORKDIR /app

# Install system dependencies
RUN apt-get update && apt-get install -y --no-install-recommends \
    gcc \
    && rm -rf /var/lib/apt/lists/*

# Copy requirements first (cached layer)
COPY requirements.txt .

# Install Python dependencies
RUN pip install --no-cache-dir -r requirements.txt

# Download NLTK data for TextBlob
RUN python -c "import nltk; nltk.download('punkt'); nltk.download('averaged_perceptron_tagger')"

# Copy application code
COPY app.py .

# Create non-root user
RUN useradd -m appuser && chown -R appuser:appuser /app
USER appuser

# Expose port
EXPOSE 5001

# Health check
HEALTHCHECK --interval=30s --timeout=3s --start-period=30s \
  CMD python -c "import urllib.request; urllib.request.urlopen('http://localhost:5001/health')" || exit 1

# Run application
CMD ["uvicorn", "app:app", "--host", "0.0.0.0", "--port", "5001"]
