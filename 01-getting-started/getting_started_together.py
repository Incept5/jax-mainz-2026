import os
import requests
from dotenv import load_dotenv

load_dotenv()

response = requests.post(
    "https://api.together.xyz/v1/chat/completions",
    headers={"Authorization": f"Bearer {os.environ['TOGETHER_API_KEY']}"},
    json={"model": "Qwen/Qwen3.5-9B",
          "messages": [{"role": "user", "content": "Hello"}]}
)

data = response.json()
print(data["choices"][0]["message"]["content"].strip())
