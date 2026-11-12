package ma.locavite.notification.service;

import ma.locavite.auth.domain.User;
import ma.locavite.notification.domain.NotificationType;
import ma.locavite.reservation.domain.ContractLanguage;
import ma.locavite.reservation.domain.Reservation;
import ma.locavite.vehicle.domain.Vehicle;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

@Service
public class NotificationTemplateService {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    public String buildMessage(NotificationType type, ContractLanguage language,
                               Reservation reservation, Vehicle vehicle, User client) {
        ContractLanguage lang = (language != null) ? language : ContractLanguage.FR;
        String code = reservation != null && reservation.getCode() != null ? reservation.getCode() : "";
        String vehicule = resolveVehicleLabel(vehicle);
        String dateDebut = formatDate(reservation != null ? reservation.getDateDebut() : null);
        String dateFin = formatDate(reservation != null ? reservation.getDateFin() : null);
        String lieu = resolveLocation(reservation, vehicle);

        return render(type, lang, code, vehicule, dateDebut, dateFin, lieu);
    }

    public String render(NotificationType type, ContractLanguage language,
                         String code, String vehicule, String dateDebut, String dateFin, String lieu) {
        ContractLanguage lang = (language != null) ? language : ContractLanguage.FR;
        NotificationType notifType = (type != null) ? type : NotificationType.CONFIRMATION;

        return switch (notifType) {
            case CONFIRMATION -> lang == ContractLanguage.AR
                    ? String.format("لوكا فيت: تم تأكيد حجزكم رقم %s للسيارة %s من %s إلى %s. المكان: %s. شكراً لثقتكم!",
                    code, vehicule, dateDebut, dateFin, lieu)
                    : String.format("LocaVite : Votre réservation %s pour le véhicule %s est confirmée du %s au %s. Lieu : %s. Merci de votre confiance !",
                    code, vehicule, dateDebut, dateFin, lieu);

            case REMINDER_J1 -> lang == ContractLanguage.AR
                    ? String.format("لوكا فيت: تذكير - يبدأ حجزكم رقم %s (%s) غداً %s في %s. يرجى إحضار رخصة القيادة وبطاقة الهوية.",
                    code, vehicule, dateDebut, lieu)
                    : String.format("LocaVite : Rappel - Votre réservation %s (%s) débute demain le %s à %s. Veuillez vous munir de votre permis et pièce d'identité.",
                    code, vehicule, dateDebut, lieu);

            case COMPLETION -> lang == ContractLanguage.AR
                    ? String.format("لوكا فيت: انتهت فترة إيجاركم رقم %s (%s). نتمنى أن تكون تجربتكم ممتعة. نراكم قريباً!",
                    code, vehicule)
                    : String.format("LocaVite : Votre location %s (%s) est terminée. Nous espérons que votre expérience a été agréable. À bientôt !",
                    code, vehicule);

            case CANCELLATION -> lang == ContractLanguage.AR
                    ? String.format("لوكا فيت: تم إلغاء حجزكم رقم %s للسيارة %s. لا تترددوا في الاتصال بنا لأي استفسار.",
                    code, vehicule)
                    : String.format("LocaVite : Votre réservation %s pour le véhicule %s a été annulée. Contactez-nous pour toute question.",
                    code, vehicule);
        };
    }

    public String formatMoroccanPhoneNumber(String rawNumber) {
        if (rawNumber == null || rawNumber.isBlank()) {
            return "";
        }
        String cleaned = rawNumber.replaceAll("[^0-9+]", "");
        if (cleaned.startsWith("0") && cleaned.length() == 10) {
            return "+212" + cleaned.substring(1);
        }
        if (cleaned.startsWith("212") && !cleaned.startsWith("+212")) {
            return "+" + cleaned;
        }
        return cleaned;
    }

    private String resolveVehicleLabel(Vehicle vehicle) {
        if (vehicle == null) {
            return "Véhicule";
        }
        String brandName = (vehicle.getBrand() != null && vehicle.getBrand().getName() != null)
                ? vehicle.getBrand().getName() : "";
        String model = vehicle.getModele() != null ? vehicle.getModele() : "";
        String label = (brandName + " " + model).trim();
        return label.isEmpty() ? "Véhicule" : label;
    }

    private String formatDate(LocalDate date) {
        return (date != null) ? date.format(DATE_FORMATTER) : "";
    }

    private String resolveLocation(Reservation reservation, Vehicle vehicle) {
        if (vehicle != null && vehicle.getVille() != null && !vehicle.getVille().isBlank()) {
            return vehicle.getVille();
        }
        return "Notre agence LocaVite";
    }
}
