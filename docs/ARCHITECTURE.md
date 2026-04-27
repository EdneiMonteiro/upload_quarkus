# Upload Quarkus PoC — Documentação de Arquitetura

## 1. Visão Geral da Arquitetura

```mermaid
flowchart LR
    subgraph Internet
        U[Usuário / Browser]
    end

    subgraph AKS["AKS Cluster (aks-poc-uploadq)"]
        direction TB
        WEB["Web<br/>(Quarkus + Qute)"]
        API["Backend<br/>(REST API)"]
        W1["Worker 1"]
        W2["Worker 2"]
        WN["Worker N"]
    end

    subgraph Azure["Azure Storage (stpocuploadqqk01)"]
        BLOB[(Blob Storage<br/>uploads/)]
        QUEUE[(Queue<br/>work-items)]
        POISON[(Queue<br/>work-items-poison)]
        TABLE[(Table Storage<br/>uploadstatus)]
    end

    subgraph Identity["Azure AD / Entra ID"]
        MI[User Assigned<br/>Managed Identity]
        FIC[Federated Identity<br/>Credential]
    end

    U -->|HTTP POST /uploads| WEB
    WEB -->|HTTP multipart| API
    API -->|upload CSV| BLOB
    API -->|enqueue job| QUEUE
    API -->|upsert status=QUEUED| TABLE
    QUEUE -->|poll messages| W1 & W2 & WN
    W1 & W2 & WN -->|download CSV| BLOB
    W1 & W2 & WN -->|update status| TABLE
    W1 & W2 & WN -.->|failed msgs| POISON
    U -->|GET /uploads/:id| WEB
    WEB -->|GET /api/uploads/:id/status| API
    API -->|get entity| TABLE

    MI -.->|OIDC federation| FIC
    FIC -.->|token exchange| AKS
```

## 2. Fluxo de Processamento

```mermaid
sequenceDiagram
    participant U as Usuário
    participant W as Web
    participant B as Backend
    participant Blob as Blob Storage
    participant Q as Queue
    participant T as Table Storage
    participant Wk as Worker

    U->>W: POST /uploads (CSV)
    W->>B: POST /api/uploads (multipart)
    B->>Blob: upload(uploads/{id}/{file}.csv)
    B->>Q: sendMessage(UploadJobMessage)
    B->>T: upsert(status=QUEUED)
    B-->>W: 202 Accepted {upload_id}
    W-->>U: 303 Redirect → /uploads/{id}

    loop Polling (10s)
        Wk->>Q: receiveMessages(batch=1)
        Q-->>Wk: Base64(JSON)
        Wk->>T: upsert(status=PROCESSING)
        Wk->>Blob: downloadStream(blob_path)
        loop Para cada linha CSV
            Wk->>Wk: processRow() + delay 100ms
        end
        Wk->>T: upsert(status=COMPLETED, records=N)
        Wk->>Q: deleteMessage(id, popReceipt)
    end

    U->>W: GET /uploads/{id}
    W->>B: GET /api/uploads/{id}/status
    B->>T: getEntity(upload, {id})
    T-->>B: ProcessingStatus
    B-->>W: JSON
    W-->>U: HTML (status page)
```

## 3. KEDA Autoscaling

```mermaid
flowchart TB
    subgraph KEDA
        SO[ScaledObject<br/>worker-scaledobject]
        HPA[HPA<br/>keda-hpa-worker]
    end

    subgraph AKS
        DEP[Deployment<br/>worker<br/>replicas: 0]
        P1[Pod 1]
        P2[Pod 2]
        PN[Pod N]
    end

    Q[(Queue<br/>work-items)] -->|queueLength trigger| SO
    SO -->|configura| HPA
    HPA -->|escala 0→10| DEP
    DEP --> P1 & P2 & PN

    style SO fill:#f9f,stroke:#333
    style Q fill:#69b,stroke:#333,color:#fff
```

**Parâmetros KEDA:**
- `pollingInterval`: 15s
- `cooldownPeriod`: 300s (5min)
- `minReplicaCount`: 0
- `maxReplicaCount`: 10
- `queueLength`: 1 (1 worker por mensagem na fila)

## 4. Cadeia de Identidade (Workload Identity)

