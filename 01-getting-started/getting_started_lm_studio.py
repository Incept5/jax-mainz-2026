import requests

response = requests.post("http://localhost:1234/v1/chat/completions",
            json={"model": "mlx-community/Qwen3.5-4B-MLX-4bit",
            "messages": [{"role": "user", "content": "Hello"}]}
)

data = response.json()
print(data["choices"][0]["message"]["content"].strip())
