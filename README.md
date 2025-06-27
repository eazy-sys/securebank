# 💳 SecureBank – DevSecOps Banking Application on AKS

SecureBank is a fully containerized, monitored, secured, and auto-remediated banking application deployed on **Azure Kubernetes Service (AKS)**. This project was built to demonstrate **end-to-end DevSecOps practices** with real-world tooling, CI/CD, GitOps, monitoring, alerting, and secure architecture.

---

## 📌 Table of Contents

- [🎯 Project Objectives](#-project-objectives)
- [🧱 Tech Stack](#-tech-stack)
- [⚙️ Architecture Overview](#️-architecture-overview)
- [🚀 CI/CD Pipeline](#-cicd-pipeline)
- [🔐 DevSecOps Integrations](#-devsecops-integrations)
- [📊 Monitoring & Alerting](#-monitoring--alerting)
- [⚒️ Self-Healing Setup](#️-self-healing-setup)
- [🌐 Application Features](#-application-features)
- [📂 Repository Structure](#-repository-structure)
- [🛠️ How to Deploy](#️-how-to-deploy)
- [🧠 Lessons Learned](#-lessons-learned)
- [📸 Screenshots](#-screenshots)
- [📃 License](#-license)

---

## 🎯 Project Objectives

- Simulate a real-world online banking platform
- Implement complete CI/CD with GitHub Actions & Argo CD
- Apply DevSecOps practices: code scan, container scan, and runtime security
- Set up monitoring dashboards with Prometheus + Grafana
- Enable auto-remediation via Prometheus Alertmanager
- Demonstrate separation of CI and CD responsibilities

---

## 🧱 Tech Stack

### 💻 Application
- **Frontend:** Angular + NGINX
- **Backend:** Spring Boot (Java)
- **Database:** Azure MySQL

### 🐳 Containerization
- Docker
- Docker Compose (for local testing)

### ☁️ Cloud & Infrastructure
- Azure Kubernetes Service (AKS)
- Azure Container Registry (ACR)
- Azure MySQL Flexible Server (private access)
- Terraform (Infrastructure as Code)

### ⚙️ CI/CD & GitOps
- GitHub Actions (CI)
- Argo CD (CD with GitOps model)

### 🔒 Security
- Trivy (container scanning)
- SonarCloud (code scanning)
- OWASP ZAP (optional for dynamic scanning)

### 📈 Monitoring
- Prometheus
- Grafana
- Alertmanager
- Slack & Email Alerting

---

## ⚙️ Architecture Overview

```text
+----------------+     Git Push      +------------------------+
|   Developer    +------------------> GitHub Repo (CI/CD)    |
+----------------+                  +-----------+------------+
                                               |
                                               v
                            +--------------------------+
                            | GitHub Actions (CI)      |
                            | - Lint, test, build       |
                            | - Trivy + SonarCloud scan|
                            | - Push image to ACR       |
                            +--------------------------+
                                               |
                                               v
                                     +------------------+
                                     |   ACR (Docker)   |
                                     +--------+---------+
                                              |
                          +-------------------v--------------------+
                          |        Argo CD (GitOps CD)             |
                          | Watches GitHub for new image/manifest |
                          +-------------------+--------------------+
                                              |
                                              v
                            +-------------------------+
                            |     Azure AKS Cluster   |
                            +-----------+-------------+
                                        |
       +--------------------------+     +--------------------------+
       | Frontend (Angular+NGINX)| <--> | Backend (Spring Boot API)|
       +--------------------------+     +--------------------------+
                    |                              |
                    +------------+  +-------------+
                                 |  |
                          +------+--v-----+
                          | Azure MySQL   |
                          +--------------+

🚀 CI/CD Pipeline
✅ CI – GitHub Actions
Runs on commit/pull request

Performs:

Code linting

Unit tests

SonarCloud static analysis

Trivy vulnerability scan

Docker image build & push to ACR

🔁 CD – Argo CD
GitOps model: auto-syncs manifests and container tags from GitHub

Deploys updated images to AKS using Deployment.yaml

Can auto-recover to previous state if config drift is detected

🔐 DevSecOps Integrations
Tool	Purpose
Trivy	Scans Docker images for CVEs
SonarCloud	Analyzes source code for bugs, vulnerabilities, smells
OWASP ZAP	(Optional) Dynamic security scanning
Email & Discord Alerts	Notify on critical events

📊 Monitoring & Alerting
Installed via Helm using kube-prometheus-stack

Grafana Dashboards for:

Pod health

CPU & memory

API response metrics

Prometheus scrapes app + node metrics

Alertmanager triggers alerts:

Via Email

Via Slack webhook

Triggers remediation job

⚒️ Self-Healing Setup
Prometheus alert fires on crash/failure

Alertmanager sends webhook to Kubernetes

Job is triggered to:

Restart crashed pods

Scale deployments

Or send diagnostics

💡 You can test it by manually deleting a pod or simulating a crash.

🌐 Application Features
Feature	Description
Register/Login	Secure with hashed password and PIN
Deposit/Withdraw	Updates balance in real-time
Transaction Logs	Viewable from frontend via /api endpoints
Ingress Routing	Clean URLs via NGINX Ingress
Live Monitoring	See pod/API performance on Grafana dashboard
Secure Communication	All backend APIs routed behind proxy


🛠️ How to Deploy
Prerequisites
Azure CLI & Terraform installed

GitHub repository with secrets set

Docker installed

AKS + ACR + Azure MySQL provisioned

## 🚀 How to Deplo
1. Clone the repo
2. Configure your `dev.tfvars` in `/env`
3. Initialize Terraform:
   ```bash
   cd infra/components
   terraform init
   terraform apply -var-file="../env/dev.tfvars"

2️⃣ Push Code to GitHub
CI pipeline will:

Build + push Docker images

Apply Trivy + SonarCloud scan

3️⃣ Argo CD Auto Deploys
Argo CD syncs manifests + image tags

AKS cluster receives updated pods

🧠 Lessons Learned
Deep understanding of CI vs CD separation

How to implement GitOps with Argo CD

Setting up Slack + Email alerts

Handling real-world Ingress proxy + path rewrites

Connecting private Azure MySQL securely to backend

Creating a realistic DevSecOps pipeline from scratch


📃 License
MIT License. Feel free to fork, use, or contribute.

🙌 Thanks
This project is part of my journey to build advanced, real-world DevOps solutions and share them publicly.

Questions or feedback?
Connect with me on LinkedIn or reach out in the comments.