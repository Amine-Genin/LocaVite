package ma.locavite.document.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import ma.locavite.document.domain.DocumentStatus;

@Schema(description = "Requête de vérification/validation d'un document utilisateur par un administrateur")
public class VerifyDocumentRequest {

    @NotNull(message = "Le statut est obligatoire")
    @Schema(description = "Nouveau statut à appliquer au document", example = "VALIDATED", requiredMode = Schema.RequiredMode.REQUIRED)
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
