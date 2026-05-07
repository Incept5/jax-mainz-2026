import os
import requests
from dotenv import load_dotenv

load_dotenv()

response = requests.post(
    "https://api.together.xyz/v1/chat/completions",
    headers={"Authorization": f"Bearer {os.environ['TOGETHER_API_KEY']}"},
    json={"model": "meta-llama/Llama-3.3-70B-Instruct-Turbo-Free",
          "messages": [{"role": "user", "content": "Hello"}]}
)

data = response.json()
print(data["choices"][0]["message"]["content"].strip())
