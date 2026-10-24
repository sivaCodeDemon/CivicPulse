package com.civicpulse.complaint;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ComplaintRepository extends JpaRepository<Complaint, UUID>, JpaSpecificationExecutor<Complaint> {

    Page<Complaint> findByCitizenId(UUID citizenId, Pageable pageable);

    Optional<Complaint> findByIdAndCitizenId(UUID id, UUID citizenId);

    Page<Complaint> findByStatus(ComplaintStatus status, Pageable pageable);

    Page<Complaint> findByDepartmentId(Long departmentId, Pageable pageable);

    long countByStatus(ComplaintStatus status);

    long countByCitizenId(UUID citizenId);

    @Query("SELECT c FROM Complaint c WHERE c.status != com.civicpulse.complaint.ComplaintStatus.RESOLVED AND c.slaDeadline < :now")
    List<Complaint> findOverdueComplaints(@Param("now") Instant now);

    @Query("SELECT c FROM Complaint c WHERE " +
            "c.status != com.civicpulse.complaint.ComplaintStatus.RESOLVED AND " +
            "c.latitude BETWEEN :minLat AND :maxLat AND " +
            "c.longitude BETWEEN :minLng AND :maxLng")
    List<Complaint> findNearbyActiveComplaints(
            @Param("minLat") Double minLat,
            @Param("maxLat") Double maxLat,
            @Param("minLng") Double minLng,
            @Param("maxLng") Double maxLng
    );
}
