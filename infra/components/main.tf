resource "azurerm_resource_group" "this" {
  name     = "${var.project_name}-${var.environment}-rg"
  location = var.location
}

module "network" {
  source              = "../modules/network"
  location            = var.location
  project_name        = var.project_name
  environment         = var.environment
  resource_group_name = azurerm_resource_group.this.name
}

module "acr" {
  source              = "../modules/acr"
  location            = var.location
  project_name        = var.project_name
  environment         = var.environment
  resource_group_name = azurerm_resource_group.this.name
}

module "aks" {
  source              = "../modules/aks"
  location            = var.location
  project_name        = var.project_name
  environment         = var.environment
  resource_group_name = azurerm_resource_group.this.name
  acr_id              = module.acr.acr_id
  subnet_id           = module.network.aks_subnet_id
}
