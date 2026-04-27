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
variable "tenant_id" {
  type    = string
  default = "5bda9e44-74f1-47eb-8741-460273dbb4bf"
}

variable "subscription_id" {
  type    = string
  default = "c5e0e3d6-4035-4e6b-aa64-cc8b5ec30745"
}

variable "resource_group_name" {
  type    = string
  default = "rg4quarkus"
}

variable "location" {
  type    = string
  default = "eastus2"
}

variable "environment_name" {
  type    = string
  default = "poc"
}

variable "workload_name" {
  type    = string
  default = "uploadq"
}

variable "global_suffix" {
  type    = string
  default = "qk01"
}

variable "aks_node_count" {
  type    = number
  default = 2
}

variable "aks_vm_size" {
  type    = string
  default = "Standard_B2s"
}

variable "aks_max_node_count" {
  type    = number
  default = 5
}

variable "uploads_container_name" {
  type    = string
  default = "uploads"
}

variable "work_queue_name" {
  type    = string
  default = "work-items"
}

variable "poison_queue_name" {
  type    = string
  default = "work-items-poison"
}

variable "status_table_name" {
  type    = string
  default = "uploadstatus"
}

variable "tags" {
  type = map(string)
  default = {
    environment = "poc"
    workload    = "upload-quarkus"
    managed_by  = "terraform"
  }
}
