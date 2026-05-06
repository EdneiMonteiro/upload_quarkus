// Copyright (c) 2026 Ednei Monteiro. Licensed under the MIT License.
// See LICENSE and DISCLAIMER.md in the project root for details.
package br.gov.upload.worker;

import br.gov.upload.shared.model.ProcessingState;
import br.gov.upload.shared.model.ProcessingStatus;
import br.gov.upload.shared.model.UploadJobMessage;
import br.gov.upload.worker.service.BlobReader;
import br.gov.upload.worker.service.QueueService;
import br.gov.upload.shared.service.StatusRepository;
import io.quarkus.runtime.StartupEvent;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import jakarta.annotation.PreDestroy;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

@ApplicationScoped
public class WorkerLoop {

    private static final Logger LOG = Logger.getLogger(WorkerLoop.class);

    @Inject QueueService queueService;
    @Inject BlobReader blobReader;
    @Inject StatusRepository statusRepo;

    @ConfigProperty(name = "upload.worker.polling-interval-seconds", defaultValue = "10")
    int pollingIntervalSeconds;

    @ConfigProperty(name = "upload.worker.batch-size", defaultValue = "1")
    int batchSize;

    @ConfigProperty(name = "upload.worker.per-record-delay-ms", defaultValue = "0")
    int perRecordDelayMs;

    @ConfigProperty(name = "upload.worker.error-backoff-ms", defaultValue = "5000")
    int errorBackoffMs;

    @ConfigProperty(name = "upload.worker.max-dequeue-count", defaultValue = "5")
    int maxDequeueCount;

    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    void onStart(@Observes StartupEvent ev) {
        executor.submit(this::loop);
        LOG.info("Worker loop started");
    }

    @PreDestroy
    void onStop() {
        // Encerra o worker de forma limpa durante shutdown do pod.
        executor.shutdownNow();
        try {
            if (!executor.awaitTermination(5, TimeUnit.SECONDS)) {
                LOG.warn("worker loop nao finalizou no tempo esperado");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private void loop() {
        while (!Thread.currentThread().isInterrupted()) {
            try {
                int processed = processOnce();
                if (processed == 0) {
                    sleepSafely(pollingIntervalSeconds * 1000L);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            } catch (Exception e) {
                LOG.error("Worker loop error", e);
                try {
                    sleepSafely(errorBackoffMs);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }
    }

    int processOnce() {
        var messages = queueService.receive(batchSize);
        int processedCount = 0;

        for (var received : messages) {
            var message = received.parsed();
            var status = new ProcessingStatus(
                    message.getUploadId(), message.getCorrelationId(),
                    ProcessingState.QUEUED, message.getSubmittedAt(),
                    message.getSubmittedAt(), message.getSubmittedBy());

            try {
                status = statusRepo.markProcessing(status);
                int rowsProcessed = processRows(message);
                statusRepo.markCompleted(status, rowsProcessed);
                queueService.delete(received.raw());

                LOG.infof("message processed: uploadId=%s records=%d", message.getUploadId(), rowsProcessed);
                processedCount++;

            } catch (Exception e) {
                LOG.errorf(e, "message processing failed: uploadId=%s dequeueCount=%d",
                        message.getUploadId(), received.raw().getDequeueCount());

                if (received.raw().getDequeueCount() >= maxDequeueCount) {
                    statusRepo.markFailed(status, e.getMessage());
                    try {
                        queueService.sendToPoison(message);
                        queueService.delete(received.raw());
                    } catch (Exception pe) {
                        LOG.errorf(pe, "failed to move message to poison queue: uploadId=%s", message.getUploadId());
                        continue;
                    }
                    LOG.warnf("message moved to poison queue after %d attempts: uploadId=%s",
                            received.raw().getDequeueCount(), message.getUploadId());
                } else {
                    LOG.infof("message will be retried (attempt %d/%d): uploadId=%s",
                            received.raw().getDequeueCount(), maxDequeueCount, message.getUploadId());
                }
            }
        }

        return processedCount;
    }

    private int processRows(UploadJobMessage message) throws InterruptedException {
        try (var rows = blobReader.iterRows(message.getBlobPath())) {
            int count = 0;

            while (rows.hasNext()) {
                rows.next();
                if (perRecordDelayMs > 0) {
                    Thread.sleep(perRecordDelayMs);
                }
                count++;
            }

            return count;
        } catch (IOException e) {
            throw new RuntimeException("falha ao fechar stream do blob", e);
        }
    }

    private void sleepSafely(long millis) throws InterruptedException {
        if (millis <= 0) {
            return;
        }
        Thread.sleep(millis);
    }
}
