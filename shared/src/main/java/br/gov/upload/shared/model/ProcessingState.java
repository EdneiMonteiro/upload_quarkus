// Copyright (c) 2026 Ednei Monteiro. Licensed under the MIT License.
// See LICENSE and DISCLAIMER.md in the project root for details.
package br.gov.upload.shared.model;

public enum ProcessingState {
    RECEIVED("received"),
    QUEUED("queued"),
    PROCESSING("processing"),
    COMPLETED("completed"),
    FAILED("failed"),
    POISONED("poisoned");

    private final String value;

    ProcessingState(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    public static ProcessingState fromValue(String value) {
        for (ProcessingState state : values()) {
            if (state.value.equalsIgnoreCase(value)) {
                return state;
            }
        }
        throw new IllegalArgumentException("Unknown processing state: " + value);
    }
}
