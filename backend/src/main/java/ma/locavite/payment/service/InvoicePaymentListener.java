package ma.locavite.payment.service;

import ma.locavite.payment.domain.PaymentStatus;
import ma.locavite.payment.event.PaymentSucceededEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
public class InvoicePaymentListener {

    private static final Logger log = LoggerFactory.getLogger(InvoicePaymentListener.class);

    private final InvoiceService invoiceService;

    public InvoicePaymentListener(InvoiceService invoiceService) {
        this.invoiceService = invoiceService;
    }

    @EventListener
    public void onPaymentSucceeded(PaymentSucceededEvent event) {
        if (event == null || event.payment() == null) {
            return;
        }
        if (event.payment().getStatut() == PaymentStatus.SUCCESS) {
            try {
                invoiceService.generate(event.payment());
            } catch (Exception e) {
                log.error("Erreur lors de la génération automatique de la facture pour le paiement [id={}, code={}] : {}",
                        event.payment().getId(), event.payment().getCode(), e.getMessage(), e);
            }
        }
    }
}
