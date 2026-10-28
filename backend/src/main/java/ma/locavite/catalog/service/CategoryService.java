package ma.locavite.catalog.service;

import ma.locavite.catalog.dto.CatalogEntryRequest;
import ma.locavite.catalog.dto.CatalogEntryResponse;
import ma.locavite.catalog.domain.Category;
import ma.locavite.catalog.repository.CategoryRepository;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;

@Service
@Transactional(readOnly = true)
public class CategoryService extends AbstractCatalogService<Category> {

    public CategoryService(CategoryRepository repository) {
        super(repository);
    }

    @Cacheable("categories")
    public List<CatalogEntryResponse> getAll() {
        return getEntries();
    }

    @Transactional
    @CacheEvict(value = "categories", allEntries = true)
    public CatalogEntryResponse create(CatalogEntryRequest request) {
        return createEntry(request);
    }

    @Override
    protected Category newEntity(String name) {
        Category category = new Category();
        category.setName(name);
        return category;
    }

    @Override
    protected String normalizeName(String name) {
        return name.toUpperCase(Locale.ROOT);
    }
}
