package com.civicpulse.complaint.dto;

import com.civicpulse.complaint.ComplaintPriority;
import com.civicpulse.complaint.ComplaintSeverity;
import com.civicpulse.complaint.ComplaintStatus;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateComplaintStatusRequest {

    @NotNull(message = "New status is required")
    private ComplaintStatus status;

    @Size(max = 1000, message = "Remarks cannot exceed 1000 characters")
    private String remarks;

    private Long departmentId;

    private ComplaintPriority priority;

    private ComplaintSeverity severity;
}
