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

variable "resource_group_name" {
  description = "Name of the Azure Resource Group"
  type        = string
  default     = null
}

variable "node_count" {
  description = "Number of AKS worker nodes"
  type        = number
  default     = 1
}

variable "node_size" {
  description = "VM size for AKS worker nodes"
  type        = string
  default     = "Standard_B2s"
}
