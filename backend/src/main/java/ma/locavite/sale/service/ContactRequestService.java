package ma.locavite.sale.service;

import ma.locavite.auth.domain.User;
import ma.locavite.auth.repository.UserRepository;
import ma.locavite.notification.service.SmsService;
import ma.locavite.sale.domain.ContactRequest;
import ma.locavite.sale.domain.ContactStatus;
import ma.locavite.sale.domain.SaleListing;
import ma.locavite.sale.domain.SaleStatus;
import ma.locavite.sale.dto.ContactReplyBody;
import ma.locavite.sale.dto.ContactRequestBody;
import ma.locavite.sale.dto.ContactRequestResponse;
import ma.locavite.sale.exception.SaleListingNotFoundException;
import ma.locavite.sale.event.ContactReplySentEvent;
import ma.locavite.sale.event.ContactRequestCreatedEvent;
import ma.locavite.sale.repository.ContactRequestRepository;
import ma.locavite.sale.repository.SaleListingRepository;
import ma.locavite.vehicle.service.CurrentUserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Service
@Transactional(readOnly = true)
public class ContactRequestService {

    private static final Logger log = LoggerFactory.getLogger(ContactRequestService.class);

    private final ContactRequestRepository contactRepository;
    private final SaleListingRepository listingRepository;
    private final UserRepository userRepository;
    private final CurrentUserService currentUser;
    private final JavaMailSender mailSender;
    private final SmsService smsService;
    private final ApplicationEventPublisher eventPublisher;

    public ContactRequestService(
            ContactRequestRepository contactRepository,
            SaleListingRepository listingRepository,
            UserRepository userRepository,
            CurrentUserService currentUser,
            JavaMailSender mailSender,
            SmsService smsService) {
        this(contactRepository, listingRepository, userRepository, currentUser, mailSender, smsService, null);
    }

    public ContactRequestService(
            ContactRequestRepository contactRepository,
            SaleListingRepository listingRepository,
            UserRepository userRepository,
            CurrentUserService currentUser,
            JavaMailSender mailSender,
            SmsService smsService,
            ApplicationEventPublisher eventPublisher) {
        this.contactRepository = contactRepository;
        this.listingRepository = listingRepository;
        this.userRepository = userRepository;
        this.currentUser = currentUser;
        this.mailSender = mailSender;
        this.smsService = smsService;
        this.eventPublisher = eventPublisher;
    }

    @Transactional
    public ContactRequestResponse create(Long listingId, ContactRequestBody body) {
        SaleListing listing = listingRepository.findById(listingId)
                .filter(item -> !Boolean.TRUE.equals(item.getDeleted()) && item.getStatut() == SaleStatus.PUBLISHED)
                .orElseThrow(() -> new SaleListingNotFoundException(listingId));

        ContactRequest request = new ContactRequest();
        request.setSaleListing(listing);
        request.setNom(body.nom().trim());
        request.setEmail(body.email().trim());
        request.setTelephone(body.telephone().trim());
        request.setMessage(body.message().trim());
        ContactRequest saved = contactRepository.save(request);

        User agency = userRepository.findById(listing.getAgenceId())
                .orElseThrow(() -> new IllegalStateException("Compte de l'agence introuvable"));
        notifyAgency(saved, listing, agency);
        if (eventPublisher != null) {
            eventPublisher.publishEvent(new ContactRequestCreatedEvent(saved, listing));
        }
        return toResponse(saved);
    }

    public List<ContactRequestResponse> listForAgency() {
        if (currentUser.isAdmin()) {
            return contactRepository.findAllByOrderByCreatedAtDesc().stream()
                    .map(this::toResponse)
                    .toList();
        }
        Long agencyId = currentUser.getCurrentUserId()
                .orElseThrow(() -> new AccessDeniedException("Utilisateur non authentifié"));
        return contactRepository.findBySaleListingAgenceIdOrderByCreatedAtDesc(agencyId).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public ContactRequestResponse reply(Long requestId, ContactReplyBody body) {
        ContactRequest request;
        if (currentUser.isAdmin()) {
            request = contactRepository.findById(requestId)
                    .orElseThrow(() -> new SaleListingNotFoundException("Demande de contact introuvable : " + requestId));
        } else {
            Long agencyId = currentUser.getCurrentUserId()
                    .orElseThrow(() -> new AccessDeniedException("Utilisateur non authentifié"));
            request = contactRepository.findByIdAndSaleListingAgenceId(requestId, agencyId)
                    .orElseThrow(() -> new SaleListingNotFoundException("Demande de contact introuvable : " + requestId));
        }
        request.setReplyMessage(body.replyMessage().trim());
        request.setRepliedAt(Instant.now());
        request.setStatut(ContactStatus.REPLIED);
        ContactRequest saved = contactRepository.save(request);
        if (eventPublisher != null) {
            eventPublisher.publishEvent(new ContactReplySentEvent(saved, saved.getSaleListing(), body.replyMessage().trim()));
        }
        return toResponse(saved);
    }

    private void notifyAgency(ContactRequest request, SaleListing listing, User agency) {
        String subject = "Nouvelle demande de contact — " + listing.getTitre();
        String content = "Demande pour l'annonce " + listing.getTitre() + " (" + listing.getCode() + ")\n\n"
                + "Nom : " + request.getNom() + "\n"
                + "E-mail : " + request.getEmail() + "\n"
                + "Téléphone : " + request.getTelephone() + "\n\n"
                + request.getMessage();
        try {
            SimpleMailMessage email = new SimpleMailMessage();
            email.setFrom("noreply@locavite.ma");
            email.setTo(agency.getEmail());
            email.setSubject(subject);
            email.setText(content);
            mailSender.send(email);
        } catch (RuntimeException e) {
            log.error("Échec de la notification e-mail pour la demande de contact {} : {}",
                    request.getId(), e.getMessage());
        }

        try {
            boolean sent = smsService.sendSms(agency.getTelephone(), "Nouvelle demande LocaVite pour "
                    + listing.getTitre() + " de " + request.getNom() + ". Consultez votre espace agence.");
            if (!sent) {
                log.error("La notification SMS de la demande de contact {} n'a pas été délivrée",
                        request.getId());
            }
        } catch (RuntimeException e) {
            log.error("Échec de la notification SMS pour la demande de contact {} : {}",
                    request.getId(), e.getMessage());
        }
    }

    private ContactRequestResponse toResponse(ContactRequest request) {
        return new ContactRequestResponse(
                request.getId(),
                request.getSaleListing().getId(),
                request.getNom(),
                request.getEmail(),
                request.getTelephone(),
                request.getMessage(),
                request.getStatut(),
                request.getCreatedAt(),
                request.getRepliedAt(),
                request.getReplyMessage());
    }
}