```mermaid
flowchart LR
    subgraph Terraform
        UAI["User Assigned<br/>Managed Identity<br/>(id-workload-poc-uploadq)"]
        FIC["Federated Identity<br/>Credential"]
        RA1["Role: Storage Blob<br/>Data Contributor"]
        RA2["Role: Storage Queue<br/>Data Contributor"]
        RA3["Role: Storage Table<br/>Data Contributor"]
    end

    subgraph K8s["Kubernetes"]
        SA["ServiceAccount<br/>upload-workload<br/>annotation: client-id"]
        POD["Pod<br/>label: azure.workload.identity/use=true"]
    end

    subgraph Azure["Azure AD / Entra ID"]
        OIDC["OIDC Issuer<br/>(AKS)"]
        TOKEN["Access Token"]
    end

    subgraph Storage["Azure Storage"]
        BLOB[(Blob)]
        QUEUE[(Queue)]
        TABLE[(Table)]
    end

    UAI -->|federates| FIC
    FIC -->|subject: system:serviceaccount:upload:upload-workload| SA
    FIC -->|issuer| OIDC
    SA -->|mounts projected token| POD
    POD -->|exchanges OIDC token| TOKEN
    TOKEN -->|authorizes| BLOB & QUEUE & TABLE
    UAI --> RA1 & RA2 & RA3
    RA1 -->|scope: storage account| BLOB
    RA2 -->|scope: storage account| QUEUE
    RA3 -->|scope: storage account| TABLE
```

## 5. Identidades e RBAC

### 5.1 Managed Identities

| Identidade | Tipo | Finalidade |
|---|---|---|
| AKS System Assigned | System Assigned | Identidade do cluster AKS para gerenciar recursos (nós, rede) |
| AKS Kubelet Identity | System Assigned | Usada pelos nós para pull de imagens do ACR |
| `id-workload-poc-uploadq` | User Assigned | Identidade dos pods para acessar Azure Storage |

### 5.2 Federated Identity Credential

| Campo | Valor |
|---|---|
| **Name** | `fic-workload-poc-uploadq` |
| **Parent** | `id-workload-poc-uploadq` (User Assigned MI) |
| **Issuer** | OIDC Issuer URL do AKS |
| **Subject** | `system:serviceaccount:upload:upload-workload` |
| **Audience** | `api://AzureADTokenExchange` |

### 5.3 Role Assignments (RBAC)

| Role | Scope | Principal | Motivo |
|---|---|---|---|
| `AcrPull` | Container Registry | AKS Kubelet Identity | Pull de imagens Docker |
| `Storage Blob Data Contributor` | Storage Account | `id-workload-poc-uploadq` | Upload/download de CSVs |
| `Storage Queue Data Contributor` | Storage Account | `id-workload-poc-uploadq` | Enviar/receber/deletar mensagens |
| `Storage Table Data Contributor` | Storage Account | `id-workload-poc-uploadq` | CRUD de status na tabela |

### 5.4 Kubernetes ServiceAccount

```yaml
apiVersion: v1
kind: ServiceAccount
metadata:
  name: upload-workload
  namespace: upload
  annotations:
    azure.workload.identity/client-id: "${WORKLOAD_IDENTITY_CLIENT_ID}"
  labels:
    azure.workload.identity/use: "true"
```

O label `azure.workload.identity/use: "true"` nos Pods faz com que o mutating webhook do Workload Identity injete:
- Volume com token OIDC projetado
- Variáveis de ambiente: `AZURE_CLIENT_ID`, `AZURE_TENANT_ID`, `AZURE_FEDERATED_TOKEN_FILE`

## 6. Acesso ao Azure Storage via Java/Quarkus

### 6.1 Blob Storage

```java
// Backend — BlobStorageService.java
@ApplicationScoped
public class BlobStorageService {
    private final BlobContainerClient containerClient;

    public BlobStorageService(
            @ConfigProperty(name = "upload.storage.blob-service-url") String blobServiceUrl,
            @ConfigProperty(name = "upload.storage.uploads-container-name") String containerName) {
        this.containerClient = new BlobServiceClientBuilder()
                .endpoint(blobServiceUrl)                          // https://stpocuploadqqk01.blob.core.windows.net
                .credential(new DefaultAzureCredentialBuilder().build()) // Workload Identity → OIDC → Token
                .buildClient()
                .getBlobContainerClient(containerName);            // "uploads"
    }

    public void upload(String blobPath, InputStream data, long length, String contentType) {
        containerClient.getBlobClient(blobPath).upload(data, length, false);
        containerClient.getBlobClient(blobPath)
                .setHttpHeaders(new BlobHttpHeaders().setContentType(contentType));
    }
}
```

**Fluxo de autenticação:**
1. `DefaultAzureCredentialBuilder().build()` tenta múltiplas credenciais em cadeia
2. No AKS com Workload Identity, usa `WorkloadIdentityCredential`
3. Lê o token OIDC do arquivo montado (`AZURE_FEDERATED_TOKEN_FILE`)
4. Troca o token OIDC por um Access Token do Azure AD
5. Usa o Access Token para autenticar chamadas ao Blob Storage
6. O role `Storage Blob Data Contributor` autoriza as operações

### 6.2 Queue Storage

