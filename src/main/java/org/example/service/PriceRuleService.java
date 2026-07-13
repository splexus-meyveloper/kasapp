package org.example.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.example.dto.request.CreatePriceRuleRequest;
import org.example.dto.request.PreviewPriceRuleRequest;
import org.example.dto.request.PriceRuleStepRequest;
import org.example.dto.request.UpdatePriceRuleStepsRequest;
import org.example.dto.response.PricePreviewResponse;
import org.example.dto.response.PriceRuleAuditResponse;
import org.example.dto.response.PriceRuleResponse;
import org.example.dto.response.PriceRuleStepResponse;
import org.example.entity.PriceRule;
import org.example.entity.PriceRuleAudit;
import org.example.entity.PriceRuleStep;
import org.example.exception.ErrorType;
import org.example.exception.KasappException;
import org.example.repository.PriceRuleAuditRepository;
import org.example.repository.PriceRuleRepository;
import org.example.repository.PriceRuleStepRepository;
import org.example.repository.UserRepository;
import org.example.skills.enums.PriceRuleStatus;
import org.example.skills.enums.PriceStepType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PriceRuleService {

    private final PriceRuleRepository ruleRepository;
    private final PriceRuleStepRepository stepRepository;
    private final PriceRuleAuditRepository auditRepository;
    private final UserRepository userRepository;
    private final PriceRuleEngine ruleEngine;
    private final RealtimeEventService realtimeEventService;
    private final ObjectMapper objectMapper;

    // ── Listeleme ───────────────────────────────────────────────────────

    public List<PriceRuleResponse> list(Long companyId, Long supplierId, PriceRuleStatus status) {
        return ruleRepository.findFiltered(companyId, supplierId, status).stream()
                .map(this::toResponseWithSteps)
                .toList();
    }

    public List<PriceRuleResponse> versions(Long companyId, String ruleGroupKey) {
        return ruleRepository.findByCompanyIdAndRuleGroupKeyOrderByVersionNoDesc(companyId, ruleGroupKey).stream()
                .map(this::toResponseWithSteps)
                .toList();
    }

    public PriceRuleResponse getOne(Long id, Long companyId) {
        return toResponseWithSteps(findOwned(id, companyId));
    }

    // ── Oluşturma ───────────────────────────────────────────────────────

    @Transactional
    public PriceRuleResponse create(CreatePriceRuleRequest req, Long companyId, Long userId) {
        PriceRule rule = PriceRule.builder()
                .companyId(companyId)
                .ruleGroupKey(UUID.randomUUID().toString())
                .versionNo(1)
                .name(req.name())
                .supplierId(req.supplierId())
                .productGroupId(req.productGroupId())
                .currencyCode(req.currencyCode())
                .status(PriceRuleStatus.DRAFT)
                .createdBy(userId)
                .createdAt(LocalDateTime.now())
                .build();
        rule = ruleRepository.save(rule);

        if (req.steps() != null && !req.steps().isEmpty()) {
            saveSteps(rule.getId(), req.steps());
        }

        writeAudit(rule.getId(), userId, "CREATE", null, null, null);
        return toResponseWithSteps(rule);
    }

    /** Var olan bir kuralın (aktif ya da arşivlenmiş) adımlarını kopyalayarak yeni bir DRAFT versiyon oluşturur. */
    @Transactional
    public PriceRuleResponse newVersion(Long id, Long companyId, Long userId) {
        PriceRule source = findOwned(id, companyId);

        int nextVersion = ruleRepository
                .findByCompanyIdAndRuleGroupKeyOrderByVersionNoDesc(companyId, source.getRuleGroupKey())
                .stream().mapToInt(PriceRule::getVersionNo).max().orElse(source.getVersionNo()) + 1;

        PriceRule newRule = PriceRule.builder()
                .companyId(companyId)
                .ruleGroupKey(source.getRuleGroupKey())
                .versionNo(nextVersion)
                .name(source.getName())
                .supplierId(source.getSupplierId())
                .productGroupId(source.getProductGroupId())
                .currencyCode(source.getCurrencyCode())
                .status(PriceRuleStatus.DRAFT)
                .createdBy(userId)
                .createdAt(LocalDateTime.now())
                .build();
        newRule = ruleRepository.save(newRule);

        List<PriceRuleStep> sourceSteps = stepRepository.findByRuleIdOrderByStepOrderAsc(source.getId());
        for (PriceRuleStep s : sourceSteps) {
            stepRepository.save(PriceRuleStep.builder()
                    .ruleId(newRule.getId())
                    .stepOrder(s.getStepOrder())
                    .stepType(s.getStepType())
                    .paramNumeric(s.getParamNumeric())
                    .paramText(s.getParamText())
                    .roundTo(s.getRoundTo())
                    .targetSlot(s.getTargetSlot())
                    .sourceSlot(s.getSourceSlot())
                    .build());
        }

        writeAudit(newRule.getId(), userId, "VERSION", "versionNo",
                String.valueOf(source.getVersionNo()), String.valueOf(nextVersion));
        return toResponseWithSteps(newRule);
    }

    // ── Adım Güncelleme (sadece DRAFT) ───────────────────────────────────

    @Transactional
    public PriceRuleResponse updateSteps(Long id, UpdatePriceRuleStepsRequest req, Long companyId, Long userId) {
        PriceRule rule = findOwned(id, companyId);
        if (rule.getStatus() != PriceRuleStatus.DRAFT) {
            throw new KasappException(ErrorType.PRICE_RULE_NOT_EDITABLE);
        }
        if (req.steps() == null || req.steps().isEmpty()) {
            throw new KasappException(ErrorType.PRICE_RULE_STEPS_EMPTY);
        }
        validateStepRequests(req.steps());

        String oldStepsJson = serializeSteps(stepRepository.findByRuleIdOrderByStepOrderAsc(rule.getId()));
        stepRepository.deleteByRuleId(rule.getId());
        saveSteps(rule.getId(), req.steps());
        String newStepsJson = serializeSteps(stepRepository.findByRuleIdOrderByStepOrderAsc(rule.getId()));

        writeAudit(rule.getId(), userId, "UPDATE", "steps", oldStepsJson, newStepsJson);
        return toResponseWithSteps(rule);
    }

    // ── Aktifleştirme ────────────────────────────────────────────────────

    /** Bu versiyonu ACTIVE yapar; aynı tedarikçi/ürün grubu için o an aktif olan başka bir versiyon varsa arşivler. */
    @Transactional
    public PriceRuleResponse activate(Long id, Long companyId, Long userId) {
        PriceRule rule = findOwned(id, companyId);
        List<PriceRuleStep> steps = stepRepository.findByRuleIdOrderByStepOrderAsc(rule.getId());
        if (steps.isEmpty()) {
            throw new KasappException(ErrorType.PRICE_RULE_STEPS_EMPTY);
        }
        // "Yeni Versiyon" adımları olduğu gibi kopyalar — kullanıcı adımları hiç
        // düzenlemeden doğrudan aktifleştirirse bozuk bir adım (örn. döviz kodu
        // alanına kur değeri girilmiş olması) sessizce aktife geçmesin diye burada
        // da kontrol ediliyor, sadece adım kaydederken değil.
        validateSteps(steps);

        ruleRepository.findActiveFor(companyId, rule.getSupplierId(), rule.getProductGroupId())
                .filter(active -> !active.getId().equals(rule.getId()))
                .ifPresent(active -> {
                    active.setStatus(PriceRuleStatus.ARCHIVED);
                    ruleRepository.save(active);
                });

        rule.setStatus(PriceRuleStatus.ACTIVE);
        rule.setActivatedBy(userId);
        rule.setActivatedAt(LocalDateTime.now());
        ruleRepository.save(rule);

        writeAudit(rule.getId(), userId, "ACTIVATE", "status", "DRAFT", "ACTIVE");
        realtimeEventService.publish("FIYAT_KURAL", "RULE_ACTIVATED", companyId, rule.getId());
        return toResponseWithSteps(rule);
    }

    /**
     * Aktif bir versiyonu, yerine yeni bir versiyon aktifleştirmeden devre dışı bırakır
     * (ARCHIVED yapar). Bundan sonra bu tedarikçi/ürün grubu için hesaplama yapıldığında
     * "aktif kural tanımlı değil" durumuna düşer — fiyat hesaplanmaz, ama eşleştirme çalışmaya
     * devam eder.
     */
    @Transactional
    public PriceRuleResponse deactivate(Long id, Long companyId, Long userId) {
        PriceRule rule = findOwned(id, companyId);
        if (rule.getStatus() != PriceRuleStatus.ACTIVE) {
            throw new KasappException(ErrorType.PRICE_RULE_NOT_ACTIVE);
        }
        rule.setStatus(PriceRuleStatus.ARCHIVED);
        ruleRepository.save(rule);

        writeAudit(rule.getId(), userId, "DEACTIVATE", "status", "ACTIVE", "ARCHIVED");
        realtimeEventService.publish("FIYAT_KURAL", "RULE_DEACTIVATED", companyId, rule.getId());
        return toResponseWithSteps(rule);
    }

    // ── Silme (yalnızca hiç aktifleştirilmemiş taslaklar) ────────────────

    /**
     * Yalnızca DRAFT durumundaki bir versiyonu kalıcı olarak siler — sistem genelinde
     * hard delete kullanılmıyor (bkz. ACTIVE/ARCHIVED için versiyonlama), ama hiç
     * aktifleştirilmemiş, yanlışlıkla oluşturulmuş bir taslağın kalıcı silinmesinin
     * hiçbir geçmiş/rapor kaydını bozma riski yok.
     */
    @Transactional
    public void delete(Long id, Long companyId) {
        PriceRule rule = findOwned(id, companyId);
        if (rule.getStatus() != PriceRuleStatus.DRAFT) {
            throw new KasappException(ErrorType.PRICE_RULE_NOT_DELETABLE);
        }
        stepRepository.deleteByRuleId(rule.getId());
        auditRepository.deleteByRuleId(rule.getId());
        ruleRepository.delete(rule);
    }

    /**
     * Bir kuralın TÜM versiyonlarını (DRAFT/ACTIVE/ARCHIVED) kalıcı olarak siler —
     * kuralın kendisini "tamamen kaldırmak" isteyen kullanıcı için. Aktif bir versiyon
     * varken silinmez (önce deactivate() ile devre dışı bırakılmalı) — aksi halde
     * kullanıcı fark etmeden fiyatlandırmanın birden ortadan kalkmasına yol açar.
     * PriceCalcResult.ruleId geçmiş rapor satırlarında salt bilgi amaçlı tutulur (FK
     * değildir), bu yüzden silme geçmiş raporları bozmaz — sadece hangi kuralın
     * kullanıldığına dair referans artık çözülemez olur.
     */
    @Transactional
    public void deleteGroup(String ruleGroupKey, Long companyId) {
        List<PriceRule> versions = ruleRepository.findByCompanyIdAndRuleGroupKeyOrderByVersionNoDesc(companyId, ruleGroupKey);
        if (versions.isEmpty()) {
            throw new KasappException(ErrorType.PRICE_RULE_NOT_FOUND);
        }
        if (versions.stream().anyMatch(v -> v.getStatus() == PriceRuleStatus.ACTIVE)) {
            throw new KasappException(ErrorType.PRICE_RULE_GROUP_HAS_ACTIVE_VERSION);
        }
        for (PriceRule v : versions) {
            stepRepository.deleteByRuleId(v.getId());
            auditRepository.deleteByRuleId(v.getId());
        }
        ruleRepository.deleteAll(versions);
    }

    // ── Önizleme (DB'ye yazmaz) ──────────────────────────────────────────

    public PricePreviewResponse preview(Long id, PreviewPriceRuleRequest req, Long companyId) {
        PriceRule rule = findOwned(id, companyId);
        List<PriceRuleStep> steps = stepRepository.findByRuleIdOrderByStepOrderAsc(rule.getId());

        Map<String, BigDecimal> fxRates = req.fxRates() != null ? req.fxRates() : Map.of();
        // Adımlardaki bir veri hatası (örn. APPLY_FX_RATE'e para birimi kodu yerine kur
        // değeri girilmesi) burada da patlayıp genel "sunucu hatası" mesajına düşmesin —
        // kullanıcının doğrudan görebileceği net bir sebep olarak dönülür.
        try {
            var ctx = ruleEngine.execute(steps, req.listPrice(), fxRates);
            return new PricePreviewResponse(req.listPrice(), ctx.getSalesSlots(), null);
        } catch (RuntimeException e) {
            return new PricePreviewResponse(req.listPrice(), Map.of(), e.getMessage());
        }
    }

    // ── Değişiklik Geçmişi ────────────────────────────────────────────────

    public List<PriceRuleAuditResponse> audit(Long id, Long companyId) {
        findOwned(id, companyId); // erişim kontrolü
        List<PriceRuleAudit> logs = auditRepository.findByRuleIdOrderByChangedAtDesc(id);
        Map<Long, String> usernames = userRepository.findUsernamesByIds(
                logs.stream().map(PriceRuleAudit::getChangedBy).collect(Collectors.toSet()));

        return logs.stream().map(a -> new PriceRuleAuditResponse(
                a.getId(), a.getChangedBy(), usernames.getOrDefault(a.getChangedBy(), "Bilinmiyor"),
                a.getChangedAt(), a.getAction(), a.getFieldChanged(), a.getOldValue(), a.getNewValue()
        )).toList();
    }

    // ── Yardımcılar ───────────────────────────────────────────────────────

    private PriceRule findOwned(Long id, Long companyId) {
        return ruleRepository.findByIdAndCompanyId(id, companyId)
                .orElseThrow(() -> new KasappException(ErrorType.PRICE_RULE_NOT_FOUND));
    }

    /**
     * APPLY_FX_RATE adımının para birimi kodu alanına yanlışlıkla kurun kendisi
     * (örn. "47,5") girilmesini engeller — bu değer bir rakam içeriyorsa gerçek bir
     * para birimi kodu (USD, EUR...) olamaz. Hesaplama anında "döviz kuru bulunamadı"
     * hatasıyla sessizce tüm satış fiyatı hesaplamasını iptal etmesindense burada,
     * kaydetme/aktifleştirme anında net bir mesajla engellenir.
     */
    private void validateStepRequests(List<PriceRuleStepRequest> steps) {
        int order = 1;
        for (PriceRuleStepRequest s : steps) {
            if (s.stepType() == PriceStepType.APPLY_FX_RATE) {
                assertValidFxCode(order, s.paramText());
            }
            order++;
        }
    }

    private void validateSteps(List<PriceRuleStep> steps) {
        for (PriceRuleStep s : steps) {
            if (s.getStepType() == PriceStepType.APPLY_FX_RATE) {
                assertValidFxCode(s.getStepOrder(), s.getParamText());
            }
        }
    }

    private void assertValidFxCode(int stepOrder, String code) {
        if (code == null || code.isBlank() || code.chars().anyMatch(Character::isDigit)) {
            throw new KasappException(ErrorType.PRICE_RULE_STEP_INVALID,
                    stepOrder + ". adımdaki döviz kuru için para birimi kodu geçersiz: \"" + code +
                            "\" — buraya kurun kendisini değil, USD/EUR gibi bir kod yazmalısınız.");
        }
    }

    private void saveSteps(Long ruleId, List<PriceRuleStepRequest> steps) {
        int order = 1;
        for (PriceRuleStepRequest s : steps) {
            stepRepository.save(PriceRuleStep.builder()
                    .ruleId(ruleId)
                    .stepOrder(order++)
                    .stepType(s.stepType())
                    .paramNumeric(s.paramNumeric())
                    .paramText(s.paramText())
                    .roundTo(s.roundTo())
                    .targetSlot(s.targetSlot())
                    .sourceSlot(s.sourceSlot())
                    .build());
        }
    }

    private void writeAudit(Long ruleId, Long userId, String action, String field, String oldVal, String newVal) {
        auditRepository.save(PriceRuleAudit.builder()
                .ruleId(ruleId)
                .changedBy(userId)
                .changedAt(LocalDateTime.now())
                .action(action)
                .fieldChanged(field)
                .oldValue(oldVal)
                .newValue(newVal)
                .build());
    }

    private String serializeSteps(List<PriceRuleStep> steps) {
        try {
            return objectMapper.writeValueAsString(steps.stream().map(this::toStepResponse).toList());
        } catch (Exception e) {
            return "[]";
        }
    }

    private PriceRuleResponse toResponseWithSteps(PriceRule rule) {
        List<PriceRuleStepResponse> steps = stepRepository.findByRuleIdOrderByStepOrderAsc(rule.getId()).stream()
                .map(this::toStepResponse)
                .toList();
        return new PriceRuleResponse(
                rule.getId(), rule.getRuleGroupKey(), rule.getVersionNo(), rule.getName(),
                rule.getSupplierId(), rule.getProductGroupId(), rule.getCurrencyCode(), rule.getStatus(),
                steps, rule.getCreatedAt(), rule.getActivatedAt()
        );
    }

    private PriceRuleStepResponse toStepResponse(PriceRuleStep s) {
        return new PriceRuleStepResponse(
                s.getId(), s.getStepOrder(), s.getStepType(), s.getParamNumeric(),
                s.getParamText(), s.getRoundTo(), s.getTargetSlot(), s.getSourceSlot()
        );
    }
}
