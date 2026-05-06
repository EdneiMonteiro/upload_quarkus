// Copyright (c) 2026 Ednei Monteiro. Licensed under the MIT License.
// See LICENSE and DISCLAIMER.md in the project root for details.
package br.gov.upload.shared.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Objects;

public class UploadJobMessage {

    @JsonProperty("upload_id")
    private String uploadId;

    @JsonProperty("correlation_id")
    private String correlationId;

    @JsonProperty("blob_path")
    private String blobPath;

    private String container;

    @JsonProperty("submitted_at")
    private String submittedAt;

    @JsonProperty("submitted_by")
    private String submittedBy;

    @JsonProperty("content_type")
    private String contentType;

    @JsonProperty("chunk_strategy")
    private String chunkStrategy;

    @JsonProperty("record_count_hint")
    private Integer recordCountHint;

    public UploadJobMessage() {}

    public UploadJobMessage(String uploadId, String correlationId, String blobPath,
                            String container, String submittedAt, String submittedBy,
                            String contentType, String chunkStrategy) {
        this.uploadId = Objects.requireNonNull(uploadId);
        this.correlationId = Objects.requireNonNull(correlationId);
        this.blobPath = Objects.requireNonNull(blobPath);
        this.container = Objects.requireNonNull(container);
        this.submittedAt = Objects.requireNonNull(submittedAt);
        this.submittedBy = Objects.requireNonNull(submittedBy);
        this.contentType = Objects.requireNonNull(contentType);
        this.chunkStrategy = Objects.requireNonNull(chunkStrategy);
    }

    public String getUploadId() { return uploadId; }
    public String getCorrelationId() { return correlationId; }
    public String getBlobPath() { return blobPath; }
    public String getContainer() { return container; }
    public String getSubmittedAt() { return submittedAt; }
    public String getSubmittedBy() { return submittedBy; }
    public String getContentType() { return contentType; }
    public String getChunkStrategy() { return chunkStrategy; }
    public Integer getRecordCountHint() { return recordCountHint; }

    public void setUploadId(String uploadId) { this.uploadId = uploadId; }
    public void setCorrelationId(String correlationId) { this.correlationId = correlationId; }
    public void setBlobPath(String blobPath) { this.blobPath = blobPath; }
    public void setContainer(String container) { this.container = container; }
    public void setSubmittedAt(String submittedAt) { this.submittedAt = submittedAt; }
    public void setSubmittedBy(String submittedBy) { this.submittedBy = submittedBy; }
    public void setContentType(String contentType) { this.contentType = contentType; }
    public void setChunkStrategy(String chunkStrategy) { this.chunkStrategy = chunkStrategy; }
    public void setRecordCountHint(Integer recordCountHint) { this.recordCountHint = recordCountHint; }
}
