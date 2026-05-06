# Copyright (c) 2026 Ednei Monteiro. Licensed under the MIT License.
# See LICENSE and DISCLAIMER.md in the project root for details.
output "resource_group_name" {
  value = azurerm_resource_group.this.name
}

output "acr_login_server" {
  value = azurerm_container_registry.this.login_server
}

output "acr_name" {
  value = azurerm_container_registry.this.name
}

output "aks_name" {
  value = azurerm_kubernetes_cluster.this.name
}

output "storage_account_name" {
  value = azurerm_storage_account.this.name
}

output "blob_service_url" {
  value = local.blob_service_url
}

output "queue_service_url" {
  value = local.queue_service_url
}

output "table_service_url" {
  value = local.table_service_url
}

output "workload_identity_client_id" {
  value = azurerm_user_assigned_identity.workload.client_id
}

output "aks_oidc_issuer_url" {
  value = azurerm_kubernetes_cluster.this.oidc_issuer_url
}

output "storage_connection_string" {
  value     = azurerm_storage_account.this.primary_connection_string
  sensitive = true
}