```java
// Backend — QueueStorageService.java
@ApplicationScoped
public class QueueStorageService {
    private final QueueClient queueClient;
    private final ObjectMapper objectMapper;

    public QueueStorageService(
            @ConfigProperty(name = "upload.storage.queue-service-url") String queueServiceUrl,
            @ConfigProperty(name = "upload.storage.work-queue-name") String queueName,
            ObjectMapper objectMapper) {
        this.queueClient = new QueueClientBuilder()
                .endpoint(queueServiceUrl)                              // https://stpocuploadqqk01.queue.core.windows.net
                .queueName(queueName)                                   // "work-items"
                .credential(new DefaultAzureCredentialBuilder().build()) // Workload Identity
                .buildClient();
        this.objectMapper = objectMapper;
    }

    public void enqueue(UploadJobMessage message) {
        String json = objectMapper.writeValueAsString(message);
        String encoded = Base64.getEncoder().encodeToString(json.getBytes());
        queueClient.sendMessage(encoded);                               // Base64-encoded JSON
    }
}
```

```java
// Worker — QueueService.java
@ApplicationScoped
public class QueueService {
    private final QueueClient workQueue;
    private final QueueClient poisonQueue;

    public QueueService(...) {
        var credential = new DefaultAzureCredentialBuilder().build();    // Reutiliza instância
        this.workQueue = new QueueClientBuilder()
                .endpoint(queueServiceUrl).queueName(workQueueName)
                .credential(credential).buildClient();
        this.poisonQueue = new QueueClientBuilder()
                .endpoint(queueServiceUrl).queueName(poisonQueueName)
                .credential(credential).buildClient();                  // Mesma credencial, outra fila
    }

    public List<ReceivedMessage> receive(int batchSize) {
        return workQueue.receiveMessages(batchSize, Duration.ofSeconds(visibilityTimeout), null, null)
                .stream()
                .map(msg -> {
                    String decoded = new String(Base64.getDecoder().decode(msg.getBody().toString()));
                    var parsed = objectMapper.readValue(decoded, UploadJobMessage.class);
                    return new ReceivedMessage(msg, parsed);
                }).toList();
    }

    public void delete(QueueMessageItem msg) {
        workQueue.deleteMessage(msg.getMessageId(), msg.getPopReceipt());
    }
}
```

**Fluxo de autenticação:**
1. Mesmo mecanismo do Blob: `DefaultAzureCredential` → `WorkloadIdentityCredential`
2. Mensagens codificadas em Base64 (requisito do Azure Queue Storage)
3. `visibilityTimeout` de 3600s garante que mensagens não são reprocessadas
4. O role `Storage Queue Data Contributor` autoriza send/receive/delete/peek

### 6.3 Table Storage

```java
// Backend e Worker — StatusRepository.java
@ApplicationScoped
public class StatusRepository {
    private static final String PARTITION_KEY = "upload";
    private final TableClient tableClient;

    public StatusRepository(
            @ConfigProperty(name = "upload.storage.table-service-url") String tableServiceUrl,
            @ConfigProperty(name = "upload.storage.status-table-name") String tableName) {
        this.tableClient = new TableClientBuilder()
                .endpoint(tableServiceUrl)                              // https://stpocuploadqqk01.table.core.windows.net
                .tableName(tableName)                                   // "uploadstatus"
                .credential(new DefaultAzureCredentialBuilder().build()) // Workload Identity
                .buildClient();
    }

    public void upsert(ProcessingStatus status) {
        var entity = new TableEntity(PARTITION_KEY, status.getUploadId());
        entity.addProperty("state", status.getState());
        // ... demais propriedades
        tableClient.upsertEntity(entity);                               // Create or Update
    }
}
```

**Fluxo de autenticação:** idêntico aos demais — `DefaultAzureCredential` com role `Storage Table Data Contributor`.

### 6.4 Dependências Maven (Azure SDK)

```xml
<!-- pom.xml pai -->
<azure-sdk.version>1.2.29</azure-sdk.version>
<azure-identity.version>1.14.2</azure-identity.version>
<azure-storage-blob.version>12.29.0</azure-storage-blob.version>
<azure-storage-queue.version>12.23.0</azure-storage-queue.version>
<azure-data-tables.version>12.5.0</azure-data-tables.version>
```

## 7. Estrutura de Módulos

```mermaid
graph TB
    PARENT["upload-parent (POM)"]
    SHARED["upload-shared<br/>DTOs / Models"]
    BACKEND["upload-backend<br/>REST API + Azure Storage"]
    WEB["upload-web<br/>HTML UI + REST Client"]
    WORKER["upload-worker<br/>Queue Poller + CSV Processor"]

    PARENT --> SHARED & BACKEND & WEB & WORKER
    BACKEND --> SHARED
    WEB --> SHARED
    WORKER --> SHARED
```
