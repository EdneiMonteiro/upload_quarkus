# Upload Quarkus PoC

[![ORCID](https://img.shields.io/badge/ORCID-0009--0006--0765--4201-A6CE39?logo=orcid&logoColor=white)](https://orcid.org/0009-0006-0765-4201)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE)
[![Azure](https://img.shields.io/badge/Cloud-Azure-0078D4?logo=microsoftazure&logoColor=white)](#)
[![Last commit](https://img.shields.io/github/last-commit/EdneiMonteiro/upload_quarkus)](https://github.com/EdneiMonteiro/upload_quarkus/commits)

## Visão Geral

Este repositório contém código de exemplo / prova de conceito (PoC) com o objetivo de demonstrar como implementar processamento assíncrono de arquivos CSV no Azure, utilizando Quarkus 3.17, Java 21, AKS e KEDA com scale-to-zero.

Este projeto foi criado para fins de aprendizado, avaliação e experimentação.

## Aviso Importante

Este repositório contém **código de exemplo e não é destinado para uso em produção**.

Antes de utilizar qualquer parte deste projeto em um ambiente produtivo ou crítico, é essencial revisar, validar, proteger e adaptar o código conforme os requisitos da sua organização, incluindo:

- Segurança
- Escalabilidade
- Confiabilidade
- Monitoramento
- Observabilidade
- Custos
- Conformidade

Leia também:

- [DISCLAIMER.md](./DISCLAIMER.md)
- [SUPPORT.md](./SUPPORT.md)

## O que este exemplo demonstra

- Upload de arquivos CSV via frontend web (Qute)
- Armazenamento no Azure Blob Storage com enfileiramento via Azure Queue
- Processamento assíncrono por workers com auto-scaling via KEDA (0–10 pods)
- Rastreamento de status via Azure Table Storage
- Infraestrutura como código com Terraform (AKS, Storage, ACR, Workload Identity)
- Zero secrets com Workload Identity e DefaultAzureCredential

## Arquitetura

```
Usuário → Web (Qute) → Backend (REST API) → Azure Storage (Blob + Queue + Table)
                                                       ↓
                                              Worker (0–10 pods via KEDA)
```

## Pré-requisitos

- Java 21+
- Maven 3.9+
- Docker
- Azure CLI + `kubectl`
- Terraform 1.5+

## Como iniciar

1. Clone este repositório
2. Provisione a infraestrutura:
   ```bash
   cd infra/terraform/environments/poc
   terraform init && terraform apply
   ```
3. Build e deploy:
   ```bash
   mvn clean package -DskipTests
   ./deploy/deploy.sh
   ```
4. Execute em ambiente não produtivo
5. Valide o comportamento antes de qualquer adaptação

## Documentação

- [Arquitetura detalhada](docs/ARCHITECTURE.md)
- [Apresentação (slides HTML)](docs/presentation.html)
- [Relatório de teste](report.md)

## Suporte

Este projeto **não possui SLA nem suporte oficial**.

Veja [SUPPORT.md](./SUPPORT.md) para detalhes.

## Aviso Legal

O uso deste projeto está sujeito aos termos descritos em [DISCLAIMER.md](./DISCLAIMER.md).

## Contribuições

Contribuições podem ser aceitas a critério do mantenedor.

## Marcas Registradas (Trademarks)

Os nomes e serviços da Microsoft são utilizados apenas para fins descritivos.

Este projeto **não é afiliado, endossado ou suportado oficialmente pela Microsoft**.

O uso de marcas da Microsoft não deve sugerir qualquer tipo de parceria ou suporte oficial.
