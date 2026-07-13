package org.example.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.dto.request.CreateProductGroupRequest;
import org.example.dto.request.UpdateProductGroupRequest;
import org.example.dto.response.ProductGroupResponse;
import org.example.security.CustomUserDetails;
import org.example.service.ProductGroupService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/price-product-groups")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('FIYAT_KURAL_YONETIMI') or hasRole('ADMIN')")
public class ProductGroupController {

    private final ProductGroupService productGroupService;

    @GetMapping
    public List<ProductGroupResponse> list(
            @RequestParam(required = false) Long supplierId,
            @AuthenticationPrincipal CustomUserDetails user) {
        return productGroupService.list(user.getCompanyId(), supplierId);
    }

    @PostMapping
    public ProductGroupResponse create(
            @Valid @RequestBody CreateProductGroupRequest req,
            @AuthenticationPrincipal CustomUserDetails user) {
        return productGroupService.create(req, user.getCompanyId(), user.getId());
    }

    @PatchMapping("/{id}")
    public ProductGroupResponse update(
            @PathVariable Long id,
            @Valid @RequestBody UpdateProductGroupRequest req,
            @AuthenticationPrincipal CustomUserDetails user) {
        return productGroupService.update(id, req, user.getCompanyId());
    }
}
