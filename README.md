# CivicPulse — AI-Powered Smart City Platform

CivicPulse is an enterprise-grade civic issue reporting and municipal resolution platform. It empowers citizens to report public infrastructure and municipal problems with geolocation coordinates and photographic evidence, while equipping municipal departments with automated AI triage, SLA tracking, duplicate detection, and a unified resolution workflow.

---

## 1. System Architecture & Folder Structure

```
CivicPulse/
├── backend/
│   ├── pom.xml
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/com/civicpulse/
│   │   │   │   ├── CivicPulseApplication.java
│   │   │   │   ├── auth/              # Authentication DTOs, Service & Controller
│   │   │   │   ├── common/            # ErrorResponse, ApiResponse, GlobalExceptionHandler
│   │   │   │   ├── config/            # OpenAPI 3 docs & DataInitializer seeder
│   │   │   │   ├── complaint/         # Complaint Entity, Enums, DTOs, Repo, Service & Controller
│   │   │   │   ├── department/        # Department Entity & Repository
│   │   │   │   ├── security/          # Spring Security, JWT utilities, RBAC filter
│   │   │   │   └── user/              # User, Role, UserPrincipal, Repositories
│   │   │   └── resources/
│   │   │       ├── application.yml
│   │   │       └── db/migration/
│   │   │           ├── V1__init_schema.sql
│   │   │           └── V2__seed_initial_data.sql
│   │   └── test/
│   │       ├── java/com/civicpulse/
│   │       │   ├── AuthControllerIntegrationTest.java
│   │       │   ├── CitizenDataIsolationIntegrationTest.java
│   │       │   └── ComplaintControllerIntegrationTest.java
│   │       └── resources/
│   │           └── application-test.yml
├── frontend/                          # (Milestone 5 - React & Tailwind UI)
└── docker-compose.yml                 # (Milestone 6 - Docker containerization)
```

---

## 2. Core Security & Privacy Model

CivicPulse enforces three distinct Role-Based Access Control (RBAC) levels:

| Role | Permissions & Boundaries |
| :--- | :--- |
| **`ROLE_CITIZEN`** | Can register, authenticate, submit complaints with GPS coordinates, track their own complaints, and view only their private reports. **Strictly forbidden** from viewing other citizens' private reports or modifying official resolution states. |
| **`ROLE_OFFICER`** | Can view municipal complaints, assign departments, transition statuses (`REPORTED` -> `VERIFIED` -> `ASSIGNED` -> `IN_PROGRESS` -> `RESOLVED`), and log resolution remarks. |
| **`ROLE_ADMIN`** | Full system visibility, department SLA configuration, and administrative audits. |

---

## 3. Database Schema Overview

PostgreSQL 16 + PostGIS schema configured with Flyway database migrations:

- **`roles`**: Standardized RBAC roles (`ROLE_CITIZEN`, `ROLE_OFFICER`, `ROLE_ADMIN`).
- **`users`** & **`user_roles`**: Account credentials, contact details, active flag, and timestamps.
- **`departments`**: Municipal bodies (`ROADS_PWD`, `SANITATION_SWM`, `WATER_SEWER`, `ELECTRIC_LIGHT`, `PARKS_URBAN`) with SLA hourly targets.
- **`complaints`**: Core issue report including citizen FK, department FK, title, description, category, status, priority, severity, coordinates (`latitude`, `longitude`), SLA deadline, and duplicate flags.
- **`complaint_images`**: Secure file metadata tracking before/after repair photos.
- **`assignments`**: Officer assignment history.
- **`complaint_status_history`**: Audit log of every status transition with officer remarks.
- **`notifications`**: Citizen & officer notifications.
- **`audit_logs`**: Security event logs.

---

## 4. Default Seed Accounts (Local Dev & Test)

Pre-seeded synthetic demonstration accounts:

| Role | Email | Password |
| :--- | :--- | :--- |
| **Citizen** | `citizen@civicpulse.city` | `Password123!` |
| **Officer** | `officer@civicpulse.city` | `Password123!` |
| **Admin** | `admin@civicpulse.city` | `Password123!` |

---

## 5. Verification Commands

### Run Automated Integration Tests
From the `backend` folder:
```powershell
mvn clean test
```
All 11 automated integration tests verify:
- Registration, validation errors, and duplicate email prevention.
- JWT login and claim validation.
- Complaint creation with geolocation validation (-90 to 90 lat, -180 to 180 lng).
- **Citizen Data Isolation**: Verifies that Citizen B cannot access Citizen A's complaints (403 Forbidden).
- Officer status transitions (`IN_PROGRESS` -> `RESOLVED` with timestamps).
- Citizen privilege boundary enforcement.

### Run Spring Boot Application Locally
```powershell
mvn spring-boot:run
```
- API Base URL: `http://localhost:8080/api/v1`
- Swagger UI (OpenAPI 3): `http://localhost:8080/api/v1/swagger-ui.html`
- OpenAPI Specification: `http://localhost:8080/api/v1/v3/api-docs`
