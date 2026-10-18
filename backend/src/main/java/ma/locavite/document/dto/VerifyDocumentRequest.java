package ma.locavite.document.dto;

import jakarta.validation.constraints.NotNull;
import ma.locavite.document.domain.DocumentStatus;

public class VerifyDocumentRequest {

    @NotNull(message = "Le statut est obligatoire")
    private DocumentStatus status;

    public VerifyDocumentRequest() {
    }

    public VerifyDocumentRequest(DocumentStatus status) {
        this.status = status;
    }

    public DocumentStatus getStatus() {
        return status;
    }

    public void setStatus(DocumentStatus status) {
        this.status = status;
    }
}
