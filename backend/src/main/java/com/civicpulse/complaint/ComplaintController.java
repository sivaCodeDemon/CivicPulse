package com.civicpulse.complaint;

import com.civicpulse.common.response.ApiResponse;
import com.civicpulse.common.response.PagedResponse;
import com.civicpulse.complaint.dto.ComplaintResponse;
import com.civicpulse.complaint.dto.CreateComplaintRequest;
import com.civicpulse.complaint.dto.UpdateComplaintStatusRequest;
import com.civicpulse.security.UserPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/complaints")
@RequiredArgsConstructor
@Tag(name = "Complaint Management", description = "Endpoints for reporting, viewing, and updating civic complaints")
public class ComplaintController {

    private final ComplaintService complaintService;
    private final com.civicpulse.storage.StorageService storageService;

    @PostMapping
    @PreAuthorize("hasAnyRole('CITIZEN', 'OFFICER', 'ADMIN')")
    @Operation(summary = "Submit a new civic complaint with geolocation")
    public ResponseEntity<ApiResponse<ComplaintResponse>> createComplaint(
            @Valid @RequestBody CreateComplaintRequest request,
            @AuthenticationPrincipal UserPrincipal currentUser) {
        ComplaintResponse response = complaintService.createComplaint(request, currentUser);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(response, "Complaint submitted successfully"));
    }

    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Get complaint details by ID (enforces citizen data privacy)")
    public ResponseEntity<ApiResponse<ComplaintResponse>> getComplaintById(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserPrincipal currentUser) {
        ComplaintResponse response = complaintService.getComplaintById(id, currentUser);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/my")
    @PreAuthorize("hasRole('CITIZEN')")
    @Operation(summary = "Get list of complaints submitted by the authenticated citizen")
    public ResponseEntity<ApiResponse<PagedResponse<ComplaintResponse>>> getMyComplaints(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String direction,
            @AuthenticationPrincipal UserPrincipal currentUser) {

        Sort sort = direction.equalsIgnoreCase("asc") ? Sort.by(sortBy).ascending() : Sort.by(sortBy).descending();
        Pageable pageable = PageRequest.of(page, size, sort);

        PagedResponse<ComplaintResponse> response = complaintService.getCitizenComplaints(currentUser, pageable);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('OFFICER', 'ADMIN')")
    @Operation(summary = "Get all municipal complaints with optional status and department filters")
    public ResponseEntity<ApiResponse<PagedResponse<ComplaintResponse>>> getAllComplaints(
            @RequestParam(required = false) ComplaintStatus status,
            @RequestParam(required = false) Long departmentId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String direction) {

        Sort sort = direction.equalsIgnoreCase("asc") ? Sort.by(sortBy).ascending() : Sort.by(sortBy).descending();
        Pageable pageable = PageRequest.of(page, size, sort);

        PagedResponse<ComplaintResponse> response = complaintService.getAllComplaints(status, departmentId, pageable);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('OFFICER', 'ADMIN')")
    @Operation(summary = "Update complaint status, department assignment, or priority")
    public ResponseEntity<ApiResponse<ComplaintResponse>> updateComplaintStatus(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateComplaintStatusRequest request,
            @AuthenticationPrincipal UserPrincipal currentUser) {
        ComplaintResponse response = complaintService.updateComplaintStatus(id, request, currentUser);
        return ResponseEntity.ok(ApiResponse.success(response, "Complaint status updated successfully"));
    }

    @PostMapping(value = "/{id}/images", consumes = org.springframework.http.MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Upload image attachment (JPEG, PNG, WebP up to 10MB) for a complaint")
    public ResponseEntity<ApiResponse<com.civicpulse.storage.dto.ComplaintImageResponse>> uploadImage(
            @PathVariable UUID id,
            @RequestParam("file") org.springframework.web.multipart.MultipartFile file,
            @RequestParam(value = "imageType", required = false) com.civicpulse.storage.ImageType imageType,
            @AuthenticationPrincipal UserPrincipal currentUser) {
        com.civicpulse.storage.dto.ComplaintImageResponse response =
                storageService.storeComplaintImage(id, file, imageType, currentUser);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(response, "Image uploaded successfully"));
    }

    @GetMapping("/{id}/images")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "List all image attachments for a complaint")
    public ResponseEntity<ApiResponse<java.util.List<com.civicpulse.storage.dto.ComplaintImageResponse>>> getComplaintImages(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserPrincipal currentUser) {
        java.util.List<com.civicpulse.storage.dto.ComplaintImageResponse> images =
                storageService.getComplaintImages(id, currentUser);
        return ResponseEntity.ok(ApiResponse.success(images));
    }

    @GetMapping("/{id}/images/{imageId}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Download/stream complaint photo attachment securely")
    public ResponseEntity<org.springframework.core.io.Resource> getComplaintImageFile(
            @PathVariable UUID id,
            @PathVariable UUID imageId,
            @AuthenticationPrincipal UserPrincipal currentUser) {
        com.civicpulse.storage.ComplaintImage image = storageService.getComplaintImageMetadata(id, imageId, currentUser);
        org.springframework.core.io.Resource resource = storageService.loadComplaintImageAsResource(id, imageId, currentUser);

        return ResponseEntity.ok()
                .contentType(org.springframework.http.MediaType.parseMediaType(image.getFileType()))
                .header(org.springframework.http.HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + image.getFileName() + "\"")
                .body(resource);
    }

    @PostMapping("/{id}/assign")
    @PreAuthorize("hasAnyRole('OFFICER', 'ADMIN')")
    @Operation(summary = "Assign a complaint to a municipal officer")
    public ResponseEntity<ApiResponse<com.civicpulse.complaint.dto.AssignmentResponse>> assignComplaint(
            @PathVariable UUID id,
            @jakarta.validation.Valid @RequestBody com.civicpulse.complaint.dto.AssignComplaintRequest request,
            @AuthenticationPrincipal UserPrincipal currentUser) {
        com.civicpulse.complaint.dto.AssignmentResponse response =
                complaintService.assignComplaintToOfficer(id, request, currentUser);
        return ResponseEntity.ok(ApiResponse.success(response, "Complaint assigned successfully"));
    }

    @GetMapping("/{id}/history")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Get audit trail of status lifecycle transitions for a complaint")
    public ResponseEntity<ApiResponse<java.util.List<com.civicpulse.complaint.dto.ComplaintStatusHistoryResponse>>> getStatusHistory(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserPrincipal currentUser) {
        java.util.List<com.civicpulse.complaint.dto.ComplaintStatusHistoryResponse> history =
                complaintService.getComplaintStatusHistory(id, currentUser);
        return ResponseEntity.ok(ApiResponse.success(history));
    }
}
