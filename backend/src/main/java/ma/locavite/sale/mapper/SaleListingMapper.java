package ma.locavite.sale.mapper;

import ma.locavite.sale.domain.SaleListing;
import ma.locavite.sale.domain.SaleTaxRegime;
import ma.locavite.sale.domain.VehicleCondition;
import ma.locavite.sale.dto.SaleListingRequest;
import ma.locavite.sale.dto.SaleListingResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface SaleListingMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "code", ignore = true)
    @Mapping(target = "vehicle", ignore = true)
    @Mapping(target = "agenceId", ignore = true)
    @Mapping(target = "statut", ignore = true)
    @Mapping(target = "deleted", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "publishedAt", ignore = true)
    @Mapping(target = "soldAt", ignore = true)
    SaleListing toEntity(SaleListingRequest request);

    @Mapping(target = "vehicleId", source = "vehicle.id")
    @Mapping(target = "garantieConstructeur", expression = "java(entity.getEtat() == VehicleCondition.NEW)")
    @Mapping(target = "regimeTva", expression = "java(entity.getEtat() == VehicleCondition.NEW ? SaleTaxRegime.STANDARD_20_PERCENT : SaleTaxRegime.MARGIN)")
    SaleListingResponse toResponse(SaleListing entity);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "code", ignore = true)
    @Mapping(target = "vehicle", ignore = true)
    @Mapping(target = "agenceId", ignore = true)
    @Mapping(target = "statut", ignore = true)
    @Mapping(target = "deleted", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "publishedAt", ignore = true)
    @Mapping(target = "soldAt", ignore = true)
    void updateEntity(SaleListingRequest request, @MappingTarget SaleListing entity);
}
