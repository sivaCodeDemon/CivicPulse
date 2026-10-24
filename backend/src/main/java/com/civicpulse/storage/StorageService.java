package com.civicpulse.storage;

import com.civicpulse.security.UserPrincipal;
import com.civicpulse.storage.dto.ComplaintImageResponse;
import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

public interface StorageService {
    ComplaintImageResponse storeComplaintImage(UUID complaintId, MultipartFile file, ImageType imageType, UserPrincipal currentUser);
    Resource loadComplaintImageAsResource(UUID complaintId, UUID imageId, UserPrincipal currentUser);
    ComplaintImage getComplaintImageMetadata(UUID complaintId, UUID imageId, UserPrincipal currentUser);
    List<ComplaintImageResponse> getComplaintImages(UUID complaintId, UserPrincipal currentUser);
}
