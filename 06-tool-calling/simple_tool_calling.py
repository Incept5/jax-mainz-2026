import json
import requests


def understand_tax_codes(tax_code="1257L"):
    explanations = {
        "1257L": "Standard tax code for 2024/25. Personal allowance of £12,570.",
        "1737L": "Higher personal allowance of £17,370, often due to additional reliefs.",
        "BR": "Basic rate. All income from this source taxed at 20%.",
        "D0": "All income from this source taxed at the higher rate (40%).",
        "NT": "No tax is to be deducted from this income.",
        "K": "Tax code with a K prefix means deductions exceed allowances.",
    }
    return explanations.get(tax_code, f"Unknown tax code: {tax_code}.")


available_functions = {
    "understand_tax_codes": understand_tax_codes,
}

tools = [
    {
        "type": "function",
        "function": {
            "name": "understand_tax_codes",
            "description": "Explain what a UK tax code means.",
            "parameters": {
                "type": "object",
                "properties": {
                    "tax_code": {
                        "type": "string",
                        "description": "The tax code to explain, e.g. '1257L'.",
                    }
                },
                "required": ["tax_code"],
            },
        },
    }
]


def chat(messages, model="qwen3.5:4b"):
    response = requests.post(
        "http://localhost:11434/api/chat",
        json={
            "model": model,
            "messages": messages,
            "tools": tools,
            "stream": False,
            "think": False,
        },
    )
    response.raise_for_status()
    return response.json()


def main():
    messages = [
        {"role": "user", "content": "What does the tax code 1737L mean?"},
    ]

    response = chat(messages)
    message = response["message"]

    tool_calls = message.get("tool_calls")
    if not tool_calls:
        print("Model responded without calling a tool:")
        print(message.get("content", ""))
        return

    # Collect tool responses to add to messages
    for tool_call in tool_calls:
        # Extract function name
        function_name = tool_call["function"]["name"]
        arguments = tool_call["function"].get("arguments", {})

        # Generate a unique ID if one doesn't exist
        tool_call_id = tool_call.get("id", str(hash(function_name)))

        # Call the function
        if function_name in available_functions:
            function_result = available_functions[function_name](**arguments)

            # Add the function call and result to the messages
            messages.append({
                "role": "assistant",
                "content": None,
                "tool_calls": [tool_call],
            })
            messages.append({
                "role": "tool",
                "tool_call_id": tool_call_id,
                "name": function_name,
                "content": function_result,
            })
            print(f"Called function: {function_name} with {arguments}")
            print(f"Result: {function_result}\n")

    # Send the tool results back to the model for a final answer
    final = chat(messages)
    print("Final answer:")
    print(final["message"].get("content", ""))


if __name__ == "__main__":
    main()
