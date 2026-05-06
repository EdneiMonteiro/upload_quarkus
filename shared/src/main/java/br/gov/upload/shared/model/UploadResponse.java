// Copyright (c) 2026 Ednei Monteiro. Licensed under the MIT License.
// See LICENSE and DISCLAIMER.md in the project root for details.
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
