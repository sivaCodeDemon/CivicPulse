package com.civicpulse.complaint.dto;

import com.civicpulse.complaint.ComplaintStatus;
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
public class ComplaintStatusHistoryResponse {
    private UUID id;
    private UUID complaintId;
    private ComplaintStatus previousStatus;
    private ComplaintStatus newStatus;
    private UUID changedById;
    private String changedByName;
    private String remarks;
    private Instant createdAt;
}
