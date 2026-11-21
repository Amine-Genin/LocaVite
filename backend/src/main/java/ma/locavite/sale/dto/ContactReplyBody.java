package ma.locavite.sale.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ContactReplyBody(
        @NotBlank @Size(max = 5000) String replyMessage
) {
}
