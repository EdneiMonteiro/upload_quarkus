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

import com.azure.identity.DefaultAzureCredentialBuilder;
import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.BlobServiceClientBuilder;
import jakarta.enterprise.context.ApplicationScoped;
import org.jboss.logging.Logger;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.io.BufferedReader;
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

    public Iterator<String[]> iterRows(String blobPath) {
        // Leitura em streaming para nao carregar arquivos grandes inteiros em memoria.
        var blobInputStream = containerClient.getBlobClient(blobPath).openInputStream();
        var reader = new BufferedReader(new InputStreamReader(blobInputStream, StandardCharsets.UTF_8));

        return new Iterator<>() {
            private String nextLine = readNext();

            private String readNext() {
                try {
                    String line = reader.readLine();
                    if (line == null) {
                        reader.close();
                    }
                    return line;
                } catch (IOException e) {
                    LOG.errorf(e, "falha ao ler blob CSV");
                    try {
                        reader.close();
                    } catch (IOException ignore) {
                        // sem acao adicional
                    }
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
        };
    }

    private static String[] parseCsvLine(String line) {
        return line.split(",", -1);
    }
}
