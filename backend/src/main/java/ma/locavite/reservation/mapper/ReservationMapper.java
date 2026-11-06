package ma.locavite.reservation.mapper;

import ma.locavite.reservation.domain.Reservation;
import ma.locavite.reservation.dto.ReservationRequest;
import ma.locavite.reservation.dto.ReservationResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface ReservationMapper {

    ReservationResponse toResponse(Reservation reservation);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "code", ignore = true)
    @Mapping(target = "userId", ignore = true)
    @Mapping(target = "prixTotal", ignore = true)
    @Mapping(target = "caution", ignore = true)
    @Mapping(target = "statut", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    Reservation toEntity(ReservationRequest request);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "code", ignore = true)
    @Mapping(target = "userId", ignore = true)
    @Mapping(target = "prixTotal", ignore = true)
    @Mapping(target = "caution", ignore = true)
    @Mapping(target = "statut", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    void updateEntityFromRequest(ReservationRequest request, @MappingTarget Reservation entity);
}
