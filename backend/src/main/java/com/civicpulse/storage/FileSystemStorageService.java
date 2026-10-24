package com.civicpulse.storage;

import com.civicpulse.common.exception.BadRequestException;
import com.civicpulse.common.exception.ResourceNotFoundException;
import com.civicpulse.complaint.Complaint;
import com.civicpulse.complaint.ComplaintRepository;
import com.civicpulse.security.UserPrincipal;
import com.civicpulse.storage.dto.ComplaintImageResponse;
import com.civicpulse.user.RoleName;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.*;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class FileSystemStorageService implements StorageService {

    private static final Logger log = LoggerFactory.getLogger(FileSystemStorageService.class);
    private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of(
            "image/jpeg",
            "image/png",
            "image/webp"
    );
    private static final long MAX_FILE_SIZE = 10 * 1024 * 1024; // 10MB

    @Value("${app.storage.upload-dir:./uploads}")
    private String uploadDir;

    private final ComplaintRepository complaintRepository;
    private final ComplaintImageRepository complaintImageRepository;
    private Path rootLocation;

    @PostConstruct
    public void init() {
        try {
            this.rootLocation = Paths.get(uploadDir).toAbsolutePath().normalize();
            Files.createDirectories(this.rootLocation);
            log.info("Initialized secure file storage location at: {}", this.rootLocation);
        } catch (IOException e) {
            throw new RuntimeException("Could not initialize storage directory", e);
        }
    }

    @Override
    @Transactional
    public ComplaintImageResponse storeComplaintImage(UUID complaintId, MultipartFile file, ImageType imageType, UserPrincipal currentUser) {
        Complaint complaint = complaintRepository.findById(complaintId)
                .orElseThrow(() -> new ResourceNotFoundException("Complaint not found: " + complaintId));

        verifyAccess(complaint, currentUser);

        if (file.isEmpty()) {
            throw new BadRequestException("Uploaded file is empty");
        }

        if (file.getSize() > MAX_FILE_SIZE) {
            throw new BadRequestException("File size exceeds maximum permitted limit of 10MB");
        }

        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_CONTENT_TYPES.contains(contentType.toLowerCase())) {
            throw new BadRequestException("Invalid file type. Only JPEG, PNG, and WebP images are permitted.");
        }

        String originalFilename = StringUtils.cleanPath(Objects.requireNonNullElse(file.getOriginalFilename(), "image.jpg"));
        String extension = getFileExtension(originalFilename);

        String storedFileName = UUID.randomUUID() + (extension.isEmpty() ? ".jpg" : "." + extension);
        Path destinationFile = this.rootLocation.resolve(storedFileName).normalize().toAbsolutePath();

        if (!destinationFile.getParent().equals(this.rootLocation)) {
            throw new BadRequestException("Invalid path traversal attempted in filename");
        }

        try {
            Files.copy(file.getInputStream(), destinationFile, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            log.error("Failed to store file on disk", e);
            throw new RuntimeException("Failed to store file: " + e.getMessage(), e);
        }

        ComplaintImage complaintImage = ComplaintImage.builder()
                .complaint(complaint)
                .fileName(originalFilename)
                .storagePath(storedFileName)
                .fileType(contentType)
                .fileSizeBytes(file.getSize())
                .imageType(imageType != null ? imageType : ImageType.COMPLAINT_ATTACHMENT)
                .build();

        ComplaintImage saved = complaintImageRepository.save(complaintImage);
        return mapToResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public Resource loadComplaintImageAsResource(UUID complaintId, UUID imageId, UserPrincipal currentUser) {
        ComplaintImage image = getComplaintImageMetadata(complaintId, imageId, currentUser);

        try {
            Path file = this.rootLocation.resolve(image.getStoragePath()).normalize();
            Resource resource = new UrlResource(file.toUri());

            if (resource.exists() && resource.isReadable()) {
                return resource;
            } else {
                throw new ResourceNotFoundException("Could not read image file: " + image.getFileName());
            }
        } catch (MalformedURLException e) {
            throw new ResourceNotFoundException("Error loading image resource", "file", image.getFileName());
        }
    }

    @Override
    @Transactional(readOnly = true)
    public ComplaintImage getComplaintImageMetadata(UUID complaintId, UUID imageId, UserPrincipal currentUser) {
        Complaint complaint = complaintRepository.findById(complaintId)
                .orElseThrow(() -> new ResourceNotFoundException("Complaint not found: " + complaintId));

        verifyAccess(complaint, currentUser);

        return complaintImageRepository.findByIdAndComplaintId(imageId, complaintId)
                .orElseThrow(() -> new ResourceNotFoundException("Image not found with id: " + imageId));
    }

    @Override
    @Transactional(readOnly = true)
    public List<ComplaintImageResponse> getComplaintImages(UUID complaintId, UserPrincipal currentUser) {
        Complaint complaint = complaintRepository.findById(complaintId)
                .orElseThrow(() -> new ResourceNotFoundException("Complaint not found: " + complaintId));

        verifyAccess(complaint, currentUser);

        return complaintImageRepository.findByComplaintId(complaintId).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    private void verifyAccess(Complaint complaint, UserPrincipal currentUser) {
        boolean isOfficerOrAdmin = currentUser.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals(RoleName.ROLE_OFFICER.name()) ||
                               a.getAuthority().equals(RoleName.ROLE_ADMIN.name()));

        if (!isOfficerOrAdmin && !complaint.getCitizen().getId().equals(currentUser.getId())) {
            throw new AccessDeniedException("You do not have permission to access images for this complaint");
        }
    }

    private String getFileExtension(String filename) {
        int dotIndex = filename.lastIndexOf('.');
        return (dotIndex > 0 && dotIndex < filename.length() - 1) ? filename.substring(dotIndex + 1).toLowerCase() : "";
    }

    private ComplaintImageResponse mapToResponse(ComplaintImage image) {
        return ComplaintImageResponse.builder()
                .id(image.getId())
                .complaintId(image.getComplaint().getId())
                .fileName(image.getFileName())
                .fileType(image.getFileType())
                .fileSizeBytes(image.getFileSizeBytes())
                .imageType(image.getImageType())
                .downloadUrl("/api/v1/complaints/" + image.getComplaint().getId() + "/images/" + image.getId())
                .createdAt(image.getCreatedAt())
                .build();
    }
}
