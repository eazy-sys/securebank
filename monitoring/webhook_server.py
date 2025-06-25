from fastapi import FastAPI, Request
from fastapi.middleware.cors import CORSMiddleware
import subprocess

app = FastAPI()

# Enable CORS to allow frontend (from any origin) to access this backend
app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],  # In production, specify your frontend IP or domain here
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

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
