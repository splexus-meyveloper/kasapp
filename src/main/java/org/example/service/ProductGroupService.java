package org.example.service;

import lombok.RequiredArgsConstructor;
import org.example.dto.request.CreateProductGroupRequest;
import org.example.dto.request.UpdateProductGroupRequest;
import org.example.dto.response.ProductGroupResponse;
import org.example.entity.ProductGroup;
import org.example.exception.ErrorType;
import org.example.exception.KasappException;
import org.example.repository.ProductGroupRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ProductGroupService {

    private final ProductGroupRepository productGroupRepository;

    public List<ProductGroupResponse> list(Long companyId, Long supplierId) {
        List<ProductGroup> groups = supplierId != null
                ? productGroupRepository.findByCompanyIdAndSupplierIdOrderByNameAsc(companyId, supplierId)
                : productGroupRepository.findByCompanyIdOrderByNameAsc(companyId);
        return groups.stream().map(this::toResponse).toList();
    }

    @Transactional
    public ProductGroupResponse create(CreateProductGroupRequest req, Long companyId, Long userId) {
        ProductGroup group = ProductGroup.builder()
                .companyId(companyId)
                .supplierId(req.supplierId())
                .name(req.name())
                .active(true)
                .createdBy(userId)
                .createdAt(LocalDateTime.now())
                .build();
        return toResponse(productGroupRepository.save(group));
    }

    @Transactional
    public ProductGroupResponse update(Long id, UpdateProductGroupRequest req, Long companyId) {
        ProductGroup group = productGroupRepository.findByIdAndCompanyId(id, companyId)
                .orElseThrow(() -> new KasappException(ErrorType.PRODUCT_GROUP_NOT_FOUND));
        if (req.name() != null && !req.name().isBlank()) group.setName(req.name());
        if (req.active() != null) group.setActive(req.active());
        return toResponse(productGroupRepository.save(group));
    }

    private ProductGroupResponse toResponse(ProductGroup g) {
        return new ProductGroupResponse(g.getId(), g.getName(), g.getSupplierId(), g.isActive());
    }
}
