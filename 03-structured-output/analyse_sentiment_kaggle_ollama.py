"""
Ollama port of the Kaggle sentiment example.

Loads the Trump-tweets dataset from Kaggle (same as analyse_sentiment_kaggle.py)
and then runs each tweet through a local Ollama model for sentiment.

Prerequisites:
  - Ollama running locally (https://ollama.com)
  - The model pulled: `ollama pull qwen3.5:4b`
"""

import os
import pandas as pd
import requests
import kagglehub

OLLAMA_URL = "http://localhost:11434/api/generate"
MODEL = "qwen3.5:4b"
SAMPLE_SIZE = 20  # how many tweets to classify


def load_kaggle_data(data):
    print(f"Downloading {data} dataset...")
    path = kagglehub.dataset_download(f"austinreese/{data}")
    print(f"Dataset downloaded to: {path}")

    csv_file = None
    for root, dirs, files in os.walk(path):
        for file in files:
            if file.endswith('.csv'):
                csv_file = os.path.join(root, file)
                break
        if csv_file:
            break

    if csv_file is None:
        raise FileNotFoundError("No CSV file found in the downloaded dataset.")

    print(f"Reading CSV file: {csv_file}")
    df = pd.read_csv(csv_file)

    print(f"\nDataset shape: {df.shape}")
    print(f"  -> {df.shape[0]:,} rows (tweets/records)")
    print(f"  -> {df.shape[1]} columns (features)")

    return df


def analyse_sentiment(text, model=MODEL):
    prompt = f"""
    Analyse the sentiment of the following text and respond with exactly one word:
    'positive', 'neutral', or 'negative'.
    Text: {text}
    Sentiment:
    """

    response = requests.post(
        OLLAMA_URL,
        json={"model": model, "prompt": prompt, "stream": False, "think": False},
        timeout=30,
    )
    response.raise_for_status()

    sentiment = response.json().get("response", "").strip().lower()

    for word in ("positive", "negative", "neutral"):
        if word in sentiment:
            return word
    return "neutral"


def main():
    tweets_df = load_kaggle_data("trump-tweets")

    text_col = next((c for c in ("content", "text", "tweet") if c in tweets_df.columns), None)
    if text_col is None:
        print(f"\nCouldn't find a text column. Columns available: {tweets_df.columns.tolist()}")
        return

    sample = tweets_df.sample(n=min(SAMPLE_SIZE, len(tweets_df)), random_state=42)
    print(f"\nClassifying {len(sample)} tweets via Ollama ({MODEL})...\n")

    counts = {"positive": 0, "neutral": 0, "negative": 0}
    for tweet in sample[text_col].astype(str):
        sentiment = analyse_sentiment(tweet)
        counts[sentiment] += 1
        preview = tweet[:80].replace("\n", " ") + ("..." if len(tweet) > 80 else "")
        print(f"  [{sentiment.upper():>8}]  {preview}")

    total = sum(counts.values())
    print("\nSummary:")
    for label, n in counts.items():
        pct = (n / total * 100) if total else 0
        print(f"  {label:>8}: {n:>3}  ({pct:5.1f}%)")


if __name__ == "__main__":
    main()
