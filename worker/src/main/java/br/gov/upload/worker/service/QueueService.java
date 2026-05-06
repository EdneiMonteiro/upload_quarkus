// Copyright (c) 2026 Ednei Monteiro. Licensed under the MIT License.
// See LICENSE and DISCLAIMER.md in the project root for details.
package br.gov.upload.worker.service;

import br.gov.upload.shared.model.UploadJobMessage;
import com.azure.identity.DefaultAzureCredentialBuilder;
import com.azure.storage.queue.QueueClient;
import com.azure.storage.queue.QueueClientBuilder;
import com.azure.storage.queue.models.QueueMessageItem;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Base64;
import java.util.List;
import java.util.Optional;

@ApplicationScoped
public class QueueService {

    private static final Logger LOG = Logger.getLogger(QueueService.class);
    private final QueueClient workQueue;
    private final QueueClient poisonQueue;
    private final ObjectMapper objectMapper;
    private final int visibilityTimeout;

    public QueueService(
            @ConfigProperty(name = "upload.storage.queue-service-url") String queueServiceUrl,
            @ConfigProperty(name = "upload.storage.work-queue-name") String workQueueName,
            @ConfigProperty(name = "upload.storage.poison-queue-name", defaultValue = "work-items-poison") String poisonQueueName,
            @ConfigProperty(name = "upload.worker.visibility-timeout-seconds", defaultValue = "3600") int visibilityTimeout,
            ObjectMapper objectMapper) {
        var credential = new DefaultAzureCredentialBuilder().build();
        this.workQueue = new QueueClientBuilder()
                .endpoint(queueServiceUrl).queueName(workQueueName)
                .credential(credential).buildClient();
        this.poisonQueue = new QueueClientBuilder()
                .endpoint(queueServiceUrl).queueName(poisonQueueName)
                .credential(credential).buildClient();
        this.objectMapper = objectMapper;
        this.visibilityTimeout = visibilityTimeout;
    }

    public record ReceivedMessage(QueueMessageItem raw, UploadJobMessage parsed) {}

    public List<ReceivedMessage> receive(int batchSize) {
        return workQueue.receiveMessages(batchSize, Duration.ofSeconds(visibilityTimeout), null, null)
                .stream()
                .map(msg -> {
                    try {
                        String decoded = new String(Base64.getDecoder().decode(msg.getBody().toString()), StandardCharsets.UTF_8);
                        var parsed = objectMapper.readValue(decoded, UploadJobMessage.class);
                        return Optional.of(new ReceivedMessage(msg, parsed));
                    } catch (Exception e) {
                        LOG.errorf(e, "falha ao desserializar mensagem da fila, descartando: messageId=%s", msg.getMessageId());
                        workQueue.deleteMessage(msg.getMessageId(), msg.getPopReceipt());
                        return Optional.<ReceivedMessage>empty();
                    }
                })
                .flatMap(Optional::stream)
                .toList();
    }

    public void delete(QueueMessageItem msg) {
        workQueue.deleteMessage(msg.getMessageId(), msg.getPopReceipt());
    }

    public void sendToPoison(UploadJobMessage message) {
        try {
            String json = objectMapper.writeValueAsString(message);
            String encoded = Base64.getEncoder().encodeToString(json.getBytes(StandardCharsets.UTF_8));
            poisonQueue.sendMessage(encoded);
        } catch (Exception e) {
            throw new RuntimeException("Failed to send to poison queue", e);
        }
    }
}
