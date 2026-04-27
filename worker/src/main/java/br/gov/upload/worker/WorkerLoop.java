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
package br.gov.upload.worker;

import br.gov.upload.shared.model.ProcessingState;
import br.gov.upload.shared.model.ProcessingStatus;
import br.gov.upload.shared.model.UploadJobMessage;
import br.gov.upload.worker.service.BlobReader;
import br.gov.upload.worker.service.QueueService;
import br.gov.upload.worker.service.StatusRepository;
import io.quarkus.runtime.StartupEvent;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import jakarta.annotation.PreDestroy;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

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
                statusRepo.markFailed(status, e.getMessage());
                try {
                    queueService.sendToPoison(message);
                    queueService.delete(received.raw());
                } catch (Exception pe) {
                    LOG.errorf(pe, "failed to move message to poison queue: uploadId=%s", message.getUploadId());
                    continue;
                }
                LOG.errorf(e, "message processing failed: uploadId=%s", message.getUploadId());
            }
        }

        return processedCount;
    }

    private int processRows(UploadJobMessage message) throws InterruptedException {
        var rows = blobReader.iterRows(message.getBlobPath());
        int count = 0;

        while (rows.hasNext()) {
            rows.next();
            if (perRecordDelayMs > 0) {
                Thread.sleep(perRecordDelayMs);
            }
            count++;
        }

        return count;
    }

    private void sleepSafely(long millis) throws InterruptedException {
        if (millis <= 0) {
            return;
        }
        Thread.sleep(millis);
    }
}
