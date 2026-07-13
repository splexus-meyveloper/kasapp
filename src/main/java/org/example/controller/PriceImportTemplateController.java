package org.example.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.example.dto.request.SavePriceImportTemplateRequest;
import org.example.dto.response.PriceImportTemplateResponse;
import org.example.dto.response.SheetPreviewResponse;
import org.example.exception.ErrorType;
import org.example.exception.KasappException;
import org.example.security.CustomUserDetails;
import org.example.service.PriceExcelParseService;
import org.example.service.PriceImportTemplateService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/price-import-templates")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('FIYAT_KURAL_YONETIMI') or hasRole('ADMIN')")
public class PriceImportTemplateController {

    private final PriceImportTemplateService templateService;
    private final PriceExcelParseService parseService;

    @GetMapping
    public List<PriceImportTemplateResponse> list(@AuthenticationPrincipal CustomUserDetails user) {
        return templateService.list(user.getCompanyId());
    }

    @PostMapping
    public PriceImportTemplateResponse save(
            @Valid @RequestBody SavePriceImportTemplateRequest req,
            @AuthenticationPrincipal CustomUserDetails user) {
        return templateService.save(req, user.getCompanyId());
    }

    /** Kolon eşleme ekranı için — dosyanın ilk satırlarını ham metin olarak döner. */
    @PostMapping("/preview")
    public SheetPreviewResponse preview(@RequestParam("file") MultipartFile file) {
        // WorkbookFactory hem eski .xls (BIFF/HSSF) hem yeni .xlsx (OOXML/XSSF) formatını
        // dosya imzasına bakarak otomatik algılar — CPM'den gelen dosyalar sıklıkla .xls olabiliyor.
        try (var wb = WorkbookFactory.create(file.getInputStream())) {
            return new SheetPreviewResponse(parseService.previewRows(wb, 15));
        } catch (Exception e) {
            throw new KasappException(ErrorType.PRICE_EXCEL_HATALI);
        }
    }
}
