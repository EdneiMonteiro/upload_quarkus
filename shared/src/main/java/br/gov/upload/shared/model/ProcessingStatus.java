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
