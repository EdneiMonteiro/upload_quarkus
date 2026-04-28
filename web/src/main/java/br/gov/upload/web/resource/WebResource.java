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
package br.gov.upload.web.resource;

import br.gov.upload.web.client.BackendClient;
import br.gov.upload.shared.model.ProcessingStatus;
import br.gov.upload.shared.model.UploadResponse;
import io.quarkus.qute.Template;
import io.quarkus.qute.TemplateInstance;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.jboss.logging.Logger;
import org.jboss.resteasy.reactive.multipart.FileUpload;
import org.jboss.resteasy.reactive.RestForm;

import java.io.File;
import java.net.URI;
import java.nio.file.Files;
import java.util.Locale;
import java.util.UUID;

@Path("/")
public class WebResource {

    private static final Logger LOG = Logger.getLogger(WebResource.class);

    @Inject Template index;
    @Inject Template status;
    @Inject @RestClient BackendClient backendClient;

    @ConfigProperty(name = "upload.web.default-submitted-by", defaultValue = "web-ui")
    String defaultSubmittedBy;

    @GET
    @Produces(MediaType.TEXT_HTML)
    public TemplateInstance indexPage() {
        return index.data("error", null);
    }

    @POST
    @Path("/uploads")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Produces(MediaType.TEXT_HTML)
    public Response upload(@RestForm("file") FileUpload file) {
        if (file == null || file.fileName() == null || file.fileName().isBlank()) {
            return Response.status(400)
                    .entity(index.data("error", "Selecione um arquivo CSV.").render())
                    .build();
        }

        String safeFileName = sanitizeFileName(file.fileName());
        if (!safeFileName.toLowerCase(Locale.ROOT).endsWith(".csv")) {
            return Response.status(400)
                .entity(index.data("error", "Somente arquivos CSV sao aceitos.").render())
                .build();
        }

        try {
            // Correlation ID explicito facilita rastreamento entre web, backend e worker.
            String correlationId = UUID.randomUUID().toString().replace("-", "");
            String submittedBy = defaultSubmittedBy == null || defaultSubmittedBy.isBlank()
                ? "web-ui"
                : defaultSubmittedBy;

            UploadResponse uploadResponse;
            // Symlink com nome original para que o REST client envie o filename correto no multipart.
            File namedFile = Files.createSymbolicLink(
                    file.uploadedFile().getParent().resolve(safeFileName),
                    file.uploadedFile()).toFile();
            try {
                uploadResponse = backendClient.createUpload(namedFile, correlationId, submittedBy);
            } finally {
                namedFile.delete();
            }

            return Response.seeOther(URI.create("/uploads/" + uploadResponse.getUploadId())).build();

        } catch (WebApplicationException e) {
            int statusCode = e.getResponse() != null ? e.getResponse().getStatus() : 502;
            LOG.errorf("backend returned HTTP %d for upload", statusCode);
            String message = statusCode >= 500
                ? "Falha temporaria ao enviar para o backend."
                : "Nao foi possivel enviar o arquivo informado.";
            return Response.status(statusCode)
                .entity(index.data("error", message).render())
                .build();
        } catch (Exception e) {
            LOG.error("backend upload request failed", e);
            return Response.status(502)
                    .entity(index.data("error", "Nao foi possivel enviar o arquivo para o backend.").render())
                    .build();
        }
    }

    @GET
    @Path("/uploads/{uploadId}")
    @Produces(MediaType.TEXT_HTML)
    public TemplateInstance statusPage(@PathParam("uploadId") String uploadId) {
        try {
            var processingStatus = backendClient.getStatus(uploadId);
            return status.data("uploadId", uploadId)
                    .data("status", processingStatus)
                    .data("error", null);

        } catch (NotFoundException e) {
            return status.data("uploadId", uploadId)
                    .data("status", null)
                    .data("error", "Status nao encontrado.");
        } catch (WebApplicationException e) {
            LOG.error("backend status request failed", e);
            return status.data("uploadId", uploadId)
                    .data("status", null)
                    .data("error", "Nao foi possivel consultar o backend neste momento.");
        } catch (Exception e) {
            LOG.error("backend status request failed", e);
            return status.data("uploadId", uploadId)
                    .data("status", null)
                    .data("error", "Nao foi possivel consultar o status do processamento.");
        }
    }

    private static String sanitizeFileName(String fileName) {
        var normalized = fileName.replace('\\', '/');
        return normalized.substring(normalized.lastIndexOf('/') + 1);
    }
}
