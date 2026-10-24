package ma.locavite.common.dto;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ApiResponseTest {

    @Test
    @DisplayName("Devrait créer une réponse de succès avec données et message")
    void shouldCreateSuccessResponse() {
        ApiResponse<String> response = ApiResponse.success("donnée test", "Succès");

        assertThat(response.isSuccess()).isTrue();
        assertThat(response.getMessage()).isEqualTo("Succès");
        assertThat(response.getData()).isEqualTo("donnée test");
        assertThat(response.getTimestamp()).isNotNull();
    }

    @Test
    @DisplayName("Devrait créer une réponse d'erreur avec message")
    void shouldCreateErrorResponse() {
        ApiResponse<Void> response = ApiResponse.error("Une erreur est survenue");

        assertThat(response.isSuccess()).isFalse();
        assertThat(response.getMessage()).isEqualTo("Une erreur est survenue");
        assertThat(response.getData()).isNull();
        assertThat(response.getTimestamp()).isNotNull();
    }
}
