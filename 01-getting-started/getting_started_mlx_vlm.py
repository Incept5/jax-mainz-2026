from mlx_vlm import load, generate
from mlx_vlm.prompt_utils import apply_chat_template
from mlx_vlm.utils import load_config

model_path = "mlx-community/Qwen3.5-VL-4B-MLX-4bit"
model, processor = load(model_path)
config = load_config(model_path)

image = ["https://upload.wikimedia.org/wikipedia/commons/3/3a/Cat03.jpg"]
prompt = "Describe this image in one short sentence"
formatted = apply_chat_template(processor, config, prompt, num_images=len(image))

output = generate(model, processor, formatted, image, verbose=False)
print(output)
