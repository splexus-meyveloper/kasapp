package org.example.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.example.dto.request.SavePriceImportTemplateRequest;
import org.example.dto.response.PriceImportTemplateResponse;
import org.example.entity.PriceImportTemplate;
import org.example.repository.PriceImportTemplateRepository;
import org.example.skills.enums.PriceImportTemplateType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class PriceImportTemplateService {

    private final PriceImportTemplateRepository templateRepository;
    private final ObjectMapper objectMapper;

    public List<PriceImportTemplateResponse> list(Long companyId) {
        return templateRepository.findByCompanyId(companyId).stream().map(this::toResponse).toList();
    }

    public Optional<PriceImportTemplate> findFor(Long companyId, PriceImportTemplateType type, Long supplierId) {
        return supplierId != null
                ? templateRepository.findByCompanyIdAndTemplateTypeAndSupplierId(companyId, type, supplierId)
                : templateRepository.findByCompanyIdAndTemplateTypeAndSupplierIdIsNull(companyId, type);
    }

    public Map<String, Integer> parseMappings(PriceImportTemplate template) {
        try {
            return objectMapper.readValue(template.getFieldMappingsJson(), new TypeReference<>() {});
        } catch (Exception e) {
            throw new IllegalStateException("Şablon kolon eşlemesi okunamadı: " + e.getMessage());
        }
    }

    /** Aynı tür+tedarikçi için şablon varsa günceller, yoksa oluşturur — kullanıcı her seferinde yeniden eşleştirmez. */
    @Transactional
    public PriceImportTemplateResponse save(SavePriceImportTemplateRequest req, Long companyId) {
        PriceImportTemplate template = findFor(companyId, req.templateType(), req.supplierId())
                .orElseGet(() -> PriceImportTemplate.builder()
                        .companyId(companyId)
                        .templateType(req.templateType())
                        .supplierId(req.supplierId())
                        .build());

        template.setHeaderRowIndex(req.headerRowIndex());
        try {
            template.setFieldMappingsJson(objectMapper.writeValueAsString(req.fieldMappings()));
        } catch (Exception e) {
            throw new IllegalStateException("Kolon eşlemesi kaydedilemedi: " + e.getMessage());
        }
        return toResponse(templateRepository.save(template));
    }

    private PriceImportTemplateResponse toResponse(PriceImportTemplate t) {
        return new PriceImportTemplateResponse(
                t.getId(), t.getTemplateType(), t.getSupplierId(), t.getHeaderRowIndex(), parseMappings(t));
    }
}
