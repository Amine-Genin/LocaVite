package ma.locavite.catalog.service;

import ma.locavite.catalog.domain.CatalogEntity;
import ma.locavite.catalog.dto.CatalogEntryRequest;
import ma.locavite.catalog.dto.CatalogEntryResponse;
import ma.locavite.catalog.exception.DuplicateCatalogEntryException;
import ma.locavite.catalog.repository.CatalogRepository;
import org.springframework.dao.DataIntegrityViolationException;

import java.util.List;

public abstract class AbstractCatalogService<T extends CatalogEntity> {

    private final CatalogRepository<T> repository;

    protected AbstractCatalogService(CatalogRepository<T> repository) {
        this.repository = repository;
    }

    protected List<CatalogEntryResponse> getEntries() {
        return repository.findAllByOrderByNameAsc().stream()
                .map(entry -> new CatalogEntryResponse(entry.getId(), entry.getName()))
                .toList();
    }

    protected CatalogEntryResponse createEntry(CatalogEntryRequest request) {
        String name = normalizeName(request.name().trim());
        if (repository.existsByNameIgnoreCase(name)) {
            throw new DuplicateCatalogEntryException(name);
        }

        T entry = newEntity(name);
        try {
            T saved = repository.saveAndFlush(entry);
            return new CatalogEntryResponse(saved.getId(), saved.getName());
        } catch (DataIntegrityViolationException exception) {
            throw new DuplicateCatalogEntryException(name);
        }
    }

    protected abstract T newEntity(String name);

    protected abstract String normalizeName(String name);
}
