// Copyright (c) 2026 Ednei Monteiro. Licensed under the MIT License.
// See LICENSE and DISCLAIMER.md in the project root for details.
package br.gov.upload.backend.service;

import br.gov.upload.shared.model.UploadJobMessage;
import com.azure.identity.DefaultAzureCredentialBuilder;
import com.azure.storage.queue.QueueClient;
import com.azure.storage.queue.QueueClientBuilder;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.enterprise.context.ApplicationScoped;
import org.jboss.logging.Logger;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

@ApplicationScoped
public class QueueStorageService {

    private static final Logger LOG = Logger.getLogger(QueueStorageService.class);
    private final QueueClient queueClient;
    private final ObjectMapper objectMapper;

    public QueueStorageService(
            @ConfigProperty(name = "upload.storage.queue-service-url") String queueServiceUrl,
            @ConfigProperty(name = "upload.storage.work-queue-name") String queueName,
            ObjectMapper objectMapper) {
        this.queueClient = new QueueClientBuilder()
                .endpoint(queueServiceUrl)
                .queueName(queueName)
                .credential(new DefaultAzureCredentialBuilder().build())
                .buildClient();
        this.objectMapper = objectMapper;
    }

    public void enqueue(UploadJobMessage message) {
        try {
            String json = objectMapper.writeValueAsString(message);
            String encoded = Base64.getEncoder().encodeToString(json.getBytes(StandardCharsets.UTF_8));
            queueClient.sendMessage(encoded);
            LOG.debugf("job enfileirado: uploadId=%s", message.getUploadId());
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to serialize queue message", e);
        }
    }
}
