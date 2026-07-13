package org.example.service;

import lombok.RequiredArgsConstructor;
import org.example.dto.request.CreatePriceSupplierRequest;
import org.example.dto.request.UpdatePriceSupplierRequest;
import org.example.dto.response.PriceSupplierResponse;
import org.example.entity.PriceSupplier;
import org.example.exception.ErrorType;
import org.example.exception.KasappException;
import org.example.repository.PriceSupplierRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PriceSupplierService {

    private final PriceSupplierRepository supplierRepository;

    public List<PriceSupplierResponse> list(Long companyId) {
        return supplierRepository.findByCompanyIdOrderByNameAsc(companyId).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public PriceSupplierResponse create(CreatePriceSupplierRequest req, Long companyId, Long userId) {
        if (supplierRepository.existsByCodeAndCompanyId(req.code(), companyId)) {
            throw new KasappException(ErrorType.PRICE_SUPPLIER_CODE_EXISTS);
        }
        PriceSupplier supplier = PriceSupplier.builder()
                .companyId(companyId)
                .code(req.code())
                .name(req.name())
                .active(true)
                .createdBy(userId)
                .createdAt(LocalDateTime.now())
                .build();
        return toResponse(supplierRepository.save(supplier));
    }

    @Transactional
    public PriceSupplierResponse update(Long id, UpdatePriceSupplierRequest req, Long companyId) {
        PriceSupplier supplier = supplierRepository.findByIdAndCompanyId(id, companyId)
                .orElseThrow(() -> new KasappException(ErrorType.PRICE_SUPPLIER_NOT_FOUND));
        if (req.name() != null && !req.name().isBlank()) supplier.setName(req.name());
        if (req.active() != null) supplier.setActive(req.active());
        if (req.ignoredCodePrefixes() != null) {
            supplier.setIgnoredCodePrefixes(req.ignoredCodePrefixes().isBlank() ? null : req.ignoredCodePrefixes());
        }
        return toResponse(supplierRepository.save(supplier));
    }

    private PriceSupplierResponse toResponse(PriceSupplier s) {
        return new PriceSupplierResponse(s.getId(), s.getCode(), s.getName(), s.isActive(), s.getIgnoredCodePrefixes());
    }
}
