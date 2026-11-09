package ma.locavite.catalog.service;

import ma.locavite.catalog.domain.Brand;
import ma.locavite.catalog.domain.Category;
import ma.locavite.catalog.dto.CatalogEntryRequest;
import ma.locavite.catalog.dto.CatalogEntryResponse;
import ma.locavite.catalog.repository.BrandRepository;
import ma.locavite.catalog.repository.CategoryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.cache.CacheManager;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@SpringBootTest(properties = "spring.cache.type=simple")
class CatalogCacheIT {

    @Autowired
    private BrandService brandService;

    @Autowired
    private CategoryService categoryService;

    @SpyBean
    private BrandRepository brandRepository;

    @SpyBean
    private CategoryRepository categoryRepository;

    @Autowired
    private CacheManager cacheManager;

    @BeforeEach
    void setUp() {
        if (cacheManager.getCache("brands") != null) {
            cacheManager.getCache("brands").clear();
        }
        if (cacheManager.getCache("categories") != null) {
            cacheManager.getCache("categories").clear();
        }
    }

    @Test
    @DisplayName("GET /api/brands : 2 appels identiques → 1 seule requête DB, POST invalide le cache")
    void brands_cacheHitAndEviction() {
        reset(brandRepository);

        // 1er appel : appel DB
        List<CatalogEntryResponse> first = brandService.getAll();
        verify(brandRepository, times(1)).findAllByOrderByNameAsc();

        // 2ème appel : résultat servi par le cache sans nouvelle requête DB
        List<CatalogEntryResponse> second = brandService.getAll();
        assertThat(second).isEqualTo(first);
        verify(brandRepository, times(1)).findAllByOrderByNameAsc();

        // POST (création) : invalide le cache brands
        brandService.create(new CatalogEntryRequest("BrandCacheTest"));

        // 3ème appel : doit interroger à nouveau la base de données
        List<CatalogEntryResponse> third = brandService.getAll();
        assertThat(third).anyMatch(b -> "Brandcachetest".equalsIgnoreCase(b.name()));
        verify(brandRepository, times(2)).findAllByOrderByNameAsc();
    }

    @Test
    @DisplayName("GET /api/categories : 2 appels identiques → 1 seule requête DB, POST invalide le cache")
    void categories_cacheHitAndEviction() {
        reset(categoryRepository);

        // 1er appel : appel DB
        List<CatalogEntryResponse> first = categoryService.getAll();
        verify(categoryRepository, times(1)).findAllByOrderByNameAsc();

        // 2ème appel : résultat servi par le cache sans nouvelle requête DB
        List<CatalogEntryResponse> second = categoryService.getAll();
        assertThat(second).isEqualTo(first);
        verify(categoryRepository, times(1)).findAllByOrderByNameAsc();

        // POST (création) : invalide le cache categories
        categoryService.create(new CatalogEntryRequest("CATTEST"));

        // 3ème appel : doit interroger à nouveau la base de données
        List<CatalogEntryResponse> third = categoryService.getAll();
        assertThat(third).anyMatch(c -> "CATTEST".equalsIgnoreCase(c.name()));
        verify(categoryRepository, times(2)).findAllByOrderByNameAsc();
    }
}
