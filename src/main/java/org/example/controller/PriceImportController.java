package org.example.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.dto.request.CalculatePriceRequest;
import org.example.dto.request.ManualMatchRequest;
import org.example.dto.response.ManufacturerRowSearchResponse;
import org.example.dto.response.PriceCalcResultResponse;
import org.example.dto.response.PriceCalcRunResponse;
import org.example.dto.response.PriceImportBatchResponse;
import org.example.security.CustomUserDetails;
import org.example.service.PriceCalcExportService;
import org.example.service.PriceCalcService;
import org.example.service.PriceImportBatchService;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/price-import")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('FIYAT_KURAL_YONETIMI') or hasRole('ADMIN')")
public class PriceImportController {

    private final PriceImportBatchService batchService;
    private final PriceCalcService calcService;
    private final PriceCalcExportService exportService;

    @PostMapping("/{supplierId}/manufacturer-list")
    public PriceImportBatchResponse uploadManufacturerList(
            @PathVariable Long supplierId,
            @RequestParam(required = false) Long productGroupId,
            @RequestParam("file") MultipartFile file,
            @AuthenticationPrincipal CustomUserDetails user) throws Exception {
        return batchService.uploadManufacturerList(supplierId, productGroupId, file, user.getCompanyId(), user.getId());
    }

    @PostMapping("/{batchId}/cpm-stock")
    public PriceImportBatchResponse uploadStock(
            @PathVariable Long batchId,
            @RequestParam("file") MultipartFile file,
            @AuthenticationPrincipal CustomUserDetails user) throws Exception {
        return batchService.uploadStock(batchId, file, user.getCompanyId());
    }

    @PostMapping("/{batchId}/calculate")
    public PriceCalcRunResponse calculate(
            @PathVariable Long batchId,
            @Valid @RequestBody(required = false) CalculatePriceRequest req,
            @AuthenticationPrincipal CustomUserDetails user) {
        Map<String, java.math.BigDecimal> fxRates = req != null && req.fxRates() != null ? req.fxRates() : Map.of();
        var run = calcService.execute(batchId, user.getCompanyId(), user.getId(), fxRates);
        return calcService.getRun(run.getId(), user.getCompanyId());
    }

    @GetMapping("/calc-runs/{id}")
    public PriceCalcRunResponse getRun(@PathVariable Long id, @AuthenticationPrincipal CustomUserDetails user) {
        return calcService.getRun(id, user.getCompanyId());
    }

    @GetMapping("/calc-runs/{id}/results")
    public List<PriceCalcResultResponse> getResults(
            @PathVariable Long id,
            @RequestParam(required = false) Boolean matched,
            @AuthenticationPrincipal CustomUserDetails user) {
        return calcService.getResults(id, user.getCompanyId(), matched);
    }

    @GetMapping("/{batchId}/manufacturer-search")
    public List<ManufacturerRowSearchResponse> searchManufacturerRows(
            @PathVariable Long batchId,
            @RequestParam String q,
            @AuthenticationPrincipal CustomUserDetails user) {
        return calcService.searchManufacturerRows(batchId, q, user.getCompanyId());
    }

    @PostMapping("/{batchId}/results/{resultId}/manual-match")
    public PriceCalcResultResponse manualMatch(
            @PathVariable Long batchId,
            @PathVariable Long resultId,
            @Valid @RequestBody ManualMatchRequest req,
            @AuthenticationPrincipal CustomUserDetails user) {
        return calcService.manualMatch(resultId, req.manufacturerRowId(), batchId, user.getCompanyId(), user.getId(), req.fxRates());
    }

    @GetMapping("/calc-runs/{id}/export")
    public ResponseEntity<ByteArrayResource> export(
            @PathVariable Long id,
            @AuthenticationPrincipal CustomUserDetails user) throws Exception {
        calcService.getRun(id, user.getCompanyId()); // erişim kontrolü
        byte[] bytes = exportService.export(id);
        ByteArrayResource resource = new ByteArrayResource(bytes);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment().filename("fiyat-guncelleme-" + id + ".xlsx").build().toString())
                .body(resource);
    }
}
