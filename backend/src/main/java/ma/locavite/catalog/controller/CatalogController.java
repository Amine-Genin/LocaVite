package ma.locavite.catalog.controller;

import jakarta.validation.Valid;
import ma.locavite.catalog.dto.CatalogEntryRequest;
import ma.locavite.catalog.dto.CatalogEntryResponse;
import ma.locavite.catalog.service.BrandService;
import ma.locavite.catalog.service.CategoryService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;

@RestController
public class CatalogController {

    private final BrandService brandService;
    private final CategoryService categoryService;

    public CatalogController(BrandService brandService, CategoryService categoryService) {
        this.brandService = brandService;
        this.categoryService = categoryService;
    }

    @GetMapping("/api/brands")
    public List<CatalogEntryResponse> getBrands() {
        return brandService.getAll();
    }

    @PostMapping("/api/admin/brands")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<CatalogEntryResponse> createBrand(@Valid @RequestBody CatalogEntryRequest request) {
        return createdResponse(brandService.create(request));
    }

    @GetMapping("/api/categories")
    public List<CatalogEntryResponse> getCategories() {
        return categoryService.getAll();
    }

    @PostMapping("/api/admin/categories")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<CatalogEntryResponse> createCategory(@Valid @RequestBody CatalogEntryRequest request) {
        return createdResponse(categoryService.create(request));
    }

    private ResponseEntity<CatalogEntryResponse> createdResponse(CatalogEntryResponse created) {
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(created.id())
                .toUri();
        return ResponseEntity.created(location).body(created);
    }
}
