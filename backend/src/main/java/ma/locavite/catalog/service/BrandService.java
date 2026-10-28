package ma.locavite.catalog.service;

import ma.locavite.catalog.domain.Brand;
import ma.locavite.catalog.dto.CatalogEntryRequest;
import ma.locavite.catalog.dto.CatalogEntryResponse;
import ma.locavite.catalog.exception.DuplicateCatalogEntryException;
import ma.locavite.catalog.repository.BrandRepository;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;

@Service
@Transactional(readOnly = true)
public class BrandService {

    private final BrandRepository repository;

    public BrandService(BrandRepository repository) {
        this.repository = repository;
    }

    @Cacheable("brands")
    public List<CatalogEntryResponse> getAll() {
        return repository.findAllByOrderByNameAsc().stream()
                .map(brand -> new CatalogEntryResponse(brand.getId(), brand.getName()))
                .toList();
    }

    @Transactional
    @CacheEvict(value = "brands", allEntries = true)
    public CatalogEntryResponse create(CatalogEntryRequest request) {
        String name = request.name().trim();
        if (repository.existsByNameIgnoreCase(name)) {
            throw new DuplicateCatalogEntryException(name);
        }

        Brand brand = new Brand();
        brand.setName(normalizeName(name));
        try {
            Brand saved = repository.saveAndFlush(brand);
            return new CatalogEntryResponse(saved.getId(), saved.getName());
        } catch (DataIntegrityViolationException exception) {
            throw new DuplicateCatalogEntryException(name);
        }
    }

    private static String normalizeName(String name) {
        return name.substring(0, 1).toUpperCase(Locale.ROOT) + name.substring(1);
    }
}
