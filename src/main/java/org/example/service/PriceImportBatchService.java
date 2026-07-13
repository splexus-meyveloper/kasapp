package org.example.service;

import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.example.dto.response.PriceImportBatchResponse;
import org.example.entity.PriceImportBatch;
import org.example.entity.PriceImportRow;
import org.example.entity.PriceImportTemplate;
import org.example.entity.StockSnapshot;
import org.example.exception.ErrorType;
import org.example.exception.KasappException;
import org.example.repository.PriceImportBatchRepository;
import org.example.repository.PriceImportRowRepository;
import org.example.repository.PriceSupplierRepository;
import org.example.repository.StockSnapshotRepository;
import org.example.skills.enums.PriceImportBatchStatus;
import org.example.skills.enums.PriceImportTemplateType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PriceImportBatchService {

    private final PriceImportBatchRepository batchRepository;
    private final PriceImportRowRepository importRowRepository;
    private final StockSnapshotRepository stockSnapshotRepository;
    private final PriceSupplierRepository supplierRepository;
    private final PriceImportTemplateService templateService;
    private final PriceExcelParseService parseService;

    @Transactional
    public PriceImportBatchResponse uploadManufacturerList(Long supplierId, Long productGroupId, MultipartFile file,
                                                            Long companyId, Long userId) throws Exception {
        supplierRepository.findByIdAndCompanyId(supplierId, companyId)
                .orElseThrow(() -> new KasappException(ErrorType.PRICE_SUPPLIER_NOT_FOUND));

        PriceImportTemplate template = templateService
                .findFor(companyId, PriceImportTemplateType.MANUFACTURER_LIST, supplierId)
                .orElseThrow(() -> new KasappException(ErrorType.PRICE_IMPORT_TEMPLATE_NOT_FOUND));

        PriceImportBatch batch = PriceImportBatch.builder()
                .companyId(companyId)
                .supplierId(supplierId)
                .productGroupId(productGroupId)
                .manufacturerFileName(file.getOriginalFilename())
                .uploadedBy(userId)
                .uploadedAt(LocalDateTime.now())
                .status(PriceImportBatchStatus.MANUFACTURER_UPLOADED)
                .build();
        batch = batchRepository.save(batch);

        List<PriceImportRow> rows;
        // WorkbookFactory .xls (HSSF) ve .xlsx (XSSF) formatlarını dosya imzasından otomatik ayırt eder.
        try (Workbook wb = WorkbookFactory.create(file.getInputStream())) {
            rows = parseService.parseManufacturerList(wb, template, batch.getId(), templateService.parseMappings(template));
        } catch (Exception e) {
            throw new KasappException(ErrorType.PRICE_EXCEL_HATALI);
        }
        importRowRepository.saveAll(rows);

        return new PriceImportBatchResponse(batch.getId(), supplierId, batch.getStatus(), rows.size());
    }

    @Transactional
    public PriceImportBatchResponse uploadStock(Long batchId, MultipartFile file, Long companyId) throws Exception {
        PriceImportBatch batch = batchRepository.findByIdAndCompanyId(batchId, companyId)
                .orElseThrow(() -> new KasappException(ErrorType.PRICE_IMPORT_BATCH_NOT_FOUND));

        PriceImportTemplate template = templateService
                .findFor(companyId, PriceImportTemplateType.CPM_STOCK, null)
                .orElseThrow(() -> new KasappException(ErrorType.PRICE_IMPORT_TEMPLATE_NOT_FOUND));

        List<StockSnapshot> rows;
        try (Workbook wb = WorkbookFactory.create(file.getInputStream())) {
            rows = parseService.parseStock(wb, template, batch.getId(), templateService.parseMappings(template));
        } catch (Exception e) {
            throw new KasappException(ErrorType.PRICE_EXCEL_HATALI);
        }
        stockSnapshotRepository.saveAll(rows);

        batch.setStockFileName(file.getOriginalFilename());
        batch.setStatus(PriceImportBatchStatus.STOCK_UPLOADED);
        batchRepository.save(batch);

        return new PriceImportBatchResponse(batch.getId(), batch.getSupplierId(), batch.getStatus(), rows.size());
    }
}
