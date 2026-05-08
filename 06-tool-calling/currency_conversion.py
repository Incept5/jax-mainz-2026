import requests


# Made-up rates expressed relative to USD.
RATES_TO_USD = {
    "USD": 1.0,
    "GBP": 1.27,
    "EUR": 1.08,
}


def convert_currency(amount, from_currency, to_currency):
    from_currency = from_currency.upper()
    to_currency = to_currency.upper()

    if from_currency not in RATES_TO_USD or to_currency not in RATES_TO_USD:
        return f"Unsupported currency. Supported: {', '.join(RATES_TO_USD)}."

    amount_in_usd = float(amount) * RATES_TO_USD[from_currency]
    converted = amount_in_usd / RATES_TO_USD[to_currency]
    return f"{amount} {from_currency} = {converted:.2f} {to_currency}"


available_functions = {
    "convert_currency": convert_currency,
}

tools = [
    {
        "type": "function",
        "function": {
            "name": "convert_currency",
            "description": "Convert an amount from one currency to another. Supports USD, GBP, and EUR.",
            "parameters": {
                "type": "object",
                "properties": {
                    "amount": {
                        "type": "number",
                        "description": "The amount of money to convert.",
                    },
                    "from_currency": {
                        "type": "string",
                        "description": "The currency code to convert from, e.g. 'USD'.",
                    },
                    "to_currency": {
                        "type": "string",
                        "description": "The currency code to convert to, e.g. 'EUR'.",
                    },
                },
                "required": ["amount", "from_currency", "to_currency"],
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
        {"role": "user", "content": "How much is 50 GBP in EUR?"},
    ]

    response = chat(messages)
    message = response["message"]

    tool_calls = message.get("tool_calls")
    if not tool_calls:
        print("Model responded without calling a tool:")
        print(message.get("content", ""))
        return

    for tool_call in tool_calls:
        function_name = tool_call["function"]["name"]
        arguments = tool_call["function"].get("arguments", {})

        tool_call_id = tool_call.get("id", str(hash(function_name)))

        if function_name in available_functions:
            function_result = available_functions[function_name](**arguments)

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

    final = chat(messages)
    print("Final answer:")
    print(final["message"].get("content", ""))


if __name__ == "__main__":
    main()
