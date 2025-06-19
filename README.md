# 🚀 SecureBank – DevSecOps Fintech Project

SecureBank is a fullstack **banking simulation app** deployed using **enterprise-grade DevSecOps practices** on **Azure Kubernetes Service (AKS)**. It includes:

- 🔐 Secure infrastructure (Terraform, private networking)
- 🚀 CI/CD automation (GitHub Actions, ArgoCD)
- 🐳 Dockerized frontend + backend
- 📊 Monitoring + alerts (Prometheus, Grafana)
- 🔍 Code & image scanning (SonarCloud, Trivy, Checkov)

---

;
## 📦 Stack Overview

| Layer              | Toolset                                 |
|-------------------|------------------------------------------|
| **Cloud**          | Azure (AKS, ACR, VNet, Key Vault)        |
| **IaC**            | Terraform (modular, production-grade)    |
| **App**            | React (frontend), Spring Boot (backend), MySQL |
| **Containerization** | Docker                                 |
| **CI/CD**          | GitHub Actions + ArgoCD (GitOps)         |
| **Security**       | Trivy, SonarCloud, Checkov               |
| **Monitoring**     | Prometheus + Grafana                     |

---

## 🧱 Folder Structure

infra/
├── components/ # Calls all modules
├── modules/ # Reusable Terraform modules (aks, acr, network)
├── env/ # Environment-specific tfvars files


---

## 🛠️ Features

- ✅ Fullstack banking app (React + Spring Boot + MySQL)
- ✅ Modular Terraform infrastructure
- ✅ GitHub Actions pipelines
- ✅ GitOps deployment with ArgoCD
- ✅ Scanning with SonarCloud, Trivy, and Checkov
- ✅ Live monitoring with Grafana dashboards

---

## 🚀 How to Deploy

1. Clone the repo
2. Configure your `dev.tfvars` in `/env`
3. Initialize Terraform:
   ```bash
   cd infra/components
   terraform init
   terraform apply -var-file="../env/dev.tfvars"
