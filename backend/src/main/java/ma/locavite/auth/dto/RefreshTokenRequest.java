package ma.locavite.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "Requête de rafraîchissement ou de déconnexion d'un token")
public class RefreshTokenRequest {

    @NotBlank(message = "Le refresh token est obligatoire")
    @Schema(description = "Jeton de rafraîchissement reçu lors du login", example = "d9c28741-f672-4d2c-8067-27b92f440ee5", requiredMode = Schema.RequiredMode.REQUIRED)
    private String refreshToken;

    public RefreshTokenRequest() {
    }

    public RefreshTokenRequest(String refreshToken) {
        this.refreshToken = refreshToken;
    }

    public String getRefreshToken() {
        return refreshToken;
    }

    public void setRefreshToken(String refreshToken) {
        this.refreshToken = refreshToken;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String refreshToken;

        public Builder refreshToken(String refreshToken) {
            this.refreshToken = refreshToken;
            return this;
        }

        public RefreshTokenRequest build() {
            return new RefreshTokenRequest(refreshToken);
        }
    }
}
