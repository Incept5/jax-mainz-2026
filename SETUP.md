# Setup & Prerequisites

These are **"ideal" prerequisites** — having them ready will save us time on the day, but none of this is strictly required. Cloud alternatives will be demonstrated and API keys supplied where needed. Don't worry if you only get partway through.

## 1. Python

We're not programming in Python, but many of the demos are written in Python — it's just easier to fit on a screen and explain. You can work 100% in Java, Go, TypeScript, or even PHP during the day, but having Python available will make running demos quicker and smoother.

- Install **Python 3.12 or newer**
- Use [MiniConda](https://docs.conda.io/projects/miniconda/) or [uv](https://docs.astral.sh/uv/) to manage your Python environment

## 2. Local Models

We'll cover several models. The starter set below is small enough to run on almost any computer — even a Raspberry Pi.

### 2a. Pick a Model Host

You only need **one** of these. Both work on Mac, Linux, and Windows. If you install both, you'll have a choice on the day.

- **LM Studio** — https://lmstudio.ai/download
- **Ollama** — https://ollama.com/download

### 2b. Pull Some Starter Models

Pick the set that matches the host you installed.

**LM Studio:**

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
