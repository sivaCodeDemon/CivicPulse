package com.civicpulse.config;

import com.civicpulse.department.Department;
import com.civicpulse.department.DepartmentRepository;
import com.civicpulse.user.*;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;

@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

    private final RoleRepository roleRepository;
    private final UserRepository userRepository;
    private final DepartmentRepository departmentRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public void run(String... args) {
        log.info("Initializing CivicPulse core roles, departments, and synthetic seed data...");

        // 1. Initialize Roles
        Role citizenRole = initRole(RoleName.ROLE_CITIZEN, "Citizen user with reporting permissions");
        Role officerRole = initRole(RoleName.ROLE_OFFICER, "Municipal officer with resolution permissions");
        Role adminRole = initRole(RoleName.ROLE_ADMIN, "Platform administrator");

        // 2. Initialize Standard Departments
        initDepartment("Road Infrastructure & Public Works", "ROADS_PWD", "Potholes and road repairs", 48);
        initDepartment("Solid Waste Management & Sanitation", "SANITATION_SWM", "Garbage overflow and waste clearance", 24);
        initDepartment("Water Supply & Sewerage Board", "WATER_SEWER", "Water leaks and drainage", 36);
        initDepartment("Electrical & Street Lighting", "ELECTRIC_LIGHT", "Streetlights and electrical fixtures", 24);
        initDepartment("Parks & Urban Horticulture", "PARKS_URBAN", "Fallen branches and park maintenance", 72);

        // 3. Initialize Synthetic Demo Accounts
        initUser("citizen@civicpulse.city", "Citizen", "User", "+1-555-0101", Set.of(citizenRole));
        initUser("officer@civicpulse.city", "Officer", "Smith", "+1-555-0102", Set.of(officerRole));
        initUser("admin@civicpulse.city", "System", "Admin", "+1-555-0103", Set.of(adminRole));

        log.info("CivicPulse system initialization completed successfully.");
    }

    private Role initRole(RoleName name, String description) {
        return roleRepository.findByName(name)
                .orElseGet(() -> roleRepository.save(new Role(name, description)));
    }

    private void initDepartment(String name, String code, String description, int defaultSlaHours) {
        if (departmentRepository.findByCode(code).isEmpty()) {
            departmentRepository.save(Department.builder()
                    .name(name)
                    .code(code)
                    .description(description)
                    .defaultSlaHours(defaultSlaHours)
                    .build());
        }
    }

    private void initUser(String email, String firstName, String lastName, String phone, Set<Role> roles) {
        if (!userRepository.existsByEmail(email)) {
            User user = User.builder()
                    .email(email)
                    .password(passwordEncoder.encode("Password123!"))
                    .firstName(firstName)
                    .lastName(lastName)
                    .phoneNumber(phone)
                    .active(true)
                    .roles(roles)
                    .build();
            userRepository.save(user);
            log.info("Created synthetic demo user: {}", email);
        }
    }
}
