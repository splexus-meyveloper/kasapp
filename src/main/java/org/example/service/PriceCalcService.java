package org.example.service;

import lombok.RequiredArgsConstructor;
import org.example.dto.response.ManufacturerRowSearchResponse;
import org.example.dto.response.PriceCalcResultResponse;
import org.example.dto.response.PriceCalcRunResponse;
import org.example.entity.*;
import org.example.exception.ErrorType;
import org.example.exception.KasappException;
import org.example.repository.*;
import org.example.skills.enums.PriceCalcRunStatus;
import org.example.skills.enums.PriceImportBatchStatus;
import org.example.skills.enums.SalesSlot;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Bir yükleme oturumundaki (batch) eşleşen ürünlere aktif fiyat kuralını
 * uygulayıp PriceCalcResult (rapor) satırları üretir. Kural çözümleme
 * önceliği: batch'in ürün grubuna özel aktif kural → yoksa tedarikçinin
 * genel (productGroupId=null) aktif kuralı.
 */
@Service
@RequiredArgsConstructor
public class PriceCalcService {

    private final PriceImportBatchRepository batchRepository;
    private final PriceImportRowRepository importRowRepository;
    private final StockSnapshotRepository stockSnapshotRepository;
    private final PriceSupplierRepository supplierRepository;
    private final PriceRuleRepository ruleRepository;
    private final PriceRuleStepRepository stepRepository;
    private final PriceCalcRunRepository calcRunRepository;
    private final PriceCalcResultRepository calcResultRepository;
    private final PriceCodeCrossReferenceRepository xrefRepository;
    private final PriceMatchingService matchingService;
    private final PriceRuleEngine ruleEngine;

    @Transactional
    public PriceCalcRun execute(Long batchId, Long companyId, Long userId, Map<String, BigDecimal> fxRates) {
        PriceImportBatch batch = batchRepository.findByIdAndCompanyId(batchId, companyId)
                .orElseThrow(() -> new KasappException(ErrorType.PRICE_IMPORT_BATCH_NOT_FOUND));

        PriceSupplier supplier = supplierRepository.findByIdAndCompanyId(batch.getSupplierId(), companyId)
                .orElseThrow(() -> new KasappException(ErrorType.PRICE_SUPPLIER_NOT_FOUND));

        List<PriceImportRow> rows = importRowRepository.findByBatchId(batchId);
        List<StockSnapshot> snapshots = stockSnapshotRepository.findByBatchId(batchId);
        List<PriceCodeCrossReference> xrefs = xrefRepository.findByCompanyIdAndSupplierId(companyId, supplier.getId());
        var matches = matchingService.match(rows, snapshots, supplier, xrefs);

        // matchingService.match() üretici listesi satırı başına bir eşleşme döner (o yön
        // için gereken tüm eşleştirme mantığı — hafıza, ön ek vb. — orada uygulanıyor).
        // Rapor ise kullanıcının KENDİ stok listesi (CPM) baz alınarak gösterilmeli — bu
        // yüzden burada CPM stok kaydı bazında ters bir indeks kurulup öyle iterasyona giriliyor.
        Map<Long, PriceMatchingService.MatchResult> matchByStockSnapshotId = new HashMap<>();
        for (var m : matches) {
            if (m.isMatched()) matchByStockSnapshotId.putIfAbsent(m.snapshot().getId(), m);
        }

        PriceRule activeRule = resolveActiveRule(companyId, supplier.getId(), batch.getProductGroupId());
        List<PriceRuleStep> steps = activeRule != null
                ? stepRepository.findByRuleIdOrderByStepOrderAsc(activeRule.getId())
                : List.of();

        PriceCalcRun run = PriceCalcRun.builder()
                .batchId(batchId)
                .companyId(companyId)
                .triggeredBy(userId)
                .startedAt(LocalDateTime.now())
                .status(PriceCalcRunStatus.RUNNING)
                .build();
        run = calcRunRepository.save(run);

        int matchedCount = 0, unmatchedCount = 0;
        for (StockSnapshot snap : snapshots) {
            PriceCalcResult.PriceCalcResultBuilder result = PriceCalcResult.builder()
                    .calcRunId(run.getId())
                    .stockCode(snap.getStockCode())
                    .productName(snap.getDescription())
                    .oldSales1(snap.getCurrentSales1()).oldSales2(snap.getCurrentSales2())
                    .oldSales3(snap.getCurrentSales3()).oldSales4(snap.getCurrentSales4());

            var m = matchByStockSnapshotId.get(snap.getId());
            if (m == null) {
                unmatchedCount++;
                calcResultRepository.save(result
                        .matched(false)
                        .reason("Üretici listesinde eşleşen fiyat bulunamadı")
                        .build());
                continue;
            }

            matchedCount++;
            result.matched(true)
                    .manufacturerCode(m.row().getManufacturerCode())
                    .netAlis(convertToTry(m.row().getListPrice(), m.row().getCurrencyCode(), fxRates));

            // Hafızadan gelmeyen (yeni) bir eşleşme bulunduysa kalıcı hafızaya yaz —
            // bir sonraki çalıştırmada bu ürün için Üretici Mal Kodu alanı boş/hatalı olsa bile
            // eşleşme hatırlanır.
            if (!"HAFIZA".equals(m.matchSource())) {
                rememberMatch(companyId, supplier.getId(), m.row().getManufacturerCode(), snap.getStockCode(), userId);
            }

            if (activeRule == null) {
                calcResultRepository.save(result.reason("Bu tedarikçi için aktif kural tanımlı değil").build());
                continue;
            }

            result.ruleId(activeRule.getId()).ruleVersionNo(activeRule.getVersionNo());

            // Kural adımlarındaki bir veri hatası (örn. APPLY_FX_RATE adımına gerçek bir para
            // birimi kodu yerine kur değeri girilmiş olması) tüm hesaplamayı (transaction'ı)
            // sessizce iptal edip diğer tüm ürünlerin sonucunu da kaybettirmesin — bu satır için
            // sebep olarak raporlanır, geri kalan ürünler etkilenmeden hesaplanmaya devam eder.
            try {
                var ctx = ruleEngine.execute(steps, m.row().getListPrice(), fxRates);
                Map<SalesSlot, BigDecimal> newSlots = ctx.getSalesSlots();
                result.newSales1(newSlots.get(SalesSlot.SATIS1))
                        .newSales2(newSlots.get(SalesSlot.SATIS2))
                        .newSales3(newSlots.get(SalesSlot.SATIS3))
                        .newSales4(newSlots.get(SalesSlot.SATIS4))
                        .updatedSlots(newSlots.keySet().stream().map(Enum::name).collect(Collectors.joining(",")))
                        .changePercent(computeChangePercent(snap.getCurrentSales1(), newSlots.get(SalesSlot.SATIS1)));
            } catch (RuntimeException e) {
                result.reason("Kural uygulanamadı: " + e.getMessage());
            }

            calcResultRepository.save(result.build());
        }

        run.setFinishedAt(LocalDateTime.now());
        run.setStatus(PriceCalcRunStatus.COMPLETED);
        run.setTotalMatched(matchedCount);
        run.setTotalUnmatched(unmatchedCount);
        calcRunRepository.save(run);

        batch.setStatus(PriceImportBatchStatus.CALCULATED);
        batchRepository.save(batch);

        return run;
    }

