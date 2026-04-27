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
package br.gov.upload.shared.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public class UploadResponse {

    @JsonProperty("upload_id")
    private String uploadId;

    @JsonProperty("correlation_id")
    private String correlationId;

    private String status;

    public UploadResponse() {}

    public UploadResponse(String uploadId, String correlationId, String status) {
        this.uploadId = uploadId;
        this.correlationId = correlationId;
        this.status = status;
    }

    public String getUploadId() { return uploadId; }
    public String getCorrelationId() { return correlationId; }
    public String getStatus() { return status; }

    public void setUploadId(String uploadId) { this.uploadId = uploadId; }
    public void setCorrelationId(String correlationId) { this.correlationId = correlationId; }
    public void setStatus(String status) { this.status = status; }
}
