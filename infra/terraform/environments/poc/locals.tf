# Copyright (c) 2026 Ednei Monteiro. Licensed under the MIT License.
# See LICENSE and DISCLAIMER.md in the project root for details.
locals {
  suffix       = "${var.environment_name}-${var.workload_name}"
  acr_name     = substr(replace("cr${var.environment_name}${var.workload_name}${var.global_suffix}", "-", ""), 0, 50)
  storage_name = substr(replace("st${var.environment_name}${var.workload_name}${var.global_suffix}", "-", ""), 0, 24)
  aks_name     = "aks-${local.suffix}"
  vnet_name    = "vnet-${local.suffix}"
  log_name     = "log-${local.suffix}"

  blob_service_url  = "https://${local.storage_name}.blob.core.windows.net"
  queue_service_url = "https://${local.storage_name}.queue.core.windows.net"
  table_service_url = "https://${local.storage_name}.table.core.windows.net"

  common_tags = merge(var.tags, { resource_group = var.resource_group_name })
}
