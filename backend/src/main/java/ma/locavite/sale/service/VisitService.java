package ma.locavite.sale.service;

import ma.locavite.auth.domain.User;
import ma.locavite.auth.repository.UserRepository;
import ma.locavite.notification.service.SmsService;
import ma.locavite.sale.domain.SaleListing;
import ma.locavite.sale.domain.SaleStatus;
import ma.locavite.sale.domain.VisitRequest;
import ma.locavite.sale.domain.VisitStatus;
import ma.locavite.sale.dto.VisitConfirmBody;
import ma.locavite.sale.dto.VisitRequestBody;
import ma.locavite.sale.dto.VisitRequestResponse;
import ma.locavite.sale.dto.VisitRescheduleBody;
import ma.locavite.sale.exception.SaleListingNotFoundException;
import ma.locavite.sale.repository.SaleListingRepository;
import ma.locavite.sale.repository.VisitRequestRepository;
import ma.locavite.sale.event.VisitConfirmedEvent;
import ma.locavite.sale.event.VisitReminderJ1Event;
import ma.locavite.sale.event.VisitRequestCreatedEvent;
import org.springframework.context.ApplicationEventPublisher;
import ma.locavite.vehicle.service.CurrentUserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Service
@Transactional(readOnly = true)
public class VisitService {

    private static final Logger log = LoggerFactory.getLogger(VisitService.class);

    private final VisitRequestRepository visitRepository;
    private final SaleListingRepository listingRepository;
    private final UserRepository userRepository;
    private final CurrentUserService currentUser;
    private final JavaMailSender mailSender;
    private final SmsService smsService;
    private final ApplicationEventPublisher eventPublisher;

    public VisitService(
            VisitRequestRepository visitRepository,
            SaleListingRepository listingRepository,
            UserRepository userRepository,
            CurrentUserService currentUser,
            JavaMailSender mailSender,
            SmsService smsService) {
        this(visitRepository, listingRepository, userRepository, currentUser, mailSender, smsService, null);
    }

    public VisitService(
            VisitRequestRepository visitRepository,
            SaleListingRepository listingRepository,
            UserRepository userRepository,
            CurrentUserService currentUser,
            JavaMailSender mailSender,
            SmsService smsService,
            ApplicationEventPublisher eventPublisher) {
        this.visitRepository = visitRepository;
        this.listingRepository = listingRepository;
        this.userRepository = userRepository;
        this.currentUser = currentUser;
        this.mailSender = mailSender;
        this.smsService = smsService;
        this.eventPublisher = eventPublisher;
    }

    @Transactional
    public VisitRequestResponse create(Long listingId, VisitRequestBody body) {
        Long clientId = currentUser.getCurrentUserId()
                .orElseThrow(() -> new AccessDeniedException("Utilisateur non authentifié"));
        SaleListing listing = listingRepository.findById(listingId)
                .filter(item -> !Boolean.TRUE.equals(item.getDeleted()) && item.getStatut() == SaleStatus.PUBLISHED)
                .orElseThrow(() -> new SaleListingNotFoundException(listingId));

        VisitRequest visit = new VisitRequest();
        visit.setSaleListing(listing);
        visit.setClientId(clientId);
        visit.setDateSouhaitee(body.dateSouhaitee());
        visit.setCreneauHoraire(body.creneauHoraire().trim());
        visit.setLieu(body.lieu().trim());
        visit.setTypeVisite(body.typeVisite());
        visit.setStatut(VisitStatus.PENDING);

        VisitRequest saved = visitRepository.save(visit);
        notifyBoth(saved, "Nouvelle demande de visite",
                "Une demande de " + body.typeVisite() + " a été enregistrée pour le "
                        + body.dateSouhaitee() + " à " + body.creneauHoraire() + ".");
        if (eventPublisher != null) {
            eventPublisher.publishEvent(new VisitRequestCreatedEvent(saved, listing));
        }
        return toResponse(saved);
    }

