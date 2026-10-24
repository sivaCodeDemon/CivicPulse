package com.civicpulse.user;

import com.civicpulse.auth.dto.UserProfileResponse;
import com.civicpulse.common.response.ApiResponse;
import com.civicpulse.common.response.PagedResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/admin/users")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Admin User Management", description = "Admin-only endpoints for viewing and managing platform users")
public class UserAdminController {

    private final UserRepository userRepository;

    @GetMapping
    @Operation(summary = "List all users, optionally filtered by role (e.g. ROLE_OFFICER, ROLE_CITIZEN)")
    public ResponseEntity<ApiResponse<PagedResponse<UserProfileResponse>>> listUsers(
            @RequestParam(required = false) String role,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String direction) {

        Sort sort = direction.equalsIgnoreCase("asc") ? Sort.by(sortBy).ascending() : Sort.by(sortBy).descending();
        Pageable pageable = PageRequest.of(page, size, sort);

        Page<User> userPage;
        if (role != null && !role.isBlank()) {
            try {
                RoleName roleName = RoleName.valueOf(role.toUpperCase());
                userPage = userRepository.findByRoleName(roleName, pageable);
            } catch (IllegalArgumentException e) {
                userPage = userRepository.findAll(pageable);
            }
        } else {
            userPage = userRepository.findAll(pageable);
        }

        List<UserProfileResponse> content = userPage.getContent().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());

        PagedResponse<UserProfileResponse> response = new PagedResponse<>(
                content,
                userPage.getNumber(),
                userPage.getSize(),
                userPage.getTotalElements(),
                userPage.getTotalPages(),
                userPage.isLast()
        );

        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a specific user's profile by ID")
    public ResponseEntity<ApiResponse<UserProfileResponse>> getUserById(@PathVariable UUID id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new com.civicpulse.common.exception.ResourceNotFoundException("User not found: " + id));
        return ResponseEntity.ok(ApiResponse.success(mapToResponse(user)));
    }

    @PatchMapping("/{id}/deactivate")
    @Operation(summary = "Deactivate a user account (soft-delete)")
    public ResponseEntity<ApiResponse<UserProfileResponse>> deactivateUser(@PathVariable UUID id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new com.civicpulse.common.exception.ResourceNotFoundException("User not found: " + id));
        user.setActive(false);
        userRepository.save(user);
        return ResponseEntity.ok(ApiResponse.success(mapToResponse(user), "User account deactivated"));
    }

    @PatchMapping("/{id}/activate")
    @Operation(summary = "Reactivate a previously deactivated user account")
    public ResponseEntity<ApiResponse<UserProfileResponse>> activateUser(@PathVariable UUID id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new com.civicpulse.common.exception.ResourceNotFoundException("User not found: " + id));
        user.setActive(true);
        userRepository.save(user);
        return ResponseEntity.ok(ApiResponse.success(mapToResponse(user), "User account activated"));
    }

    private UserProfileResponse mapToResponse(User user) {
        List<String> roles = user.getRoles().stream()
                .map(r -> r.getName().name())
                .collect(Collectors.toList());

        return UserProfileResponse.builder()
                .id(user.getId())
                .email(user.getEmail())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .phoneNumber(user.getPhoneNumber())
                .active(user.isActive())
                .roles(roles)
                .createdAt(user.getCreatedAt())
                .build();
    }
}
