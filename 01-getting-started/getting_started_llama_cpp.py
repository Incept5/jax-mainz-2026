import requests

response = requests.post(
    "http://localhost:8080/v1/chat/completions",
    json={"messages": [{"role": "user", "content": "Hello"}]}
)

data = response.json()
print(data["choices"][0]["message"]["content"].strip())
