package ma.locavite.catalog.service;

import ma.locavite.catalog.domain.Category;
import ma.locavite.catalog.dto.CatalogEntryRequest;
import ma.locavite.catalog.dto.CatalogEntryResponse;
import ma.locavite.catalog.exception.DuplicateCatalogEntryException;
import ma.locavite.catalog.repository.CategoryRepository;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;

@Service
@Transactional(readOnly = true)
public class CategoryService {

    private final CategoryRepository repository;

    public CategoryService(CategoryRepository repository) {
        this.repository = repository;
    }

    @Cacheable("categories")
    public List<CatalogEntryResponse> getAll() {
        return repository.findAllByOrderByNameAsc().stream()
                .map(category -> new CatalogEntryResponse(category.getId(), category.getName()))
                .toList();
    }

    @Transactional
    @CacheEvict(value = "categories", allEntries = true)
    public CatalogEntryResponse create(CatalogEntryRequest request) {
        String name = request.name().trim().toUpperCase(Locale.ROOT);
        if (repository.existsByNameIgnoreCase(name)) {
            throw new DuplicateCatalogEntryException(name);
        }

        Category category = new Category();
        category.setName(name);
        try {
            Category saved = repository.saveAndFlush(category);
            return new CatalogEntryResponse(saved.getId(), saved.getName());
        } catch (DataIntegrityViolationException exception) {
            throw new DuplicateCatalogEntryException(name);
        }
    }
}
