package ma.locavite.auth.service;

import ma.locavite.auth.domain.User;
import ma.locavite.reservation.domain.ContractLanguage;
import ma.locavite.reservation.domain.Reservation;
import ma.locavite.reservation.domain.ReservationContract;

public interface EmailService {

    void sendConfirmationEmail(User user);

    void sendReservationConfirmation(Reservation reservation);

    void sendReservationConfirmation(Reservation reservation, ReservationContract contract);

    void sendReservationConfirmation(Reservation reservation, ReservationContract contract, ContractLanguage language);
}
