package org.example.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.dto.request.CreatePriceRuleRequest;
import org.example.dto.request.PreviewPriceRuleRequest;
import org.example.dto.request.UpdatePriceRuleStepsRequest;
import org.example.dto.response.PricePreviewResponse;
import org.example.dto.response.PriceRuleAuditResponse;
import org.example.dto.response.PriceRuleResponse;
import org.example.security.CustomUserDetails;
import org.example.service.PriceRuleService;
import org.example.skills.enums.PriceRuleStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/price-rules")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('FIYAT_KURAL_YONETIMI') or hasRole('ADMIN')")
public class PriceRuleController {

    private final PriceRuleService priceRuleService;

    @GetMapping
    public List<PriceRuleResponse> list(
            @RequestParam(required = false) Long supplierId,
            @RequestParam(required = false) PriceRuleStatus status,
            @AuthenticationPrincipal CustomUserDetails user) {
        return priceRuleService.list(user.getCompanyId(), supplierId, status);
    }

    @GetMapping("/{id}")
    public PriceRuleResponse getOne(@PathVariable Long id, @AuthenticationPrincipal CustomUserDetails user) {
        return priceRuleService.getOne(id, user.getCompanyId());
    }

    @GetMapping("/group/{ruleGroupKey}/versions")
    public List<PriceRuleResponse> versions(
            @PathVariable String ruleGroupKey,
            @AuthenticationPrincipal CustomUserDetails user) {
        return priceRuleService.versions(user.getCompanyId(), ruleGroupKey);
    }

    @PostMapping
    public PriceRuleResponse create(
            @Valid @RequestBody CreatePriceRuleRequest req,
            @AuthenticationPrincipal CustomUserDetails user) {
        return priceRuleService.create(req, user.getCompanyId(), user.getId());
    }

    @PostMapping("/{id}/new-version")
    public PriceRuleResponse newVersion(@PathVariable Long id, @AuthenticationPrincipal CustomUserDetails user) {
        return priceRuleService.newVersion(id, user.getCompanyId(), user.getId());
    }

    @PutMapping("/{id}/steps")
    public PriceRuleResponse updateSteps(
            @PathVariable Long id,
            @Valid @RequestBody UpdatePriceRuleStepsRequest req,
            @AuthenticationPrincipal CustomUserDetails user) {
        return priceRuleService.updateSteps(id, req, user.getCompanyId(), user.getId());
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id, @AuthenticationPrincipal CustomUserDetails user) {
        priceRuleService.delete(id, user.getCompanyId());
    }

    @DeleteMapping("/group/{ruleGroupKey}")
    public void deleteGroup(@PathVariable String ruleGroupKey, @AuthenticationPrincipal CustomUserDetails user) {
        priceRuleService.deleteGroup(ruleGroupKey, user.getCompanyId());
    }

    @PostMapping("/{id}/activate")
    public PriceRuleResponse activate(@PathVariable Long id, @AuthenticationPrincipal CustomUserDetails user) {
        return priceRuleService.activate(id, user.getCompanyId(), user.getId());
    }

    @PostMapping("/{id}/deactivate")
    public PriceRuleResponse deactivate(@PathVariable Long id, @AuthenticationPrincipal CustomUserDetails user) {
        return priceRuleService.deactivate(id, user.getCompanyId(), user.getId());
    }

    @PostMapping("/{id}/preview")
    public PricePreviewResponse preview(
            @PathVariable Long id,
            @Valid @RequestBody PreviewPriceRuleRequest req,
            @AuthenticationPrincipal CustomUserDetails user) {
        return priceRuleService.preview(id, req, user.getCompanyId());
    }

    @GetMapping("/{id}/audit")
    public List<PriceRuleAuditResponse> audit(@PathVariable Long id, @AuthenticationPrincipal CustomUserDetails user) {
        return priceRuleService.audit(id, user.getCompanyId());
    }
}