    /** Ürün grubuna özel aktif kural varsa onu, yoksa tedarikçinin genel kuralını döner. */
    private PriceRule resolveActiveRule(Long companyId, Long supplierId, Long productGroupId) {
        if (productGroupId != null) {
            var groupRule = ruleRepository.findActiveFor(companyId, supplierId, productGroupId);
            if (groupRule.isPresent()) return groupRule.get();
        }
        return ruleRepository.findActiveFor(companyId, supplierId, null).orElse(null);
    }

    private void rememberMatch(Long companyId, Long supplierId, String manufacturerCode, String stockCode, Long userId) {
        if (xrefRepository.findByCompanyIdAndSupplierIdAndManufacturerCode(companyId, supplierId, manufacturerCode).isPresent()) {
            return;
        }
        xrefRepository.save(PriceCodeCrossReference.builder()
                .companyId(companyId).supplierId(supplierId)
                .manufacturerCode(manufacturerCode).stockCode(stockCode)
                .source("AUTO").createdBy(userId).createdAt(LocalDateTime.now())
                .build());
    }

    /**
     * Elle eşleştirme — kullanıcı raporda (kendi CPM stok ürününe karşılık) "Eşleştir"
     * ile üretici listesinden bir satır seçtiğinde çağrılır.
     */
    @Transactional
    public PriceCalcResultResponse manualMatch(Long resultId, Long manufacturerRowId, Long batchId, Long companyId,
                                                Long userId, Map<String, BigDecimal> fxRates) {
        PriceImportBatch batch = batchRepository.findByIdAndCompanyId(batchId, companyId)
                .orElseThrow(() -> new KasappException(ErrorType.PRICE_IMPORT_BATCH_NOT_FOUND));
        PriceCalcResult result = calcResultRepository.findById(resultId)
                .orElseThrow(() -> new KasappException(ErrorType.PRICE_CALC_RESULT_NOT_FOUND));
        PriceImportRow row = importRowRepository.findByIdAndBatchId(manufacturerRowId, batchId)
                .orElseThrow(() -> new KasappException(ErrorType.PRICE_IMPORT_ROW_NOT_FOUND));
        Map<String, BigDecimal> rates = fxRates != null ? fxRates : Map.of();

        rememberMatch(companyId, batch.getSupplierId(), row.getManufacturerCode(), result.getStockCode(), userId);

        PriceRule activeRule = resolveActiveRule(companyId, batch.getSupplierId(), batch.getProductGroupId());
        List<PriceRuleStep> steps = activeRule != null
                ? stepRepository.findByRuleIdOrderByStepOrderAsc(activeRule.getId())
                : List.of();

        result.setMatched(true);
        result.setManufacturerCode(row.getManufacturerCode());
        result.setNetAlis(convertToTry(row.getListPrice(), row.getCurrencyCode(), rates));
        result.setReason(null);

        if (activeRule == null) {
            result.setReason("Bu tedarikçi için aktif kural tanımlı değil");
        } else {
            result.setRuleId(activeRule.getId());
            result.setRuleVersionNo(activeRule.getVersionNo());
            try {
                var ctx = ruleEngine.execute(steps, row.getListPrice(), rates);
                Map<SalesSlot, BigDecimal> newSlots = ctx.getSalesSlots();
                result.setNewSales1(newSlots.get(SalesSlot.SATIS1));
                result.setNewSales2(newSlots.get(SalesSlot.SATIS2));
                result.setNewSales3(newSlots.get(SalesSlot.SATIS3));
                result.setNewSales4(newSlots.get(SalesSlot.SATIS4));
                result.setUpdatedSlots(newSlots.keySet().stream().map(Enum::name).collect(Collectors.joining(",")));
                result.setChangePercent(computeChangePercent(result.getOldSales1(), newSlots.get(SalesSlot.SATIS1)));
            } catch (RuntimeException e) {
                result.setReason("Kural uygulanamadı: " + e.getMessage());
            }
        }

        calcResultRepository.save(result);
        return toResponse(result);
    }

