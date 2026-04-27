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
                        return new ReceivedMessage(msg, parsed);
                    } catch (Exception e) {
                        LOG.errorf(e, "falha ao desserializar mensagem da fila: messageId=%s", msg.getMessageId());
                        throw new RuntimeException("Failed to parse queue message", e);
                    }
                })
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
