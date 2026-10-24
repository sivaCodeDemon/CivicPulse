package com.civicpulse.auth;

import com.civicpulse.auth.dto.JwtResponse;
import com.civicpulse.auth.dto.LoginRequest;
import com.civicpulse.auth.dto.RegisterRequest;
import com.civicpulse.auth.dto.UserProfileResponse;
import com.civicpulse.common.exception.BadRequestException;
import com.civicpulse.common.exception.ResourceNotFoundException;
import com.civicpulse.security.JwtUtils;
import com.civicpulse.security.UserPrincipal;
import com.civicpulse.user.*;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtils jwtUtils;

    @Transactional
    public JwtResponse login(LoginRequest loginRequest) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(loginRequest.getEmail(), loginRequest.getPassword()));

        SecurityContextHolder.getContext().setAuthentication(authentication);
        String jwt = jwtUtils.generateJwtToken(authentication);

        UserPrincipal userDetails = (UserPrincipal) authentication.getPrincipal();
        List<String> roles = userDetails.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toList());

        return JwtResponse.builder()
                .token(jwt)
                .type("Bearer")
                .id(userDetails.getId())
                .email(userDetails.getUsername())
                .firstName(userDetails.getFirstName())
                .lastName(userDetails.getLastName())
                .roles(roles)
                .build();
    }

    @Transactional
    public UserProfileResponse register(RegisterRequest registerRequest) {
        if (userRepository.existsByEmail(registerRequest.getEmail())) {
            throw new BadRequestException("Email is already in use: " + registerRequest.getEmail());
        }

        Set<Role> roles = new HashSet<>();
        if (registerRequest.getRole() != null && !registerRequest.getRole().isBlank()) {
            try {
                RoleName requestedRole = RoleName.valueOf(registerRequest.getRole().toUpperCase());
                Role role = roleRepository.findByName(requestedRole)
                        .orElseThrow(() -> new ResourceNotFoundException("Role not found: " + requestedRole));
                roles.add(role);
            } catch (IllegalArgumentException e) {
                throw new BadRequestException("Invalid role specified: " + registerRequest.getRole());
            }
        } else {
            Role citizenRole = roleRepository.findByName(RoleName.ROLE_CITIZEN)
                    .orElseThrow(() -> new ResourceNotFoundException("Default role ROLE_CITIZEN not found"));
            roles.add(citizenRole);
        }

        User user = User.builder()
                .email(registerRequest.getEmail())
                .password(passwordEncoder.encode(registerRequest.getPassword()))
                .firstName(registerRequest.getFirstName())
                .lastName(registerRequest.getLastName())
                .phoneNumber(registerRequest.getPhoneNumber())
                .active(true)
                .roles(roles)
                .build();

        User savedUser = userRepository.save(user);

        return mapToProfileResponse(savedUser);
    }

    @Transactional(readOnly = true)
    public UserProfileResponse getCurrentUserProfile(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));

        return mapToProfileResponse(user);
    }

    private UserProfileResponse mapToProfileResponse(User user) {
        List<String> roles = user.getRoles().stream()
                .map(role -> role.getName().name())
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
