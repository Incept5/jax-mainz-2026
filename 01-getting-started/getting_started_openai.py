import os
import requests
from dotenv import load_dotenv

load_dotenv()

response = requests.post(
    "https://api.openai.com/v1/chat/completions",
    headers={"Authorization": f"Bearer {os.environ['OPENAI_API_KEY']}"},
    json={"model": "gpt-4o-mini",
          "messages": [{"role": "user", "content": "Hello"}]}
)

data = response.json()
print(data["choices"][0]["message"]["content"].strip())
