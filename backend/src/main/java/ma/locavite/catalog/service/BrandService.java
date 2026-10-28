package ma.locavite.catalog.service;

import ma.locavite.catalog.dto.CatalogEntryRequest;
import ma.locavite.catalog.dto.CatalogEntryResponse;
import ma.locavite.catalog.domain.Brand;
import ma.locavite.catalog.repository.BrandRepository;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;

@Service
@Transactional(readOnly = true)
public class BrandService extends AbstractCatalogService<Brand> {

    public BrandService(BrandRepository repository) {
        super(repository);
    }

    @Cacheable("brands")
    public List<CatalogEntryResponse> getAll() {
        return getEntries();
    }

    @Transactional
    @CacheEvict(value = "brands", allEntries = true)
    public CatalogEntryResponse create(CatalogEntryRequest request) {
        return createEntry(request);
    }

    @Override
    protected Brand newEntity(String name) {
        Brand brand = new Brand();
        brand.setName(name);
        return brand;
    }

    @Override
    protected String normalizeName(String name) {
        return name.substring(0, 1).toUpperCase(Locale.ROOT) + name.substring(1);
    }
}
