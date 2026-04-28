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
package br.gov.upload.backend.resource;

import br.gov.upload.backend.service.BlobStorageService;
import br.gov.upload.backend.service.QueueStorageService;
import br.gov.upload.backend.service.StatusRepository;
import br.gov.upload.shared.model.ProcessingState;
import br.gov.upload.shared.model.ProcessingStatus;
import br.gov.upload.shared.model.UploadJobMessage;
import br.gov.upload.shared.model.UploadResponse;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;
import org.jboss.resteasy.reactive.multipart.FileUpload;
import org.jboss.resteasy.reactive.RestForm;
import org.jboss.resteasy.reactive.RestHeader;

import java.io.FileInputStream;
import java.io.IOException;
import java.time.Instant;
import java.util.Locale;
import java.util.UUID;

@Path("/api/uploads")
public class UploadResource {

    private static final Logger LOG = Logger.getLogger(UploadResource.class);
    private static final String CSV_CONTENT_TYPE = "text/csv";
    private static final String FILE_REQUIRED_MESSAGE = "file is required";
    private static final String CSV_ONLY_MESSAGE = "only CSV uploads are supported";
    private static final String FILE_EMPTY_MESSAGE = "file is empty";
    private static final String PERSIST_FAILED_MESSAGE = "failed to persist upload";

    @Inject BlobStorageService blobStorage;
    @Inject QueueStorageService queueStorage;
    @Inject StatusRepository statusRepo;

    @ConfigProperty(name = "upload.upload.prefix", defaultValue = "uploads")
    String uploadPrefix;

    @ConfigProperty(name = "upload.storage.uploads-container-name")
    String containerName;

    @ConfigProperty(name = "upload.upload.default-submitted-by", defaultValue = "anonymous")
    String defaultSubmittedBy;

    @ConfigProperty(name = "upload.upload.chunk-strategy", defaultValue = "rows:1000")
    String chunkStrategy;

    @POST
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Produces(MediaType.APPLICATION_JSON)
    public Response createUpload(
            @RestForm("file") FileUpload file,
            @RestHeader("X-Correlation-ID") String correlationId,
            @RestHeader("X-Submitted-By") String submittedBy) {

        if (file == null || file.fileName() == null || file.fileName().isBlank()) {
            return badRequest(FILE_REQUIRED_MESSAGE);
        }

        String safeFileName = sanitizeFileName(file.fileName());
        if (!safeFileName.toLowerCase(Locale.ROOT).endsWith(".csv")) {
            return badRequest(CSV_ONLY_MESSAGE);
        }

        if (file.size() <= 0) {
            return badRequest(FILE_EMPTY_MESSAGE);
        }

        String uploadId = randomId();
        correlationId = isBlank(correlationId) ? randomId() : correlationId.trim();
        submittedBy = isBlank(submittedBy) ? defaultSubmittedBy : submittedBy.trim();

        String submittedAt = Instant.now().toString();
        String blobPath = uploadPrefix + "/" + uploadId + "/" + safeFileName;

        try (var inputStream = new FileInputStream(file.uploadedFile().toFile())) {
            blobStorage.upload(blobPath, inputStream, file.size(), CSV_CONTENT_TYPE);

            var message = new UploadJobMessage(uploadId, correlationId, blobPath,
                    containerName, submittedAt, submittedBy, CSV_CONTENT_TYPE, chunkStrategy);
            queueStorage.enqueue(message);

            var status = new ProcessingStatus(uploadId, correlationId,
                    ProcessingState.QUEUED, submittedAt, submittedAt, submittedBy);
            statusRepo.upsert(status);

            LOG.infof("upload accepted: uploadId=%s correlationId=%s", uploadId, correlationId);

            return Response.status(202)
                    .entity(new UploadResponse(uploadId, correlationId, ProcessingState.QUEUED.getValue()))
                    .build();

        } catch (IOException e) {
            LOG.errorf(e, "upload processing failed: uploadId=%s", uploadId);
            return Response.status(502).entity(new ErrorResponse(PERSIST_FAILED_MESSAGE)).build();
        }
    }

    @GET
    @Path("/{uploadId}/status")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getStatus(@PathParam("uploadId") String uploadId) {
        try {
            return statusRepo.get(uploadId)
                    .map(s -> Response.ok(s).build())
                    .orElse(Response.status(404).entity(new ErrorResponse("upload not found")).build());
        } catch (Exception e) {
            LOG.errorf(e, "falha ao consultar status: uploadId=%s", uploadId);
            return Response.status(502).entity(new ErrorResponse("failed to retrieve upload status")).build();
        }
    }

    private Response badRequest(String message) {
        return Response.status(400).entity(new ErrorResponse(message)).build();
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private static String randomId() {
        return UUID.randomUUID().toString().replace("-", "");
    }

    private static String sanitizeFileName(String fileName) {
        // Evita que o nome enviado pelo cliente injete diretorios no blob path.
        var normalized = fileName.replace('\\', '/');
        return normalized.substring(normalized.lastIndexOf('/') + 1);
    }

    public record ErrorResponse(String error) {}
}
