package com.civicpulse.complaint.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AssignmentResponse {
    private UUID id;
    private UUID complaintId;
    private String complaintTitle;
    private UUID officerId;
    private String officerName;
    private UUID assignedById;
    private String assignedByName;
    private String notes;
    private Instant assignedAt;
    private Instant completedAt;
}
