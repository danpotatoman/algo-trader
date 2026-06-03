import json
from pathlib import Path

import matplotlib.pyplot as plt
import pandas as pd


LOG_DIR = Path("data/logs/trading-cycles")
STARTING_BALANCE = 10_000.0


def load_action_logs(log_dir: Path) -> pd.DataFrame:
    rows = []

    for path in sorted(log_dir.glob("*.json")):
        with path.open("r", encoding="utf-8") as f:
            cycle = json.load(f)

        for action in cycle.get("actions", []):
            rows.append({
                "cycleId": cycle.get("cycleId"),
                "cycleTimestamp": cycle.get("timestamp"),
                "timestamp": action["timestamp"],
                "action": action["action"],
                "ticker": cycle["metadata"]["ticker"],
                "quantity": action["quantity"],
                "price": action["price"],
            })

    return pd.DataFrame(rows)


def compute_balance_curve(actions: pd.DataFrame) -> pd.DataFrame:
    actions = actions.copy()
    actions["timestamp"] = pd.to_datetime(actions["timestamp"])
    actions = actions.sort_values("timestamp")

    def cash_change(row):
        value = row["price"] * row["quantity"]

        if row["action"] == "BUY":
            return -value

        if row["action"] == "SELL":
            return value

        return 0.0

    actions["cashChange"] = actions.apply(cash_change, axis=1)
    actions["balance"] = STARTING_BALANCE + actions["cashChange"].cumsum()

    return actions


actions = load_action_logs(LOG_DIR)

if actions.empty:
    raise RuntimeError("No action logs found.")

balance_curve = compute_balance_curve(actions)

plt.figure(figsize=(12, 6))
plt.plot(balance_curve["timestamp"], balance_curve["balance"])
plt.title("Simulated Account Balance")
plt.xlabel("Time")
plt.ylabel("Account Balance ($)")
plt.grid(True)
plt.tight_layout()
plt.show()