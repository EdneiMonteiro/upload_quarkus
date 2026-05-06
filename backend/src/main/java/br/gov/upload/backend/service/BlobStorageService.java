// Copyright (c) 2026 Ednei Monteiro. Licensed under the MIT License.
// See LICENSE and DISCLAIMER.md in the project root for details.
package br.gov.upload.backend.service;

import com.azure.identity.DefaultAzureCredentialBuilder;
import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.BlobServiceClientBuilder;
import com.azure.storage.blob.models.BlobHttpHeaders;
import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.io.InputStream;

@ApplicationScoped
public class BlobStorageService {

    private final BlobContainerClient containerClient;

    public BlobStorageService(
            @ConfigProperty(name = "upload.storage.blob-service-url") String blobServiceUrl,
            @ConfigProperty(name = "upload.storage.uploads-container-name") String containerName) {
        this.containerClient = new BlobServiceClientBuilder()
                .endpoint(blobServiceUrl)
                .credential(new DefaultAzureCredentialBuilder().build())
                .buildClient()
                .getBlobContainerClient(containerName);
    }

    public void upload(String blobPath, InputStream data, long length, String contentType) {
        var headers = new BlobHttpHeaders().setContentType(contentType);
        containerClient.getBlobClient(blobPath)
                .upload(data, length, false);
        containerClient.getBlobClient(blobPath)
                .setHttpHeaders(headers);
    }
}
