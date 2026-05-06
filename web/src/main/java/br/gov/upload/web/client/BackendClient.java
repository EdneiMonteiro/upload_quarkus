// Copyright (c) 2026 Ednei Monteiro. Licensed under the MIT License.
// See LICENSE and DISCLAIMER.md in the project root for details.
package br.gov.upload.web.client;

import br.gov.upload.shared.model.ProcessingStatus;
import br.gov.upload.shared.model.UploadResponse;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;
import org.jboss.resteasy.reactive.PartType;
import org.jboss.resteasy.reactive.RestForm;
import org.jboss.resteasy.reactive.RestHeader;

import java.io.File;

@RegisterRestClient(configKey = "backend-api")
@Path("/api/uploads")
public interface BackendClient {

    @POST
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Produces(MediaType.APPLICATION_JSON)
    UploadResponse createUpload(
            @RestForm("file") @PartType(MediaType.APPLICATION_OCTET_STREAM) File file,
            @RestHeader("X-Correlation-ID") String correlationId,
            @RestHeader("X-Submitted-By") String submittedBy);

    @GET
    @Path("/{uploadId}/status")
    @Produces(MediaType.APPLICATION_JSON)
    ProcessingStatus getStatus(@PathParam("uploadId") String uploadId);
}
