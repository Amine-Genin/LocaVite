package ma.locavite.notification.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Notification {

    private Long id;
    private Long userId;
    private String titre;
    private String message;

    @Builder.Default
    private NotificationType type = NotificationType.IN_APP;

    @Builder.Default
    private Boolean lue = false;

    @Builder.Default
    private LocalDateTime dateEnvoi = LocalDateTime.now();
}
