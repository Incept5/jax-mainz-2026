# Setup & Prerequisites

These are **"ideal" prerequisites** — having them ready will save us time on the day, but none of this is strictly required. Cloud alternatives will be demonstrated and API keys supplied where needed. Don't worry if you only get partway through.

## 1. Python

We're not programming in Python, but many of the demos are written in Python — it's just easier to fit on a screen and explain. You can work 100% in Java, Go, TypeScript, or even PHP during the day, but having Python available will make running demos quicker and smoother.

- Install **Python 3.12 or newer**
- Use [MiniConda](https://docs.conda.io/projects/miniconda/) or [uv](https://docs.astral.sh/uv/) to manage your Python environment

## 2. Local Models

We'll cover several models. The starter set below is small enough to run on almost any computer — even a Raspberry Pi.

### 2a. Pick a Model Host

You only need **one** of these. Install whichever fits your machine — if you install more than one, you'll have a choice on the day.

- **LM Studio** — https://lmstudio.ai/download (Mac, Linux, Windows)
- **Ollama** — https://ollama.com/download (Mac, Linux, Windows)
- **oMLX** — https://omlx.ai/ (Apple Silicon Macs only — fast MLX-based inference, OpenAI/Anthropic-compatible API, menu-bar app)
- **llama.cpp** — https://github.com/ggml-org/llama.cpp (Mac, Linux, Windows — bare-metal C/C++ inference, GGUF models, ships with `llama-server` for an OpenAI-compatible HTTP API)
- **MLX-VLM** — https://github.com/Blaizzy/mlx-vlm (Apple Silicon only — for vision-language models; we'll use this for the visual-model demos)

### 2b. Pull Some Starter Models

Pick the set that matches the host you installed.

**LM Studio / oMLX** (both use MLX models on Apple Silicon):

```
mlx-community/Qwen3.5-4B-MLX-4bit
mlx-community/granite-4.1-3b-mxfp4
mlx-community/NVIDIA-Nemotron-3-Nano-4B-4bit
```

**Ollama:**

```bash
ollama pull qwen3.5:4b
ollama pull granite4.1:3b
ollama pull nemotron-3-nano:4b
ollama pull embeddinggemma
```

`embeddinggemma` is an embedding model (~622 MB) used by the RAG demo in `05-rag/` — the chat models above can't produce embeddings, so this one is needed alongside them.

**llama.cpp:**

Install the binary first:

```bash
# macOS / Linux (Homebrew)
brew install llama.cpp

# or build from source — see https://github.com/ggml-org/llama.cpp
```

Then pull and serve a GGUF model directly from Hugging Face. `llama-server` exposes an OpenAI-compatible API on `http://localhost:8080`:

```bash
# Search Hugging Face for a GGUF build of each starter model and run e.g.
llama-server -hf bartowski/Qwen3.5-4B-Instruct-GGUF
llama-server -hf bartowski/granite-4.1-3b-GGUF
llama-server -hf bartowski/Nemotron-3-Nano-4B-GGUF
```

(Exact GGUF repo names vary — search `bartowski` or `ggml-org` on Hugging Face for the model you want. Add `--port 8081` etc. to run more than one in parallel.)

**MLX-VLM** (Apple Silicon, vision-language models):

Install with `uv` or `pip`:

```bash
# with uv (recommended)
uv tool install mlx-vlm

# or with pip
pip install mlx-vlm
```

Models are pulled from the `mlx-community` org on Hugging Face the first time you use them. Use the **vision variants** of the same model families we're running elsewhere in the workshop:

```bash
# One-off generation against an image
python -m mlx_vlm.generate \
  --model mlx-community/Qwen3.5-VL-4B-MLX-4bit \
  --image path/to/image.jpg \
  --prompt "Describe this image"

# Or run a server (OpenAI-compatible) for the demos
python -m mlx_vlm.server \
  --model mlx-community/Qwen3.5-VL-4B-MLX-4bit
```

Other vision models worth grabbing (same families as the text starters):

- `mlx-community/granite-4.1-vision-3b-mxfp4`
- `mlx-community/gemma-4-vision-e4b-4bit`

**Tokenizer model (for `02-llm-basics/simple_token_test.py`):**

The tokenization demo uses Hugging Face `transformers` to load a tokenizer directly — no model host required. Pre-download it so the demo runs offline:

```bash
pip install -U transformers huggingface_hub
hf download Qwen/Qwen3.5-0.8B
```

(`AutoTokenizer.from_pretrained("Qwen/Qwen3.5-0.8B")` will also fetch it on first run if you skip this step.)

### 2c. Verify Your Setup

Run the matching test for whichever host you installed. A short German greeting confirms the model is loaded, the API is reachable, and tokens are flowing.

**LM Studio**

1. Open LM Studio, load one of the starter models.
2. Go to the **Developer** tab and click **Start Server** (defaults to port `1234`).
3. From a terminal:

```bash
curl http://localhost:1234/v1/chat/completions \
  -H "Content-Type: application/json" \
  -d '{"messages": [{"role": "user", "content": "Say hello in German"}]}'
```

**Ollama**

```bash
ollama run qwen3.5:4b "Say hello in German"
```

Or via the HTTP API (Ollama runs on `11434` by default):

```bash
curl http://localhost:11434/api/generate -d '{
  "model": "qwen3.5:4b",
  "prompt": "Say hello in German",
  "stream": false
}'
```

**oMLX**

Start oMLX from the menu bar and load a model. It serves on `http://localhost:8000/v1`.

> **API key note:** oMLX guards the server with an API key by default — there's no signup or "get your key" flow, the key is whatever **you** decide it should be. Two ways to handle it:
>
> - **Easiest for the workshop:** open the admin panel (http://localhost:8000/admin) and turn on the "skip verification on localhost" / no-auth-for-localhost option in global settings.
> - **Or set your own:** start the server with `--api-key dev-key` (any string works) and pass it as `Authorization: Bearer dev-key` on every request.

With auth disabled for localhost:

```bash
curl http://localhost:8000/v1/chat/completions \
  -H "Content-Type: application/json" \
  -d '{
    "model": "Qwen3.5-4B-MLX-4bit",
    "messages": [{"role": "user", "content": "Say hello in German"}]
  }'
```

With your own API key:

```bash
curl http://localhost:8000/v1/chat/completions \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer dev-key" \
  -d '{
    "model": "Qwen3.5-4B-MLX-4bit",
    "messages": [{"role": "user", "content": "Say hello in German"}]
  }'
```

**llama.cpp**

In one terminal, start the server (it'll download the model on first run):

```bash
llama-server -hf bartowski/Qwen3.5-4B-Instruct-GGUF
```

In a second terminal:

```bash
curl http://localhost:8080/v1/chat/completions \
  -H "Content-Type: application/json" \
  -d '{"messages": [{"role": "user", "content": "Say hello in German"}]}'
```

**MLX-VLM**

A vision-language model needs an image. This one-liner downloads a sample and asks the model to describe it:

```bash
python -m mlx_vlm.generate \
  --model mlx-community/Qwen3.5-VL-4B-MLX-4bit \
  --image https://upload.wikimedia.org/wikipedia/commons/3/3a/Cat03.jpg \
  --prompt "Describe this image in one short sentence"
```

If you see a sensible response in any of these, you're good to go.

### 2d. Bigger Models (Optional)

If you have the memory (typically a Mac with plenty of unified memory), also grab any of:

- `Qwen3.5:9B`, `Qwen3.6:27B`, `Qwen3.6:35B`
- `gemma-4:e2b`, `gemma-4:e4b` (the new Gemma-4 models)

We'll cover the details of all these models and their variations during the workshop.

### 2e. Cloud Alternative — Use OpenAI Instead

If you already have an OpenAI account/API key, you can skip the local model host entirely and run every exercise against `api.openai.com`. The whole workshop should cost a few cents on `gpt-4o-mini`.

**Set your key** (the exercises read it from the environment, or from a `.env` at the project root via `python-dotenv`):

```bash
export OPENAI_API_KEY="sk-..."
```

**Pick a model:**

- Chat: `gpt-4o-mini` (cheap, fast, capable enough for everything here). `gpt-5-nano` if your account has it.
- Embeddings (used by `05-rag/`): `text-embedding-3-small` — 1536 dims, very cheap.
- Tool calling (used by `06-tool-calling/`, `07-mcp/`): any chat model above supports tools.

**Quick verify:**

```bash
curl https://api.openai.com/v1/chat/completions \
  -H "Authorization: Bearer $OPENAI_API_KEY" \
  -H "Content-Type: application/json" \
  -d '{
    "model": "gpt-4o-mini",
    "messages": [{"role": "user", "content": "Say hello in German"}]
  }'
```

A working starter is checked in at `01-getting-started/getting_started_openai.py`. Run that first to confirm your key works end-to-end.

**Adapting the other exercises**

The exercise scripts default to Ollama's *native* API (`http://localhost:11434/api/generate` or `/api/chat`). The change to point them at OpenAI is the same everywhere:

| Replace | With |
| --- | --- |
| URL `http://localhost:11434/api/generate` or `/api/chat` | `https://api.openai.com/v1/chat/completions` |
| (no auth header) | `Authorization: Bearer $OPENAI_API_KEY` |
| `model` value (e.g. `qwen3.5:4b`) | `gpt-4o-mini` |
| `{"prompt": "..."}` (Ollama `/api/generate`) | `{"messages": [{"role":"user","content":"..."}]}` |
| Response field `data["response"]` | `data["choices"][0]["message"]["content"]` |
| Ollama-only fields (`think`, `options.num_ctx`, `stream: false`) | drop them |

`getting_started_together.py` already follows this exact shape — copy it as a template if it helps.

**Per-folder notes:**

- **`01-getting-started/`** — use `getting_started_openai.py`.
- **`02-llm-basics/`** — `simple_token_test.py` uses Hugging Face's tokenizer locally; no model host needed. `cosine_similarity.py` calls Ollama's `/api/embeddings` — swap that to OpenAI's `/v1/embeddings` with `text-embedding-3-small` and read the result from `data["data"][0]["embedding"]`.
- **`03-structured-output/`** — `analyse_sentiment_01.py` and `analyse_sentiment_02.py`: replace the `requests.post(...)` block per the table above. `formatted_response_example.py` uses the `ollama` Python library with `format="json"` — for OpenAI use the official SDK with `response_format`:

  ```python
  from openai import OpenAI
  client = OpenAI()
  resp = client.chat.completions.create(
      model="gpt-4o-mini",
      messages=[{"role": "user", "content": prompt}],
      response_format={"type": "json_object"},
  )
  print(resp.choices[0].message.content)
  ```

- **`05-rag/`** — replace `OllamaClient` with calls to OpenAI's `/v1/embeddings` and `/v1/chat/completions`. Use `text-embedding-3-small` and drop the `task: search result | query: …` / `title: none | text: …` prefixes — those are EmbeddingGemma-specific and OpenAI doesn't need them. The chunking, cosine similarity, and ChromaDB code is unchanged. `grimm_fairy_tales_rag_demo.py` uses the `ollama` library directly, so it needs the bigger rewrite — easiest to mirror the structure of `rag_alice_in_wonderland.py` after you've ported that one.
- **`06-tool-calling/simple_tool_calling.py`** — Ollama's tool-calling format is copied from OpenAI, so the `tools` array and the tool-result message shape pass through unchanged. Just switch the URL, auth header, model, and response parsing per the table.
- **`07-mcp/test_mcp_client_ollama.py`** — same story: drop in OpenAI's chat completions endpoint and auth, keep the MCP-tool conversion as-is. A renamed `test_mcp_client_openai.py` is a fine starting point.

### 2f. Cloud Alternative — Together AI (Workshop Key Provided)

If you don't have an OpenAI account and can't run a local host, use **Together AI**. A throwaway key is provided for the workshop:

```
TOGETHER_API_KEY=tgp_v1_1Vz3vT2d4JjMnJQX7d8g_8PZbu0yxLP-8PrpcWFVJUA
```

> This key is workshop-only and **will be rotated/deleted after the talk**. Don't build anything important on top of it.

Drop it into a `.env` at the project root (the scripts use `python-dotenv`):

```bash
echo 'TOGETHER_API_KEY=tgp_v1_1Vz3vT2d4JjMnJQX7d8g_8PZbu0yxLP-8PrpcWFVJUA' > .env
```

Then run the starter:

```bash
python 01-getting-started/getting_started_together.py
```

Together's API is OpenAI-compatible, so the per-folder advice in **2e** applies verbatim — just change the URL to `https://api.together.xyz/v1/chat/completions`, send the key as `Authorization: Bearer $TOGETHER_API_KEY`, and pick a Together-hosted model (e.g. `Qwen/Qwen3.5-9B`). For embeddings (used by `05-rag/`), Together hosts e.g. `togethercomputer/m2-bert-80M-8k-retrieval` via `/v1/embeddings`.

## 3. Coding Agents

We'll run a few demos with **Claude Code** — not only against Anthropic models, but also against:

- Kimi K2.6
- MiniMax M2.7
- DeepSeek V4
- GLM-5.1

If you can install and sign in to Claude Code ahead of time, it'll save setup time on the day. **Note:** Claude Code requires at least a basic Anthropic subscription.

We'll also use **Open Code** and **Pi**.

## 4. Corporate Laptops

If you're on a managed/corporate machine:

- Try to have admin access (or know who to ask)
- Know how to **disable your VPN** if you need to — some local model hosts and downloads behave better without it

Neither is critical, but both can save frustration.

## 5. Java (Optional but Useful)

The Embabel framework section uses the JVM. If you'd like to follow along with that part:

- Install a recent **JDK (Java 21+)**
- Make sure `java -version` and `mvn -version` work in your terminal

---

That's it. Show up with as much of this as you can manage and we'll fill in the gaps on the day.
