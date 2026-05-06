# Copyright (c) 2026 Ednei Monteiro. Licensed under the MIT License.
# See LICENSE and DISCLAIMER.md in the project root for details.
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
