// Copyright (c) 2026 Ednei Monteiro. Licensed under the MIT License.
// See LICENSE and DISCLAIMER.md in the project root for details.
package br.gov.upload.worker.service;

import com.azure.identity.DefaultAzureCredentialBuilder;
import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.BlobServiceClientBuilder;
import jakarta.enterprise.context.ApplicationScoped;
import org.jboss.logging.Logger;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.io.BufferedReader;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Iterator;
import java.util.NoSuchElementException;

@ApplicationScoped
public class BlobReader {

    private static final Logger LOG = Logger.getLogger(BlobReader.class);
    private final BlobContainerClient containerClient;

    public BlobReader(
            @ConfigProperty(name = "upload.storage.blob-service-url") String blobServiceUrl,
            @ConfigProperty(name = "upload.storage.uploads-container-name") String containerName) {
        this.containerClient = new BlobServiceClientBuilder()
                .endpoint(blobServiceUrl)
                .credential(new DefaultAzureCredentialBuilder().build())
                .buildClient()
                .getBlobContainerClient(containerName);
    }

    public interface CloseableIterator<T> extends Iterator<T>, Closeable {}

    public CloseableIterator<String[]> iterRows(String blobPath) {
        // Leitura em streaming para nao carregar arquivos grandes inteiros em memoria.
        var blobInputStream = containerClient.getBlobClient(blobPath).openInputStream();
        var reader = new BufferedReader(new InputStreamReader(blobInputStream, StandardCharsets.UTF_8));

        return new CloseableIterator<>() {
            private String nextLine = readNext();
            private boolean closed = false;

            private String readNext() {
                try {
                    String line = reader.readLine();
                    if (line == null) {
                        close();
                    }
                    return line;
                } catch (IOException e) {
                    LOG.errorf(e, "falha ao ler blob CSV");
                    close();
                    return null;
                }
            }

            @Override
            public boolean hasNext() {
                return nextLine != null;
            }

            @Override
            public String[] next() {
                if (nextLine == null) throw new NoSuchElementException();
                String[] fields = parseCsvLine(nextLine);
                nextLine = readNext();
                return fields;
            }

            @Override
            public void close() {
                if (!closed) {
                    closed = true;
                    try {
                        reader.close();
                    } catch (IOException ignore) {
                        // sem acao adicional
                    }
                }
            }
        };
    }

    private static String[] parseCsvLine(String line) {
        return line.split(",", -1);
    }
}
