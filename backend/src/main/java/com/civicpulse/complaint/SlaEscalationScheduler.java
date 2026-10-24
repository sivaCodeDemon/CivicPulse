package com.civicpulse.complaint;

import com.civicpulse.notification.NotificationService;
import com.civicpulse.notification.NotificationType;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Component
@RequiredArgsConstructor
public class SlaEscalationScheduler {

    private static final Logger log = LoggerFactory.getLogger(SlaEscalationScheduler.class);

    private final ComplaintRepository complaintRepository;
    private final NotificationService notificationService;

    @Scheduled(fixedDelayString = "${app.sla.check-interval-ms:60000}")
    @Transactional
    public void scanAndEscalateOverdueComplaints() {
        Instant now = Instant.now();
        List<Complaint> overdueComplaints = complaintRepository.findOverdueComplaints(now);

        if (!overdueComplaints.isEmpty()) {
            log.warn("SLA Escalation Scanner: Found {} overdue civic complaints", overdueComplaints.size());

            for (Complaint complaint : overdueComplaints) {
                // Escalate priority to CRITICAL if not already set
                if (complaint.getPriority() != ComplaintPriority.CRITICAL) {
                    complaint.setPriority(ComplaintPriority.CRITICAL);
                    complaintRepository.save(complaint);
                    log.info("Complaint {} priority escalated to CRITICAL due to SLA breach", complaint.getId());
                }

                // Notify reporting citizen of official SLA escalation
                notificationService.sendNotification(
                        complaint.getCitizen(),
                        complaint,
                        "SLA Escalation: Your complaint has been elevated",
                        String.format("Complaint '%s' has exceeded standard resolution time and has been prioritized for urgent action.",
                                complaint.getTitle()),
                        NotificationType.SLA_BREACH_WARNING
                );
            }
        }
    }
}
