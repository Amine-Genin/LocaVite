package ma.locavite.vehicle.mapper;

import ma.locavite.vehicle.domain.Vehicle;
import ma.locavite.vehicle.dto.VehicleRequest;
import ma.locavite.vehicle.dto.VehicleResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface VehicleMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "brand", ignore = true)
    @Mapping(target = "category", ignore = true)
    @Mapping(target = "statut", ignore = true)
    @Mapping(target = "agenceId", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    Vehicle toEntity(VehicleRequest request);

    @Mapping(target = "brandId", source = "brand.id")
    @Mapping(target = "marque", source = "brand.name")
    @Mapping(target = "categoryId", source = "category.id")
    @Mapping(target = "categorie", source = "category.name")
    VehicleResponse toResponse(Vehicle vehicle);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "brand", ignore = true)
    @Mapping(target = "category", ignore = true)
    @Mapping(target = "statut", ignore = true)
    @Mapping(target = "agenceId", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    void updateEntity(VehicleRequest request, @MappingTarget Vehicle vehicle);
}