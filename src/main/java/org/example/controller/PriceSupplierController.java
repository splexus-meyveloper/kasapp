package org.example.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.dto.request.CreatePriceSupplierRequest;
import org.example.dto.request.UpdatePriceSupplierRequest;
import org.example.dto.response.PriceSupplierResponse;
import org.example.security.CustomUserDetails;
import org.example.service.PriceSupplierService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/price-suppliers")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('FIYAT_KURAL_YONETIMI') or hasRole('ADMIN')")
public class PriceSupplierController {

    private final PriceSupplierService supplierService;

    @GetMapping
    public List<PriceSupplierResponse> list(@AuthenticationPrincipal CustomUserDetails user) {
        return supplierService.list(user.getCompanyId());
    }

    @PostMapping
    public PriceSupplierResponse create(
            @Valid @RequestBody CreatePriceSupplierRequest req,
            @AuthenticationPrincipal CustomUserDetails user) {
        return supplierService.create(req, user.getCompanyId(), user.getId());
    }

    @PatchMapping("/{id}")
    public PriceSupplierResponse update(
            @PathVariable Long id,
            @Valid @RequestBody UpdatePriceSupplierRequest req,
            @AuthenticationPrincipal CustomUserDetails user) {
        return supplierService.update(id, req, user.getCompanyId());
    }
}
