package ma.locavite.reservation.service;

import ma.locavite.reservation.event.ReservationConfirmedEvent;
import ma.locavite.reservation.event.ReservationExtendedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
public class ContractEmailListener {

    private static final Logger log = LoggerFactory.getLogger(ContractEmailListener.class);

    private final ContractService contractService;

    public ContractEmailListener(ContractService contractService) {
        this.contractService = contractService;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void sendContractAfterConfirmation(ReservationConfirmedEvent event) {
        try {
            contractService.emailContract(event.reservation(), event.contract());
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
                contractService.emailContract(event.reservation(), event.contract());
            }
        } catch (RuntimeException exception) {
            log.error("Échec du traitement après prolongation [reservationId={}]",
                    event.reservation().getId(), exception);
        }
    }
}
