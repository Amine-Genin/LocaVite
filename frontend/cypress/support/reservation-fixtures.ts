type ReservationFixture = {
  statut: string;
  dateDebut?: string;
  dateFin?: string;
};

const RESERVATION_DATE_OFFSETS: Record<string, [number, number]> = {
  PENDING: [2, 7],
  CONFIRMED: [2, 7],
  ACTIVE: [-2, 3],
  COMPLETED: [-30, -25],
  CANCELLED: [-15, -10],
};

function formatUtcDateFromToday(days: number, today: Date): string {
  const date = new Date(today);
  date.setUTCDate(date.getUTCDate() + days);
  return date.toISOString().slice(0, 10);
}

/** Keeps reservation fixture dates aligned with the status filters on the current day. */
export function withCurrentReservationDates<T extends ReservationFixture>(
  reservations: T[],
): T[] {
  const today = new Date();

  return reservations.map((reservation) => {
    const offsets = RESERVATION_DATE_OFFSETS[reservation.statut];
    if (!offsets) return reservation;

    return {
      ...reservation,
      dateDebut: formatUtcDateFromToday(offsets[0], today),
      dateFin: formatUtcDateFromToday(offsets[1], today),
    };
  });
}
