/*
 * Disclaimer
 * Notice: Any sample scripts, code, or commands comes with the following notification.
 *
 * This Sample Code is provided for the purpose of illustration only and is not intended to be used in a production
 * environment. THIS SAMPLE CODE AND ANY RELATED INFORMATION ARE PROVIDED "AS IS" WITHOUT WARRANTY OF ANY KIND, EITHER
 * EXPRESSED OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE IMPLIED WARRANTIES OF MERCHANTABILITY AND/OR FITNESS FOR A
 * PARTICULAR PURPOSE.
 *
 * We grant You a nonexclusive, royalty-free right to use and modify the Sample Code and to reproduce and distribute
 * the object code form of the Sample Code, provided that You agree: (i) to not use Our name, logo, or trademarks to
 * market Your software product in which the Sample Code is embedded; (ii) to include a valid copyright notice on Your
 * software product in which the Sample Code is embedded; and (iii) to indemnify, hold harmless, and defend Us and Our
 * suppliers from and against any claims or lawsuits, including attorneys' fees, that arise or result from the use or
 * distribution of the Sample Code.
 *
 * Please note: None of the conditions outlined in the disclaimer above will supersede the terms and conditions
 * contained within the Customers Support Services Description.
 */
package br.gov.upload.backend.service;

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
            return Optional.empty();
        } catch (RuntimeException e) {
            LOG.errorf(e, "falha inesperada ao montar status: uploadId=%s", uploadId);
            return Optional.empty();
        }
    }
}
