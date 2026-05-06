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
ollama pull qwen3.5-4b:q4_0
ollama pull granite4.1:3b
ollama pull nemotron-3-nano:4b
```

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

Models are pulled from the `mlx-community` org on Hugging Face the first time you use them. A couple of small VLMs that pair well with the workshop:

```bash
# One-off generation against an image
python -m mlx_vlm.generate \
  --model mlx-community/Qwen2.5-VL-3B-Instruct-4bit \
  --image path/to/image.jpg \
  --prompt "Describe this image"

# Or run a server (OpenAI-compatible) for the demos
python -m mlx_vlm.server \
  --model mlx-community/Qwen2.5-VL-3B-Instruct-4bit
```

Other small VLMs worth grabbing: `mlx-community/SmolVLM-Instruct-bf16`, `mlx-community/gemma-3-4b-it-4bit`.

### 2c. Bigger Models (Optional)

If you have the memory (typically a Mac with plenty of unified memory), also grab any of:

- `Qwen3.5-9B`, `Qwen3.6-27B`, `Qwen3.6-35B`
- `gemma-4-e2b`, `gemma-4-e4b` (the new Gemma-4 models)

We'll cover the details of all these models and their variations during the workshop.

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
