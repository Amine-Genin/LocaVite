package ma.locavite.reservation.service;

import ma.locavite.auth.service.EmailService;
import ma.locavite.reservation.event.ReservationCancelledEvent;
import ma.locavite.reservation.event.ReservationConfirmedEvent;
import ma.locavite.reservation.event.ReservationExtendedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
public class ContractEmailListener {

    private static final Logger log = LoggerFactory.getLogger(ContractEmailListener.class);

    private final ContractService contractService;
    private final EmailService emailService;

    public ContractEmailListener(ContractService contractService, EmailService emailService) {
        this.contractService = contractService;
        this.emailService = emailService;
    }

    @Async
    @EventListener
    public void sendContractAfterConfirmation(ReservationConfirmedEvent event) {
        try {
            emailService.sendReservationConfirmation(event.reservation(), event.contract());
        } catch (RuntimeException exception) {
            log.error("Échec du traitement du contrat et de son e-mail après confirmation [reservationId={}]",
                    event.reservation().getId(), exception);
        }
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void onReservationExtended(ReservationExtendedEvent event) {
        try {
            if (event.extension() != null) {
                contractService.notifyAgencyOfExtension(event.reservation(), event.extension());
            }
            if (event.contract() != null) {
                emailService.sendReservationConfirmation(event.reservation(), event.contract());
            }
        } catch (RuntimeException exception) {
            log.error("Échec du traitement après prolongation [reservationId={}]",
                    event.reservation().getId(), exception);
        }
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void onReservationCancelled(ReservationCancelledEvent event) {
        try {
            if (event.refund() != null) {
                contractService.notifyAgencyOfCancellation(event.reservation(), event.refund());
            }
        } catch (RuntimeException exception) {
            log.error("Échec de la notification d'annulation [reservationId={}]",
                    event.reservation().getId(), exception);
        }
    }
}
