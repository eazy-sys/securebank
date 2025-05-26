variable "location" {
  description = "Azure region for resources"
  type        = string
}

variable "project_name" {
  description = "Base name for tagging and resource names"
  type        = string
}

variable "environment" {
  description = "Deployment environment (dev/staging/prod)"
  type        = string
}

