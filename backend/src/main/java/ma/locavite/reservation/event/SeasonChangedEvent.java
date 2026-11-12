package ma.locavite.reservation.event;

import ma.locavite.reservation.domain.Season;

public record SeasonChangedEvent(Season season, String action) {
}