    public List<VisitRequestResponse> listForAgency() {
        if (currentUser.isAdmin()) {
            return visitRepository.findAllByOrderByCreatedAtDesc().stream()
                    .map(this::toResponse)
                    .toList();
        }
        Long agencyId = currentUser.getCurrentUserId()
                .orElseThrow(() -> new AccessDeniedException("Utilisateur non authentifié"));
        return visitRepository.findBySaleListingAgenceIdOrderByCreatedAtDesc(agencyId).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public VisitRequestResponse confirm(Long visitId, VisitConfirmBody body) {
        VisitRequest visit = findManagedVisit(visitId);
        visit.setStatut(VisitStatus.CONFIRMED);
        if (body != null && body.notes() != null && !body.notes().isBlank()) {
            visit.setNotes(body.notes().trim());
        }
        VisitRequest saved = visitRepository.save(visit);
        notifyBoth(saved, "Visite confirmée",
                "Votre demande de visite pour le " + saved.getDateSouhaitee()
                        + " à " + saved.getCreneauHoraire() + " est confirmée.");
        if (eventPublisher != null) {
            eventPublisher.publishEvent(new VisitConfirmedEvent(saved, saved.getSaleListing()));
        }
        return toResponse(saved);
    }

    @Transactional
    public VisitRequestResponse reschedule(Long visitId, VisitRescheduleBody body) {
        VisitRequest visit = findManagedVisit(visitId);
        visit.setDateSouhaitee(body.dateSouhaitee());
        visit.setCreneauHoraire(body.creneauHoraire().trim());
        if (body.lieu() != null && !body.lieu().isBlank()) {
            visit.setLieu(body.lieu().trim());
        }
        if (body.notes() != null && !body.notes().isBlank()) {
            visit.setNotes(body.notes().trim());
        }
        visit.setStatut(VisitStatus.RESCHEDULED);
        VisitRequest saved = visitRepository.save(visit);
        notifyBoth(saved, "Visite reportée",
                "La visite a été reportée au " + saved.getDateSouhaitee()
                        + " à " + saved.getCreneauHoraire() + ".");
        return toResponse(saved);
    }

    @Transactional
    public VisitRequestResponse cancelByClient(Long visitId) {
        Long clientId = currentUser.getCurrentUserId()
                .orElseThrow(() -> new AccessDeniedException("Utilisateur non authentifié"));
        VisitRequest visit = visitRepository.findByIdAndClientId(visitId, clientId)
                .orElseThrow(() -> new SaleListingNotFoundException("Demande de visite introuvable : " + visitId));
        if (visit.getStatut() == VisitStatus.COMPLETED || visit.getStatut() == VisitStatus.CANCELLED) {
            throw new IllegalStateException("Cette demande de visite ne peut plus être annulée");
        }
        visit.setStatut(VisitStatus.CANCELLED);
        VisitRequest saved = visitRepository.save(visit);
        notifyBoth(saved, "Visite annulée", "La demande de visite du "
                + saved.getDateSouhaitee() + " a été annulée par le client.");
        return toResponse(saved);
    }

    @Transactional
    public int sendReminders(java.time.LocalDate targetDate) {
        List<VisitRequest> visits = visitRepository.findByStatutAndDateSouhaiteeAndReminderSentAtIsNull(
                VisitStatus.CONFIRMED,
                targetDate);
        int sent = 0;
        for (VisitRequest visit : visits) {
            try {
                notifyBoth(visit, "Rappel de visite",
                        "Rappel : votre visite est prévue demain " + visit.getDateSouhaitee()
                                + " à " + visit.getCreneauHoraire() + ".");
                if (eventPublisher != null) {
                    eventPublisher.publishEvent(new VisitReminderJ1Event(visit, visit.getSaleListing()));
                }
                visit.setReminderSentAt(Instant.now());
                visitRepository.save(visit);
                sent++;
            } catch (RuntimeException e) {
                log.error("Échec du rappel de visite {} : {}", visit.getId(), e.getMessage());
            }
        }
        return sent;
    }

    private VisitRequest findManagedVisit(Long visitId) {
        if (currentUser.isAdmin()) {
            return visitRepository.findById(visitId)
                    .orElseThrow(() -> new SaleListingNotFoundException("Demande de visite introuvable : " + visitId));
        }
        Long agencyId = currentUser.getCurrentUserId()
                .orElseThrow(() -> new AccessDeniedException("Utilisateur non authentifié"));
        return visitRepository.findByIdAndSaleListingAgenceId(visitId, agencyId)
                .orElseThrow(() -> new SaleListingNotFoundException("Demande de visite introuvable : " + visitId));
    }

    private void notifyBoth(VisitRequest visit, String subject, String content) {
        User client = userRepository.findById(visit.getClientId()).orElse(null);
        User agency = userRepository.findById(visit.getSaleListing().getAgenceId()).orElse(null);

        if (client != null) {
            sendEmail(client.getEmail(), subject, content);
            sendSms(client.getTelephone(), content);
        }
        if (agency != null) {
            sendEmail(agency.getEmail(), subject, content);
            sendSms(agency.getTelephone(), content);
        }
    }

    private void sendEmail(String recipient, String subject, String content) {
        if (recipient == null || recipient.isBlank()) {
            return;
        }
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom("noreply@locavite.ma");
            message.setTo(recipient);
            message.setSubject(subject);
            message.setText(content);
            mailSender.send(message);
        } catch (RuntimeException e) {
            log.error("Échec notification e-mail visite à {} : {}", recipient, e.getMessage());
        }
    }

    private void sendSms(String phone, String content) {
        if (phone == null || phone.isBlank()) {
            return;
        }
        try {
            smsService.sendSms(phone, content);
        } catch (RuntimeException e) {
            log.error("Échec notification SMS visite à {} : {}", phone, e.getMessage());
        }
    }

    private VisitRequestResponse toResponse(VisitRequest visit) {
        return new VisitRequestResponse(
                visit.getId(),
                visit.getSaleListing().getId(),
                visit.getClientId(),
                visit.getDateSouhaitee(),
                visit.getCreneauHoraire(),
                visit.getLieu(),
                visit.getTypeVisite(),
                visit.getStatut(),
                visit.getNotes(),
                visit.getCreatedAt());
    }
}
