FROM python:3.11-slim

WORKDIR /app

# Install dependencies
COPY requirements.txt .
RUN pip install -r requirements.txt

# 🛠️ Install kubectl
RUN apt-get update && \
    apt-get install -y curl gnupg && \
    curl -LO "https://dl.k8s.io/release/$(curl -L -s https://dl.k8s.io/release/stable.txt)/bin/linux/amd64/kubectl" && \
    install -o root -g root -m 0755 kubectl /usr/local/bin/kubectl && \
    rm kubectl

# Copy source code
COPY . .

# Run FastAPI app
CMD ["uvicorn", "webhook_server:app", "--host", "0.0.0.0", "--port", "9001"]
