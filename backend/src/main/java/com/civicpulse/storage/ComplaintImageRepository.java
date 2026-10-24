package com.civicpulse.storage;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ComplaintImageRepository extends JpaRepository<ComplaintImage, UUID> {
    List<ComplaintImage> findByComplaintId(UUID complaintId);
    Optional<ComplaintImage> findByIdAndComplaintId(UUID id, UUID complaintId);
}
