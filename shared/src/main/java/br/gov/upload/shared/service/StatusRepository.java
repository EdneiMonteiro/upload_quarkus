// Copyright (c) 2026 Ednei Monteiro. Licensed under the MIT License.
// See LICENSE and DISCLAIMER.md in the project root for details.
package br.gov.upload.shared.service;

import br.gov.upload.shared.model.ProcessingState;
import br.gov.upload.shared.model.ProcessingStatus;
import com.azure.data.tables.TableClient;
import com.azure.data.tables.TableClientBuilder;
import com.azure.data.tables.models.TableEntity;
import com.azure.data.tables.models.TableServiceException;
import com.azure.identity.DefaultAzureCredentialBuilder;
import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

import java.util.Optional;

@ApplicationScoped
public class StatusRepository {

    private static final Logger LOG = Logger.getLogger(StatusRepository.class);
    private static final String PARTITION_KEY = "upload";
    private final TableClient tableClient;

    public StatusRepository(
            @ConfigProperty(name = "upload.storage.table-service-url") String tableServiceUrl,
            @ConfigProperty(name = "upload.storage.status-table-name") String tableName) {
        this.tableClient = new TableClientBuilder()
                .endpoint(tableServiceUrl)
                .tableName(tableName)
                .credential(new DefaultAzureCredentialBuilder().build())
                .buildClient();
    }

    public void upsert(ProcessingStatus status) {
        var entity = new TableEntity(PARTITION_KEY, status.getUploadId());
        entity.addProperty("upload_id", status.getUploadId());
        entity.addProperty("correlation_id", status.getCorrelationId());
        entity.addProperty("state", status.getState());
        entity.addProperty("submitted_at", status.getSubmittedAt());
        entity.addProperty("updated_at", status.getUpdatedAt());
        entity.addProperty("submitted_by", status.getSubmittedBy());
        if (status.getRecordsProcessed() != null) {
            entity.addProperty("records_processed", status.getRecordsProcessed());
        }
        if (status.getErrorMessage() != null) {
            entity.addProperty("error_message", status.getErrorMessage());
        }
        tableClient.upsertEntity(entity);
        LOG.debugf("status atualizado: uploadId=%s state=%s", status.getUploadId(), status.getState());
    }

    public Optional<ProcessingStatus> get(String uploadId) {
        try {
            TableEntity entity = tableClient.getEntity(PARTITION_KEY, uploadId);
            var status = new ProcessingStatus();
            status.setUploadId((String) entity.getProperty("upload_id"));
            status.setCorrelationId((String) entity.getProperty("correlation_id"));
            status.setState((String) entity.getProperty("state"));
            status.setSubmittedAt((String) entity.getProperty("submitted_at"));
            status.setUpdatedAt((String) entity.getProperty("updated_at"));
            status.setSubmittedBy((String) entity.getProperty("submitted_by"));
            Object rp = entity.getProperty("records_processed");
            if (rp instanceof Number n) {
                status.setRecordsProcessed(n.intValue());
            }
            status.setErrorMessage((String) entity.getProperty("error_message"));
            return Optional.of(status);
        } catch (TableServiceException e) {
            if (e.getResponse() != null && e.getResponse().getStatusCode() == 404) {
                return Optional.empty();
            }
            LOG.errorf(e, "falha ao consultar status no Azure Table: uploadId=%s", uploadId);
            throw e;
        }
    }

    public ProcessingStatus markProcessing(ProcessingStatus status) {
        var updated = status.withState(ProcessingState.PROCESSING);
        upsert(updated);
        return updated;
    }

    public ProcessingStatus markCompleted(ProcessingStatus status, int recordsProcessed) {
        var updated = status.withCompleted(recordsProcessed);
        upsert(updated);
        return updated;
    }

    public ProcessingStatus markFailed(ProcessingStatus status, String errorMessage) {
        var updated = status.withFailed(errorMessage);
        upsert(updated);
        return updated;
    }
}
