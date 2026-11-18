package ma.locavite.sale.repository;

import ma.locavite.sale.domain.SaleListing;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SaleListingRepository extends JpaRepository<SaleListing, Long>, JpaSpecificationExecutor<SaleListing> {

    Optional<SaleListing> findByCode(String code);

    boolean existsByCode(String code);

    boolean existsByNumeroChassisIgnoreCaseAndDeletedFalse(String numeroChassis);

    boolean existsByNumeroChassisIgnoreCaseAndIdNotAndDeletedFalse(String numeroChassis, Long id);

    List<SaleListing> findByVehicleIdAndDeletedFalse(Long vehicleId);
}
