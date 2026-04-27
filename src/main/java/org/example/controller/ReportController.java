package org.example.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.dto.request.ReportRequest;
import org.example.dto.response.ReportSummaryResponse;
import org.example.security.CustomUserDetails;
import org.example.service.ExcelExportService;
import org.example.service.ReportService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/reports")
@RequiredArgsConstructor
public class ReportController {

    private final ReportService reportService;
    private final ExcelExportService excelExportService;

    // JSON rapor — frontend grafik/tablo için
    @GetMapping
    public ReportSummaryResponse getReport(
            @AuthenticationPrincipal CustomUserDetails user,
            @RequestParam LocalDate startDate,
            @RequestParam LocalDate endDate) {

        return reportService.getReport(
                user.getCompanyId(),
                new ReportRequest(startDate, endDate)
        );
    }

    // Excel indir
    @GetMapping("/export/excel")
    public ResponseEntity<byte[]> exportExcel(
            @AuthenticationPrincipal CustomUserDetails user,
            @RequestParam LocalDate startDate,
            @RequestParam LocalDate endDate) throws Exception {

        byte[] excel = excelExportService.exportReport(
                user.getCompanyId(),
                new ReportRequest(startDate, endDate)
        );

        String filename = "rapor_" + startDate + "_" + endDate + ".xlsx";

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + filename + "\"")
                .contentType(MediaType.parseMediaType(
                        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(excel);
    }
}