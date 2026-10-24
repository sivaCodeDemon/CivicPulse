package com.civicpulse.storage.dto;

import com.civicpulse.storage.ImageType;
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
public class ComplaintImageResponse {
    private UUID id;
    private UUID complaintId;
    private String fileName;
    private String fileType;
    private Long fileSizeBytes;
    private ImageType imageType;
    private String downloadUrl;
    private Instant createdAt;
}
