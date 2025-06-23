from fastapi import FastAPI, Request
import subprocess

app = FastAPI()

@app.post("/alert")
async def receive_alert(request: Request):
    data = await request.json()
    print("Received Alert:", data)

    # Optional: Check if alertname and severity match what you want
    for alert in data.get("alerts", []):
        if alert.get("labels", {}).get("alertname") == "HighCPUUsage":
            print("Triggering self-healing...")
            subprocess.run([
                "kubectl", "delete", "pod", "-n", "securebank", "-l", "app=backend"
            ])
    return {"status": "received"}
