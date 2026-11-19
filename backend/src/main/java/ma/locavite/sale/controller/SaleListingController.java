package ma.locavite.sale.controller;

import jakarta.validation.Valid;
import ma.locavite.sale.dto.SaleListingFilter;
import ma.locavite.sale.dto.SaleListingRequest;
import ma.locavite.sale.dto.SaleListingResponse;
import ma.locavite.sale.dto.SaleListingSearchFilter;
import ma.locavite.sale.service.SaleListingService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;

@RestController
@RequestMapping("/api/sale-listings")
public class SaleListingController {

    private final SaleListingService service;

    public SaleListingController(SaleListingService service) {
        this.service = service;
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('AGENCE', 'ADMIN')")
    public ResponseEntity<SaleListingResponse> create(@Valid @RequestBody SaleListingRequest request) {
        SaleListingResponse created = service.create(request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(created.id())
                .toUri();
        return ResponseEntity.created(location).body(created);
    }

    @GetMapping("/{id}")
    public ResponseEntity<SaleListingResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(service.getById(id));
    }

    @GetMapping
    public ResponseEntity<Page<SaleListingResponse>> search(
            SaleListingFilter filter,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(service.search(filter, pageable));
    }

    @GetMapping("/search")
    public ResponseEntity<Page<SaleListingResponse>> searchPublished(
            @Valid @ModelAttribute SaleListingSearchFilter filter,
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(service.searchPublished(filter, pageable));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('AGENCE', 'ADMIN')")
    public ResponseEntity<SaleListingResponse> update(
            @PathVariable Long id,
            @Valid @RequestBody SaleListingRequest request) {
        return ResponseEntity.ok(service.update(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('AGENCE', 'ADMIN')")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{id}/publish")
    @PreAuthorize("hasAnyRole('AGENCE', 'ADMIN')")
    public ResponseEntity<SaleListingResponse> publish(@PathVariable Long id) {
        return ResponseEntity.ok(service.publish(id));
    }

    @PutMapping("/{id}/mark-sold")
    @PreAuthorize("hasAnyRole('AGENCE', 'ADMIN')")
    public ResponseEntity<SaleListingResponse> markSold(@PathVariable Long id) {
        return ResponseEntity.ok(service.markSold(id));
    }
}
