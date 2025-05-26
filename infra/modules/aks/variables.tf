variable "location" {
  type        = string
  description = "Azure region"
}

variable "project_name" {
  type        = string
  description = "Project name"
}

variable "environment" {
  type        = string
  description = "Deployment environment"
}

variable "resource_group_name" {
  type        = string
  description = "Name of the resource group"
}

variable "node_count" {
  type        = number
  description = "Number of AKS nodes"
}

variable "node_size" {
  type        = string
  description = "VM size for AKS nodes"
}

variable "subnet_id" {
  type        = string
  description = "ID of the subnet to deploy AKS in"
}

variable "acr_id" {
  type        = string
  description = "ID of the Azure Container Registry to assign pull access to"
}

