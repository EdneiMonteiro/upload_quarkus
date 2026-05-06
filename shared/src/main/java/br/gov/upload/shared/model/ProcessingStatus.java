// Copyright (c) 2026 Ednei Monteiro. Licensed under the MIT License.
// See LICENSE and DISCLAIMER.md in the project root for details.
package br.gov.upload.shared.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.Instant;
import java.util.Objects;

public class ProcessingStatus {

    @JsonProperty("upload_id")
    private String uploadId;

    @JsonProperty("correlation_id")
    private String correlationId;

    private String state;

    @JsonProperty("submitted_at")
    private String submittedAt;

    @JsonProperty("updated_at")
    private String updatedAt;

    @JsonProperty("submitted_by")
    private String submittedBy;

    @JsonProperty("records_processed")
    private Integer recordsProcessed;

    @JsonProperty("error_message")
    private String errorMessage;

    public ProcessingStatus() {}

    public ProcessingStatus(String uploadId, String correlationId, ProcessingState state,
                            String submittedAt, String updatedAt, String submittedBy) {
        this.uploadId = Objects.requireNonNull(uploadId);
        this.correlationId = Objects.requireNonNull(correlationId);
        this.state = state.getValue();
        this.submittedAt = submittedAt;
        this.updatedAt = updatedAt;
        this.submittedBy = submittedBy;
    }

    public ProcessingStatus withState(ProcessingState newState) {
        var copy = new ProcessingStatus(uploadId, correlationId, newState, submittedAt, Instant.now().toString(), submittedBy);
        copy.recordsProcessed = this.recordsProcessed;
        copy.errorMessage = this.errorMessage;
        return copy;
    }

    public ProcessingStatus withCompleted(int records) {
        var copy = withState(ProcessingState.COMPLETED);
        copy.recordsProcessed = records;
        return copy;
    }

    public ProcessingStatus withFailed(String error) {
        var copy = withState(ProcessingState.FAILED);
        copy.errorMessage = error;
        return copy;
    }

    public String getUploadId() { return uploadId; }
    public String getCorrelationId() { return correlationId; }
    public String getState() { return state; }
    public String getSubmittedAt() { return submittedAt; }
    public String getUpdatedAt() { return updatedAt; }
    public String getSubmittedBy() { return submittedBy; }
    public Integer getRecordsProcessed() { return recordsProcessed; }
    public String getErrorMessage() { return errorMessage; }

    public void setUploadId(String uploadId) { this.uploadId = uploadId; }
    public void setCorrelationId(String correlationId) { this.correlationId = correlationId; }
    public void setState(String state) { this.state = state; }
    public void setSubmittedAt(String submittedAt) { this.submittedAt = submittedAt; }
    public void setUpdatedAt(String updatedAt) { this.updatedAt = updatedAt; }
    public void setSubmittedBy(String submittedBy) { this.submittedBy = submittedBy; }
    public void setRecordsProcessed(Integer recordsProcessed) { this.recordsProcessed = recordsProcessed; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
}
