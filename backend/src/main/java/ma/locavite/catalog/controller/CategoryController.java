package ma.locavite.catalog.controller;

import jakarta.validation.Valid;
import ma.locavite.catalog.dto.CatalogEntryRequest;
import ma.locavite.catalog.dto.CatalogEntryResponse;
import ma.locavite.catalog.service.CategoryService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;

@RestController
public class CategoryController {

    private final CategoryService service;

    public CategoryController(CategoryService service) {
        this.service = service;
    }

    @GetMapping("/api/categories")
    public List<CatalogEntryResponse> getAll() {
        return service.getAll();
    }

    @PostMapping("/api/admin/categories")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<CatalogEntryResponse> create(@Valid @RequestBody CatalogEntryRequest request) {
        CatalogEntryResponse created = service.create(request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(created.id())
                .toUri();
        return ResponseEntity.created(location).body(created);
    }
}
