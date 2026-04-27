# Upload Quarkus PoC - Relatório de Teste de Carga

**Data**: 2026-04-27  
**Ambiente**: AKS (`aks-poc-uploadq`) — East US 2  
**Endpoint**: `http://135.224.188.51`

---

## Resumo

| Métrica | Valor |
|---|---|
| Arquivos enviados | 10 |
| Total de registros | 69.449 |
| Tempo de upload (10 arquivos) | 12s |
| Tempo total de processamento | 902s (~15min) |
| Workers ativos | 10 |
| Nós AKS | 4 (2 originais + 2 via autoscaler) |
| Falhas | 0 |
| Taxa de processamento | ~77 registros/s (agregada) |

---

## Resultados por Arquivo

| # | Arquivo | Linhas CSV | Registros Processados | Status | Upload ID |
|---|---|---:|---:|---|---|
| 01 | sample_01.csv | 5.633 | 5.634 | completed | `7fc85127` |
| 02 | sample_02.csv | 7.381 | 7.382 | completed | `1c225a87` |
| 03 | sample_03.csv | 8.238 | 8.239 | completed | `a09ffa70` |
| 04 | sample_04.csv | 5.511 | 5.512 | completed | `31d050da` |
| 05 | sample_05.csv | 7.150 | 7.151 | completed | `036a14ef` |
| 06 | sample_06.csv | 7.824 | 7.825 | completed | `1bbc4c47` |
| 07 | sample_07.csv | 5.552 | 5.553 | completed | `04b980a6` |
| 08 | sample_08.csv | 7.652 | 7.653 | completed | `4bb25a65` |
| 09 | sample_09.csv | 9.017 | 9.018 | completed | `4e4e0f13` |
| 10 | sample_10.csv | 5.481 | 5.482 | completed | `6ea7c552` |
| **Total** | | **69.439** | **69.449** | | |

> Nota: registros processados = linhas CSV + 1 (header contabilizado).

---

## Cronologia do Processamento

| Tempo (s) | Arquivos Concluídos | Observação |
|---:|---:|---|
| 0 | 0 | Upload dos 10 arquivos (12s) |
| 535 | 1 | Primeiro arquivo concluído (~9min) |
| 568 | 4 | Três arquivos menores concluem juntos |
| 702 | 5 | |
| 735 | 6 | |
| 768 | 8 | |
| 835 | 9 | |
| 902 | 10 | Último arquivo (9.018 registros) concluído |

---

## Infraestrutura

### Cluster AKS
- **VM Size**: Standard_B2s (2 vCPU, 4 GiB RAM)
- **Nós iniciais**: 2
- **Autoscaler**: habilitado (min=2, max=5)
- **Nós utilizados no teste**: 4 (autoscaler adicionou 2)

### Distribuição de Workers por Nó

| Nó | Workers | Outros Pods |
|---|---:|---|
| vmss000000 | 2 | — |
| vmss000001 | 2 | backend, web |
| vmss000002 | 2 | — |
| vmss000003 | 4 | — |

### KEDA Autoscaling
- **Trigger**: Azure Storage Queue (`work-items`)
- **queueLength**: 1 (1 worker por mensagem)
- **minReplicas**: 0 → **maxReplicas**: 10
- **Escalonamento**: 0 → 2 → 4 → 8 → 10 workers em ~2min

### Configuração do Worker
| Parâmetro | Valor |
|---|---|
| `WORKER_BATCH_SIZE` | 1 |
| `WORKER_PER_RECORD_DELAY_MS` | 100 |
| `WORKER_POLLING_INTERVAL_SECONDS` | 10 |
| CPU request / limit | 250m / 500m |
| Memory request / limit | 512Mi / 768Mi |

---

## Limpeza Pré-Teste

Antes do teste, os seguintes recursos foram limpos:
- **Blob Storage**: container `uploads` esvaziado (0 blobs)
- **Queue `work-items`**: mensagens removidas
- **Queue `work-items-poison`**: mensagens removidas
- **Table `uploadstatus`**: 24 entidades de testes anteriores deletadas

---

## Conclusão

O sistema processou com sucesso **69.449 registros** distribuídos em **10 arquivos CSV** usando **10 workers paralelos** em **~15 minutos**. O autoscaler de nós do AKS e o KEDA funcionaram corretamente, escalando automaticamente tanto a infraestrutura (nós) quanto os pods (workers) conforme a demanda da fila.