    /** Manuel eşleştirme arama kutusu — üretici kodu veya açıklamaya göre, bu batch içinde. */
    public List<ManufacturerRowSearchResponse> searchManufacturerRows(Long batchId, String q, Long companyId) {
        batchRepository.findByIdAndCompanyId(batchId, companyId)
                .orElseThrow(() -> new KasappException(ErrorType.PRICE_IMPORT_BATCH_NOT_FOUND));
        return importRowRepository
                .search(batchId, q, PageRequest.of(0, 20))
                .stream()
                .map(r -> new ManufacturerRowSearchResponse(r.getId(), r.getManufacturerCode(), r.getDescription(), r.getListPrice()))
                .toList();
    }

    public PriceCalcRunResponse getRun(Long runId, Long companyId) {
        PriceCalcRun run = calcRunRepository.findByIdAndCompanyId(runId, companyId)
                .orElseThrow(() -> new KasappException(ErrorType.PRICE_CALC_RUN_NOT_FOUND));
        return new PriceCalcRunResponse(run.getId(), run.getBatchId(), run.getStatus(),
                run.getTotalMatched(), run.getTotalUnmatched());
    }

    public List<PriceCalcResultResponse> getResults(Long runId, Long companyId, Boolean matched) {
        calcRunRepository.findByIdAndCompanyId(runId, companyId)
                .orElseThrow(() -> new KasappException(ErrorType.PRICE_CALC_RUN_NOT_FOUND));
        List<PriceCalcResult> results = matched != null
                ? calcResultRepository.findByCalcRunIdAndMatched(runId, matched)
                : calcResultRepository.findByCalcRunId(runId);
        return results.stream().map(this::toResponse).toList();
    }

    private PriceCalcResultResponse toResponse(PriceCalcResult r) {
        return new PriceCalcResultResponse(
                r.getId(), r.getStockCode(), r.getManufacturerCode(), r.getProductName(), r.isMatched(),
                r.getRuleId(), r.getRuleVersionNo(), r.getNetAlis(),
                r.getOldSales1(), r.getOldSales2(), r.getOldSales3(), r.getOldSales4(),
                r.getNewSales1(), r.getNewSales2(), r.getNewSales3(), r.getNewSales4(),
                r.getChangePercent(), r.getUpdatedSlots(), r.getReason()
        );
    }

    private BigDecimal computeChangePercent(BigDecimal oldVal, BigDecimal newVal) {
        if (oldVal == null || newVal == null || oldVal.compareTo(BigDecimal.ZERO) == 0) return null;
        return newVal.subtract(oldVal)
                .divide(oldVal, 6, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100))
                .setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * "Alış 1" raporda/export'ta CPM'in kendi Alış Fiyatı alanına yazılıyor — CPM bunu
     * TL bekliyor. Üretici listesi satırı yabancı para birimindeyse (örn. USD) ve o para
     * birimi için hesaplamada bir kur girilmişse, gösterim/export için TL karşılığına
     * çevrilir. Kur yoksa (ör. bu para birimi hiç kullanılmıyorsa) ham değer aynen kalır —
     * hesaplama zaten APPLY_FX_RATE adımı olmadan bu satırı işlemeyecektir.
     */
    private BigDecimal convertToTry(BigDecimal amount, String currencyCode, Map<String, BigDecimal> fxRates) {
        if (amount == null || currencyCode == null || currencyCode.isBlank() || "TRY".equalsIgnoreCase(currencyCode)) {
            return amount;
        }
        BigDecimal rate = fxRates.get(currencyCode.toUpperCase());
        return rate != null ? amount.multiply(rate).setScale(2, RoundingMode.HALF_UP) : amount;
    }
}
