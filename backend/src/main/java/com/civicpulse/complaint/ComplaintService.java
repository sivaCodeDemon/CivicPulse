package com.civicpulse.complaint;

import com.civicpulse.common.exception.BadRequestException;
import com.civicpulse.common.exception.ResourceNotFoundException;
import com.civicpulse.common.response.PagedResponse;
import com.civicpulse.complaint.dto.ComplaintResponse;
import com.civicpulse.complaint.dto.CreateComplaintRequest;
import com.civicpulse.complaint.dto.UpdateComplaintStatusRequest;
import com.civicpulse.department.Department;
import com.civicpulse.department.DepartmentRepository;
import com.civicpulse.security.UserPrincipal;
import com.civicpulse.user.RoleName;
import com.civicpulse.user.User;
import com.civicpulse.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ComplaintService {

    private final ComplaintRepository complaintRepository;
    private final UserRepository userRepository;
    private final DepartmentRepository departmentRepository;
    private final com.civicpulse.ai.AiInferenceService aiInferenceService;
    private final com.civicpulse.ai.DuplicateDetectionService duplicateDetectionService;
    private final ComplaintStatusHistoryRepository statusHistoryRepository;
    private final AssignmentRepository assignmentRepository;
    private final com.civicpulse.notification.NotificationService notificationService;

    @Transactional
    public ComplaintResponse createComplaint(CreateComplaintRequest request, UserPrincipal currentUser) {
        User citizen = userRepository.findById(currentUser.getId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + currentUser.getId()));

        Department department = resolveDepartment(request.getDepartmentId(), request.getCategory());

        Instant now = Instant.now();
        int slaHours = (department != null) ? department.getDefaultSlaHours() : 48;
        Instant slaDeadline = now.plus(slaHours, ChronoUnit.HOURS);

        // 1. Run AI Inference for categorization, confidence, and severity suggestions
        com.civicpulse.ai.dto.AiAnalysisResult aiResult = aiInferenceService.analyzeComplaint(
                request.getTitle(), request.getDescription(), request.getCategory());

        // 2. Run Duplicate Detection Engine (Geographic distance + text similarity)
        com.civicpulse.ai.dto.DuplicateMatchResult duplicateResult = duplicateDetectionService.findPotentialDuplicate(
                request.getLatitude(), request.getLongitude(), request.getTitle(), request.getDescription(), request.getCategory());

        ComplaintPriority effectivePriority = request.getPriority() != null ?
                request.getPriority() : aiResult.getSuggestedPriority();
        ComplaintSeverity effectiveSeverity = request.getSeverity() != null ?
                request.getSeverity() : aiResult.getSuggestedSeverity();

        Complaint complaint = Complaint.builder()
                .citizen(citizen)
                .department(department)
                .title(request.getTitle())
                .description(request.getDescription())
                .category(request.getCategory())
                .status(ComplaintStatus.REPORTED)
                .priority(effectivePriority)
                .severity(effectiveSeverity)
                .latitude(request.getLatitude())
                .longitude(request.getLongitude())
                .addressText(request.getAddressText())
                .isPotentialDuplicate(duplicateResult.isDuplicate())
                .duplicateOfId(duplicateResult.getMatchedComplaintId())
                .aiConfidenceScore(aiResult.getConfidenceScore())
                .aiSuggestedCategory(aiResult.getSuggestedCategory().name())
                .slaDeadline(slaDeadline)
                .build();

        Complaint saved = complaintRepository.save(complaint);

        // Record initial status history audit entry
        statusHistoryRepository.save(ComplaintStatusHistory.builder()
                .complaint(saved)
                .previousStatus(null)
                .newStatus(ComplaintStatus.REPORTED)
                .changedBy(citizen)
                .remarks("Complaint submitted by citizen")
                .build());

        // Send submission confirmation notification to citizen
        notificationService.sendNotification(
                citizen,
                saved,
                "Complaint Registered: " + saved.getTitle(),
                String.format("Your complaint #%s has been successfully logged. Estimated resolution SLA is %d hours.",
                        saved.getId().toString().substring(0, 8), slaHours),
                com.civicpulse.notification.NotificationType.COMPLAINT_SUBMITTED
        );

        return mapToResponse(saved);
    }

    @Transactional(readOnly = true)
    public ComplaintResponse getComplaintById(UUID id, UserPrincipal currentUser) {
        Complaint complaint = complaintRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Complaint not found with id: " + id));

        boolean isOfficerOrAdmin = currentUser.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals(RoleName.ROLE_OFFICER.name()) ||
                               a.getAuthority().equals(RoleName.ROLE_ADMIN.name()));

        if (!isOfficerOrAdmin && !complaint.getCitizen().getId().equals(currentUser.getId())) {
            throw new AccessDeniedException("You are not authorized to view this complaint");
        }

        return mapToResponse(complaint);
    }

    @Transactional(readOnly = true)
    public PagedResponse<ComplaintResponse> getCitizenComplaints(UserPrincipal currentUser, Pageable pageable) {
        Page<Complaint> page = complaintRepository.findByCitizenId(currentUser.getId(), pageable);
        return mapToPagedResponse(page);
    }

    @Transactional(readOnly = true)
    public PagedResponse<ComplaintResponse> getAllComplaints(ComplaintStatus status, Long departmentId, Pageable pageable) {
        Page<Complaint> page;
        if (status != null) {
            page = complaintRepository.findByStatus(status, pageable);
        } else if (departmentId != null) {
            page = complaintRepository.findByDepartmentId(departmentId, pageable);
        } else {
            page = complaintRepository.findAll(pageable);
        }
        return mapToPagedResponse(page);
    }

    @Transactional
    public ComplaintResponse updateComplaintStatus(UUID id, UpdateComplaintStatusRequest request, UserPrincipal currentUser) {
        Complaint complaint = complaintRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Complaint not found with id: " + id));

        User officerUser = userRepository.findById(currentUser.getId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + currentUser.getId()));

        ComplaintStatus previousStatus = complaint.getStatus();
        validateStatusTransition(previousStatus, request.getStatus());

        complaint.setStatus(request.getStatus());

        if (request.getStatus() == ComplaintStatus.RESOLVED) {
            complaint.setResolvedAt(Instant.now());
        } else if (request.getStatus() == ComplaintStatus.REOPENED) {
            complaint.setResolvedAt(null);
        }

        if (request.getDepartmentId() != null) {
            Department dept = departmentRepository.findById(request.getDepartmentId())
                    .orElseThrow(() -> new ResourceNotFoundException("Department not found: " + request.getDepartmentId()));
            complaint.setDepartment(dept);
        }

        if (request.getPriority() != null) {
            complaint.setPriority(request.getPriority());
        }
        if (request.getSeverity() != null) {
            complaint.setSeverity(request.getSeverity());
        }

        Complaint updated = complaintRepository.save(complaint);

        // Audit status history transition
        statusHistoryRepository.save(ComplaintStatusHistory.builder()
                .complaint(updated)
                .previousStatus(previousStatus)
                .newStatus(request.getStatus())
                .changedBy(officerUser)
                .remarks(request.getRemarks() != null ? request.getRemarks() : "Status updated by municipal officer")
                .build());

        // Dispatch notification to reporting citizen
        com.civicpulse.notification.NotificationType notificationType = request.getStatus() == ComplaintStatus.RESOLVED ?
                com.civicpulse.notification.NotificationType.COMPLAINT_RESOLVED :
                com.civicpulse.notification.NotificationType.STATUS_UPDATED;

        notificationService.sendNotification(
                complaint.getCitizen(),
                updated,
                "Complaint Status Updated: " + request.getStatus(),
                String.format("Complaint '%s' has transitioned to %s. Remarks: %s",
                        updated.getTitle(), request.getStatus(),
                        request.getRemarks() != null ? request.getRemarks() : "No additional remarks"),
                notificationType
        );

        return mapToResponse(updated);
    }

    @Transactional
    public com.civicpulse.complaint.dto.AssignmentResponse assignComplaintToOfficer(
            UUID complaintId, com.civicpulse.complaint.dto.AssignComplaintRequest request, UserPrincipal currentUser) {

        Complaint complaint = complaintRepository.findById(complaintId)
                .orElseThrow(() -> new ResourceNotFoundException("Complaint not found with id: " + complaintId));

        User officer = userRepository.findById(request.getOfficerId())
                .orElseThrow(() -> new ResourceNotFoundException("Officer not found with id: " + request.getOfficerId()));

        boolean isOfficer = officer.getRoles().stream()
                .anyMatch(r -> r.getName() == RoleName.ROLE_OFFICER || r.getName() == RoleName.ROLE_ADMIN);
        if (!isOfficer) {
            throw new BadRequestException("Selected user is not a municipal officer: " + officer.getEmail());
        }

        User assignedBy = userRepository.findById(currentUser.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Assigner not found: " + currentUser.getId()));

        Assignment assignment = Assignment.builder()
                .complaint(complaint)
                .officer(officer)
                .assignedBy(assignedBy)
                .notes(request.getNotes())
                .build();

        Assignment saved = assignmentRepository.save(assignment);

        // Transition complaint status to ASSIGNED if currently REPORTED or VERIFIED
        if (complaint.getStatus() == ComplaintStatus.REPORTED || complaint.getStatus() == ComplaintStatus.VERIFIED) {
            ComplaintStatus prev = complaint.getStatus();
            complaint.setStatus(ComplaintStatus.ASSIGNED);
            complaintRepository.save(complaint);

            statusHistoryRepository.save(ComplaintStatusHistory.builder()
                    .complaint(complaint)
                    .previousStatus(prev)
                    .newStatus(ComplaintStatus.ASSIGNED)
                    .changedBy(assignedBy)
                    .remarks("Assigned to officer " + officer.getFullName())
                    .build());
        }

        // Send notifications to both assigned officer and reporting citizen
        notificationService.sendNotification(
                officer,
                complaint,
                "New Task Assigned: " + complaint.getTitle(),
                "You have been assigned to resolve complaint #" + complaint.getId().toString().substring(0, 8),
                com.civicpulse.notification.NotificationType.COMPLAINT_ASSIGNED
        );

        notificationService.sendNotification(
                complaint.getCitizen(),
                complaint,
                "Officer Assigned to your complaint",
                String.format("Officer %s has been assigned to resolve '%s'.", officer.getFullName(), complaint.getTitle()),
                com.civicpulse.notification.NotificationType.STATUS_UPDATED
        );

        return com.civicpulse.complaint.dto.AssignmentResponse.builder()
                .id(saved.getId())
                .complaintId(complaint.getId())
                .complaintTitle(complaint.getTitle())
                .officerId(officer.getId())
                .officerName(officer.getFullName())
                .assignedById(assignedBy.getId())
                .assignedByName(assignedBy.getFullName())
                .notes(saved.getNotes())
                .assignedAt(saved.getAssignedAt())
                .completedAt(saved.getCompletedAt())
                .build();
    }

    @Transactional(readOnly = true)
    public List<com.civicpulse.complaint.dto.ComplaintStatusHistoryResponse> getComplaintStatusHistory(
            UUID complaintId, UserPrincipal currentUser) {
        Complaint complaint = complaintRepository.findById(complaintId)
                .orElseThrow(() -> new ResourceNotFoundException("Complaint not found with id: " + complaintId));

        boolean isOfficerOrAdmin = currentUser.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals(RoleName.ROLE_OFFICER.name()) ||
                               a.getAuthority().equals(RoleName.ROLE_ADMIN.name()));

        if (!isOfficerOrAdmin && !complaint.getCitizen().getId().equals(currentUser.getId())) {
            throw new AccessDeniedException("You do not have permission to view status history for this complaint");
        }

        return statusHistoryRepository.findByComplaintIdOrderByCreatedAtAsc(complaintId).stream()
                .map(h -> com.civicpulse.complaint.dto.ComplaintStatusHistoryResponse.builder()
                        .id(h.getId())
                        .complaintId(h.getComplaint().getId())
                        .previousStatus(h.getPreviousStatus())
                        .newStatus(h.getNewStatus())
                        .changedById(h.getChangedBy().getId())
                        .changedByName(h.getChangedBy().getFullName())
                        .remarks(h.getRemarks())
                        .createdAt(h.getCreatedAt())
                        .build())
                .collect(Collectors.toList());
    }

    private void validateStatusTransition(ComplaintStatus currentStatus, ComplaintStatus newStatus) {
        if (currentStatus == newStatus) {
            return;
        }

        boolean isValid = switch (currentStatus) {
            case REPORTED -> newStatus == ComplaintStatus.VERIFIED ||
                             newStatus == ComplaintStatus.ASSIGNED ||
                             newStatus == ComplaintStatus.IN_PROGRESS;
            case VERIFIED -> newStatus == ComplaintStatus.ASSIGNED ||
                             newStatus == ComplaintStatus.IN_PROGRESS;
            case ASSIGNED -> newStatus == ComplaintStatus.IN_PROGRESS;
            case IN_PROGRESS -> newStatus == ComplaintStatus.RESOLVED;
            case RESOLVED -> newStatus == ComplaintStatus.REOPENED;
            case REOPENED -> newStatus == ComplaintStatus.IN_PROGRESS ||
                             newStatus == ComplaintStatus.ASSIGNED;
        };

        if (!isValid) {
            throw new BadRequestException(String.format("Invalid status transition from '%s' to '%s'", currentStatus, newStatus));
        }
    }

    private Department resolveDepartment(Long departmentId, ComplaintCategory category) {
        if (departmentId != null) {
            return departmentRepository.findById(departmentId)
                    .orElseThrow(() -> new ResourceNotFoundException("Department not found: " + departmentId));
        }

        String departmentCode = switch (category) {
            case POTHOLE -> "ROADS_PWD";
            case GARBAGE_OVERFLOW -> "SANITATION_SWM";
            case WATER_LEAKAGE -> "WATER_SEWER";
            case BROKEN_STREETLIGHT -> "ELECTRIC_LIGHT";
            case FALLEN_TREE -> "PARKS_URBAN";
            case OTHER -> "ROADS_PWD";
        };

        return departmentRepository.findByCode(departmentCode).orElse(null);
    }

    private ComplaintResponse mapToResponse(Complaint complaint) {
        boolean isOverdue = complaint.getStatus() != ComplaintStatus.RESOLVED &&
                complaint.getSlaDeadline() != null &&
                complaint.getSlaDeadline().isBefore(Instant.now());

        return ComplaintResponse.builder()
                .id(complaint.getId())
                .citizenId(complaint.getCitizen().getId())
                .citizenName(complaint.getCitizen().getFullName())
                .citizenEmail(complaint.getCitizen().getEmail())
                .departmentId(complaint.getDepartment() != null ? complaint.getDepartment().getId() : null)
                .departmentName(complaint.getDepartment() != null ? complaint.getDepartment().getName() : null)
                .departmentCode(complaint.getDepartment() != null ? complaint.getDepartment().getCode() : null)
                .title(complaint.getTitle())
                .description(complaint.getDescription())
                .category(complaint.getCategory())
                .status(complaint.getStatus())
                .priority(complaint.getPriority())
                .severity(complaint.getSeverity())
                .latitude(complaint.getLatitude())
                .longitude(complaint.getLongitude())
                .addressText(complaint.getAddressText())
                .isPotentialDuplicate(complaint.isPotentialDuplicate())
                .duplicateOfId(complaint.getDuplicateOfId())
                .aiConfidenceScore(complaint.getAiConfidenceScore())
                .aiSuggestedCategory(complaint.getAiSuggestedCategory())
                .slaDeadline(complaint.getSlaDeadline())
                .isOverdue(isOverdue)
                .resolvedAt(complaint.getResolvedAt())
                .createdAt(complaint.getCreatedAt())
                .updatedAt(complaint.getUpdatedAt())
                .build();
    }

    private PagedResponse<ComplaintResponse> mapToPagedResponse(Page<Complaint> page) {
        List<ComplaintResponse> content = page.getContent().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());

        return PagedResponse.<ComplaintResponse>builder()
                .content(content)
                .pageNumber(page.getNumber())
                .pageSize(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .last(page.isLast())
                .build();
    }
}
