# Upload Quarkus PoC

Processamento assíncrono de arquivos CSV no Azure, usando **Quarkus 3.17**, **Java 21**, **AKS** e **KEDA**.

## Arquitetura

```
Usuário → Web (Qute) → Backend (REST API) → Azure Storage (Blob + Queue + Table)
                                                       ↓
                                              Worker (0–10 pods via KEDA)
```

- **Web** — Frontend Qute para upload de CSV e consulta de status
- **Backend** — API REST que armazena o CSV no Blob, enfileira o job na Queue e registra status na Table
- **Worker** — Consome a Queue, processa o CSV linha a linha e atualiza o status
- **Shared** — Contratos comuns (models, DTOs)

## Stack

| Componente | Tecnologia |
|---|---|
| Runtime | Java 21 (LTS) |
| Framework | Quarkus 3.17 |
| Cloud | Azure (AKS, Storage Account, ACR) |
| Autoscaling | KEDA (scale-to-zero, trigger por Azure Queue) |
| Identidade | Workload Identity (zero secrets) |
| IaC | Terraform |
| Container | Multi-stage Docker (Maven + JRE Alpine) |

## Estrutura do Projeto

```
├── backend/          # REST API (upload, status)
├── web/              # Frontend Qute (upload form, status page)
├── worker/           # Queue consumer (CSV processing)
├── shared/           # Contratos compartilhados
├── deploy/           # Scripts de deploy + manifests K8s
├── infra/terraform/  # IaC (AKS, Storage, ACR, Identidade, KEDA)
├── tests/            # Scripts de teste de carga + dados CSV
└── docs/             # Arquitetura e apresentação
```

## Pré-requisitos

- Java 21+
- Maven 3.9+
- Docker
- Azure CLI + `kubectl`
- Terraform 1.5+

## Build

```bash
mvn clean package -DskipTests
```

## Deploy

```bash
# 1. Provisionar infra
cd infra/terraform/environments/poc
terraform init && terraform apply

# 2. Build das imagens + deploy no AKS
./deploy/deploy.sh
```

## Teste de Carga

```bash
# Gerar CSVs de teste
python3 tests/generate_csv.py

# Upload e polling
./tests/upload_test.sh <WEB_URL>
./tests/poll_status.sh
```

### Resultados

| Métrica | Valor |
|---|---|
| Arquivos | 10 |
| Registros | 69.449 |
| Workers | 10 (auto-scaled) |
| Tempo total | ~15 min |
| Throughput | ~77 reg/s |
| Falhas | 0 |

## Segurança

- **Zero secrets** — Workload Identity via OIDC + Azure AD (`DefaultAzureCredential`)
- **RBAC** — Storage Blob/Queue/Table Data Contributor atribuído à Managed Identity
- **Rede** — AKS com Azure CNI em VNet dedicada

## Documentação

- [Arquitetura detalhada](docs/ARCHITECTURE.md)
- [Apresentação (slides HTML)](docs/presentation.html)
- [Relatório de teste](report.md)

## Disclaimer

This Sample Code is provided for the purpose of illustration only and is not intended to be used in a production environment. THIS SAMPLE CODE AND ANY RELATED INFORMATION ARE PROVIDED "AS IS" WITHOUT WARRANTY OF ANY KIND, EITHER EXPRESSED OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE IMPLIED WARRANTIES OF MERCHANTABILITY AND/OR FITNESS FOR A PARTICULAR PURPOSE.

We grant You a nonexclusive, royalty-free right to use and modify the Sample Code and to reproduce and distribute the object code form of the Sample Code, provided that You agree: (i) to not use Our name, logo, or trademarks to market Your software product in which the Sample Code is embedded; (ii) to include a valid copyright notice on Your software product in which the Sample Code is embedded; and (iii) to indemnify, hold harmless, and defend Us and Our suppliers from and against any claims or lawsuits, including attorneys' fees, that arise or result from the use or distribution of the Sample Code.

Please note: None of the conditions outlined in the disclaimer above will supersede the terms and conditions contained within the Customers Support Services Description.
