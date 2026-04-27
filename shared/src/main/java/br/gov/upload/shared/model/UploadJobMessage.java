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
