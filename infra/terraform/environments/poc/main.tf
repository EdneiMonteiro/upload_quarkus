# Disclaimer
# Notice: Any sample scripts, code, or commands comes with the following notification.
#
# This Sample Code is provided for the purpose of illustration only and is not intended to be used in a production
# environment. THIS SAMPLE CODE AND ANY RELATED INFORMATION ARE PROVIDED "AS IS" WITHOUT WARRANTY OF ANY KIND, EITHER
# EXPRESSED OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE IMPLIED WARRANTIES OF MERCHANTABILITY AND/OR FITNESS FOR A
# PARTICULAR PURPOSE.
#
# We grant You a nonexclusive, royalty-free right to use and modify the Sample Code and to reproduce and distribute
# the object code form of the Sample Code, provided that You agree: (i) to not use Our name, logo, or trademarks to
# market Your software product in which the Sample Code is embedded; (ii) to include a valid copyright notice on Your
# software product in which the Sample Code is embedded; and (iii) to indemnify, hold harmless, and defend Us and Our
# suppliers from and against any claims or lawsuits, including attorneys' fees, that arise or result from the use or
# distribution of the Sample Code.
#
# Please note: None of the conditions outlined in the disclaimer above will supersede the terms and conditions
# contained within the Customers Support Services Description.
# ---------- Resource Group ----------
resource "azurerm_resource_group" "this" {
  name     = var.resource_group_name
  location = var.location
  tags     = local.common_tags
}

# ---------- Log Analytics ----------
resource "azurerm_log_analytics_workspace" "this" {
  name                = local.log_name
  location            = azurerm_resource_group.this.location
  resource_group_name = azurerm_resource_group.this.name
  sku                 = "PerGB2018"
  retention_in_days   = 30
  tags                = local.common_tags
}

# ---------- ACR ----------
resource "azurerm_container_registry" "this" {
  name                = local.acr_name
  location            = azurerm_resource_group.this.location
  resource_group_name = azurerm_resource_group.this.name
  sku                 = "Basic"
  admin_enabled       = false
  tags                = local.common_tags
}

# ---------- VNet ----------
resource "azurerm_virtual_network" "this" {
  name                = local.vnet_name
  location            = azurerm_resource_group.this.location
  resource_group_name = azurerm_resource_group.this.name
  address_space       = ["10.50.0.0/16"]
  tags                = local.common_tags
}

resource "azurerm_subnet" "aks" {
  name                 = "snet-aks-${local.suffix}"
  resource_group_name  = azurerm_resource_group.this.name
  virtual_network_name = azurerm_virtual_network.this.name
  address_prefixes     = ["10.50.0.0/22"]
}

resource "azurerm_subnet" "pep" {
  name                              = "snet-pep-${local.suffix}"
  resource_group_name               = azurerm_resource_group.this.name
  virtual_network_name              = azurerm_virtual_network.this.name
  address_prefixes                  = ["10.50.4.0/27"]
  private_endpoint_network_policies = "Disabled"
}

# ---------- Storage ----------
resource "azurerm_storage_account" "this" {
  name                            = local.storage_name
  resource_group_name             = azurerm_resource_group.this.name
  location                        = azurerm_resource_group.this.location
  account_tier                    = "Standard"
  account_replication_type        = "LRS"
  min_tls_version                 = "TLS1_2"
  https_traffic_only_enabled      = true
  public_network_access_enabled   = true
  allow_nested_items_to_be_public = false
  shared_access_key_enabled       = true
  default_to_oauth_authentication = true
  tags                            = local.common_tags
}

resource "azapi_resource" "uploads_container" {
  type      = "Microsoft.Storage/storageAccounts/blobServices/containers@2023-05-01"
  name      = var.uploads_container_name
  parent_id = "${azurerm_storage_account.this.id}/blobServices/default"
  body = { properties = { publicAccess = "None" } }
}

resource "azapi_resource" "work_queue" {
  type      = "Microsoft.Storage/storageAccounts/queueServices/queues@2023-05-01"
  name      = var.work_queue_name
  parent_id = "${azurerm_storage_account.this.id}/queueServices/default"
  body = { properties = { metadata = {} } }
}

resource "azapi_resource" "poison_queue" {
  type      = "Microsoft.Storage/storageAccounts/queueServices/queues@2023-05-01"
  name      = var.poison_queue_name
  parent_id = "${azurerm_storage_account.this.id}/queueServices/default"
  body = { properties = { metadata = {} } }
}

resource "azapi_resource" "status_table" {
  type      = "Microsoft.Storage/storageAccounts/tableServices/tables@2023-05-01"
  name      = var.status_table_name
  parent_id = "${azurerm_storage_account.this.id}/tableServices/default"
  body = { properties = { signedIdentifiers = [] } }
}

# ---------- AKS ----------
resource "azurerm_kubernetes_cluster" "this" {
  name                = local.aks_name
  location            = azurerm_resource_group.this.location
  resource_group_name = azurerm_resource_group.this.name
  dns_prefix          = local.aks_name
  tags                = local.common_tags

  default_node_pool {
    name                 = "default"
    vm_size              = var.aks_vm_size
    vnet_subnet_id       = azurerm_subnet.aks.id
    node_count           = var.aks_node_count
    enable_auto_scaling  = true
    min_count            = var.aks_node_count
    max_count            = var.aks_max_node_count
  }

  identity {
    type = "SystemAssigned"
  }

  oidc_issuer_enabled       = true
  workload_identity_enabled = true

  oms_agent {
    log_analytics_workspace_id = azurerm_log_analytics_workspace.this.id
  }

  workload_autoscaler_profile {
    keda_enabled = true
  }

  network_profile {
    network_plugin = "azure"
    service_cidr   = "10.51.0.0/16"
    dns_service_ip = "10.51.0.10"
  }
}

# ---------- RBAC: AKS → ACR ----------
resource "azurerm_role_assignment" "aks_acr_pull" {
  scope                = azurerm_container_registry.this.id
  role_definition_name = "AcrPull"
  principal_id         = azurerm_kubernetes_cluster.this.kubelet_identity[0].object_id
}

# ---------- RBAC: AKS Workload Identity → Storage ----------
resource "azurerm_user_assigned_identity" "workload" {
  name                = "id-workload-${local.suffix}"
  location            = azurerm_resource_group.this.location
  resource_group_name = azurerm_resource_group.this.name
  tags                = local.common_tags
}

resource "azurerm_federated_identity_credential" "workload" {
  name                = "fic-workload-${local.suffix}"
  resource_group_name = azurerm_resource_group.this.name
  parent_id           = azurerm_user_assigned_identity.workload.id
  audience            = ["api://AzureADTokenExchange"]
  issuer              = azurerm_kubernetes_cluster.this.oidc_issuer_url
  subject             = "system:serviceaccount:upload:upload-workload"
}

resource "azurerm_role_assignment" "workload_blob" {
  scope                = azurerm_storage_account.this.id
  role_definition_name = "Storage Blob Data Contributor"
  principal_id         = azurerm_user_assigned_identity.workload.principal_id
}

resource "azurerm_role_assignment" "workload_queue" {
  scope                = azurerm_storage_account.this.id
  role_definition_name = "Storage Queue Data Contributor"
  principal_id         = azurerm_user_assigned_identity.workload.principal_id
}

resource "azurerm_role_assignment" "workload_table" {
  scope                = azurerm_storage_account.this.id
  role_definition_name = "Storage Table Data Contributor"
  principal_id         = azurerm_user_assigned_identity.workload.principal_id
}
