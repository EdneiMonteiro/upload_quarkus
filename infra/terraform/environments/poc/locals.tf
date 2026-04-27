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
