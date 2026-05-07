import requests

response = requests.post(
    "http://localhost:8000/v1/chat/completions",
    headers={"Authorization": "Bearer dev-key"},
    json={"model": "Qwen3.5-4B-MLX-4bit",
          "messages": [{"role": "user", "content": "Hello"}]}
)

response.raise_for_status()
data = response.json()
print(data["choices"][0]["message"]["content"].strip())
