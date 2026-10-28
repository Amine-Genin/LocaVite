package ma.locavite.catalog.repository;

import ma.locavite.catalog.domain.CatalogEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.NoRepositoryBean;

import java.util.List;
import java.util.Optional;

@NoRepositoryBean
public interface CatalogRepository<T extends CatalogEntity> extends JpaRepository<T, Long> {

    List<T> findAllByOrderByNameAsc();

    boolean existsByNameIgnoreCase(String name);

    Optional<T> findByNameIgnoreCase(String name);
}
