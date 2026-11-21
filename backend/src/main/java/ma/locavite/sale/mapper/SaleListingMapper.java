package ma.locavite.sale.mapper;

import ma.locavite.sale.domain.SaleListing;
import ma.locavite.sale.domain.SaleTaxRegime;
import ma.locavite.sale.domain.VehicleCondition;
import ma.locavite.sale.dto.SaleListingRequest;
import ma.locavite.sale.dto.SaleListingResponse;
import ma.locavite.vehicle.domain.VehiclePhoto;
import ma.locavite.vehicle.repository.VehiclePhotoRepository;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.springframework.beans.factory.annotation.Autowired;

@Mapper(componentModel = "spring")
public abstract class SaleListingMapper {

    @Autowired
    protected VehiclePhotoRepository vehiclePhotoRepository;

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
    public abstract SaleListing toEntity(SaleListingRequest request);

    @Mapping(target = "vehicleId", source = "vehicle.id")
    @Mapping(target = "garantieConstructeur", expression = "java(entity.getEtat() == VehicleCondition.NEW)")
    @Mapping(target = "regimeTva", expression = "java(entity.getEtat() == VehicleCondition.NEW ? SaleTaxRegime.STANDARD_20_PERCENT : SaleTaxRegime.MARGIN)")
    @Mapping(target = "marque", source = "vehicle.brand.name")
    @Mapping(target = "modele", source = "vehicle.modele")
    @Mapping(target = "annee", source = "vehicle.annee")
    @Mapping(target = "ville", source = "vehicle.ville")
    @Mapping(target = "carburant", expression = "java(entity.getVehicle().getCarburant().name())")
    @Mapping(target = "categorie", source = "vehicle.category.name")
    @Mapping(target = "photoUrl", expression = "java(resolvePhotoUrl(entity))")
    public abstract SaleListingResponse toResponse(SaleListing entity);

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
    public abstract void updateEntity(SaleListingRequest request, @MappingTarget SaleListing entity);

    protected String resolvePhotoUrl(SaleListing entity) {
        if (entity.getVehicle() == null) {
            return null;
        }
        var photos = vehiclePhotoRepository.findByVehicleIdOrderByOrdreAsc(entity.getVehicle().getId());
        if (photos == null || photos.isEmpty()) {
            return null;
        }
        return photos.get(0).getPath();
    }
}
