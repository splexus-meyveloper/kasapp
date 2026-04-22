package org.example.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.example.dto.request.CashUpdateRequestDto;
import org.example.dto.response.ChangeRequestResponseDto;
import org.example.entity.CashTransaction;
import org.example.entity.ChangeRequest;
import org.example.entity.User;
import org.example.repository.CashTransactionRepository;
import org.example.repository.ChangeRequestRepository;
import org.example.repository.UserRepository;
import org.example.skills.enums.AuditAction;
import org.example.skills.enums.ChangeRequestAction;
import org.example.skills.enums.ChangeRequestStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ChangeRequestService {

    private final ChangeRequestRepository changeRequestRepository;
    private final CashTransactionRepository cashTransactionRepository;
    private final UserRepository userRepository;
    private final ObjectMapper objectMapper;
    private final AuditService auditService;

    @Transactional
    public void createCashUpdateRequest(Long cashId,
                                        CashUpdateRequestDto dto,
                                        Long userId,
                                        Long companyId) {
        try {
            CashTransaction existing = cashTransactionRepository.findById(cashId)
                    .orElseThrow(() -> new RuntimeException("Kasa hareketi bulunamadı"));

            if (!existing.getCompanyId().equals(companyId)) {
                throw new RuntimeException("Bu kayıt üzerinde işlem yetkiniz yok");
            }

            String oldDataJson = objectMapper.writeValueAsString(existing);

            CashUpdateRequestDto newData = new CashUpdateRequestDto(
                    dto.amount(),
                    dto.description()
            );

            String newDataJson = objectMapper.writeValueAsString(newData);

            ChangeRequest request = ChangeRequest.builder()
                    .companyId(companyId)
                    .entityType("CASH")
                    .entityId(existing.getId())
                    .actionType(ChangeRequestAction.UPDATE)
                    .oldData(oldDataJson)
                    .newData(newDataJson)
                    .requestedBy(userId)
                    .requestedAt(LocalDateTime.now())
                    .status(ChangeRequestStatus.PENDING)
                    .build();

            request = changeRequestRepository.save(request);

            // 🔥 FIX: HashMap + null kontrolü
            Map<String, Object> payload = new HashMap<>();

            payload.put("requestId", request.getId());
            payload.put("entityType", request.getEntityType());
            payload.put("entityId", request.getEntityId());

            if (request.getOldData() != null) {
                payload.put("oldData", objectMapper.readValue(request.getOldData(), Object.class));
            }
            if (request.getNewData() != null) {
                payload.put("newData", objectMapper.readValue(request.getNewData(), Object.class));
            }

            auditService.log(
                    AuditAction.CASH_UPDATE_REQUEST_CREATED,
                    "CASH",
                    existing.getId(),
                    userId,
                    companyId,
                    payload
            );

        } catch (Exception e) {
            throw new RuntimeException("Kasa güncelleme talebi oluşturulurken hata oluştu", e);
        }
    }

    @Transactional(readOnly = true)
    public List<ChangeRequestResponseDto> getPendingRequests(Long companyId) {
        return changeRequestRepository
                .findByCompanyIdAndStatusOrderByRequestedAtDesc(
                        companyId,
                        ChangeRequestStatus.PENDING
                )
                .stream()
                .map(req -> {
                    String username = userRepository.findById(req.getRequestedBy())
                            .map(User::getUsername)
                            .orElse("Bilinmiyor");

                    return new ChangeRequestResponseDto(
                            req.getId(),
                            req.getEntityType(),
                            req.getEntityId(),
                            req.getActionType(),
                            req.getOldData(),
                            req.getNewData(),
                            req.getRequestedBy(),
                            username,
                            req.getRequestedAt(),
                            req.getStatus(),
                            req.getApprovedBy(),
                            req.getApprovedAt()
                    );
                })
                .toList();
    }

    @Transactional
    public void approveRequest(Long requestId, Long adminId, Long companyId) {
        try {
            ChangeRequest request = changeRequestRepository.findById(requestId)
                    .orElseThrow(() -> new RuntimeException("Onay kaydı bulunamadı"));

            if (request.getStatus() != ChangeRequestStatus.PENDING) {
                throw new RuntimeException("Bu talep zaten işlenmiş");
            }

            if (!request.getCompanyId().equals(companyId)) {
                throw new RuntimeException("Bu talep üzerinde işlem yetkiniz yok");
            }

            if ("CASH".equals(request.getEntityType())
                    && request.getActionType() == ChangeRequestAction.UPDATE) {

                CashTransaction existing = cashTransactionRepository.findById(request.getEntityId())
                        .orElseThrow(() -> new RuntimeException("Kasa hareketi bulunamadı"));

                if (!existing.getCompanyId().equals(companyId)) {
                    throw new RuntimeException("Bu kayıt üzerinde işlem yetkiniz yok");
                }

                CashUpdateRequestDto newData = objectMapper.readValue(
                        request.getNewData(),
                        CashUpdateRequestDto.class
                );

                existing.setAmount(newData.amount());
                existing.setDescription(newData.description());

                cashTransactionRepository.save(existing);
            }

            request.setStatus(ChangeRequestStatus.APPROVED);
            request.setApprovedBy(adminId);
            request.setApprovedAt(LocalDateTime.now());

            request = changeRequestRepository.save(request);

            // 🔥 FIX
            Map<String, Object> payload = new HashMap<>();

            payload.put("requestId", request.getId());
            payload.put("entityType", request.getEntityType());
            payload.put("entityId", request.getEntityId());
            payload.put("approvedBy", adminId);
            payload.put("approvedAt", request.getApprovedAt().toString());

            if (request.getOldData() != null) {
                payload.put("oldData", objectMapper.readValue(request.getOldData(), Object.class));
            }
            if (request.getNewData() != null) {
                payload.put("newData", objectMapper.readValue(request.getNewData(), Object.class));
            }

            auditService.log(
                    AuditAction.CASH_UPDATE_REQUEST_APPROVED,
                    "CASH",
                    request.getEntityId(),
                    adminId,
                    request.getCompanyId(),
                    payload
            );

        } catch (Exception e) {
            throw new RuntimeException("Talep onaylanırken hata oluştu", e);
        }
    }

    @Transactional
    public void rejectRequest(Long requestId, Long adminId) {
        try {
            ChangeRequest request = changeRequestRepository.findById(requestId)
                    .orElseThrow(() -> new RuntimeException("Onay kaydı bulunamadı"));

            if (request.getStatus() != ChangeRequestStatus.PENDING) {
                throw new RuntimeException("Bu talep zaten işlenmiş");
            }

            request.setStatus(ChangeRequestStatus.REJECTED);
            request.setApprovedBy(adminId);
            request.setApprovedAt(LocalDateTime.now());

            request = changeRequestRepository.save(request);

            // 🔥 FIX
            Map<String, Object> payload = new HashMap<>();

            payload.put("requestId", request.getId());
            payload.put("entityType", request.getEntityType());
            payload.put("entityId", request.getEntityId());
            payload.put("rejectedBy", adminId);
            payload.put("rejectedAt", request.getApprovedAt().toString());

            if (request.getOldData() != null) {
                payload.put("oldData", objectMapper.readValue(request.getOldData(), Object.class));
            }
            if (request.getNewData() != null) {
                payload.put("newData", objectMapper.readValue(request.getNewData(), Object.class));
            }

            auditService.log(
                    AuditAction.CASH_UPDATE_REQUEST_REJECTED,
                    "CASH",
                    request.getEntityId(),
                    adminId,
                    request.getCompanyId(),
                    payload
            );

        } catch (Exception e) {
            throw new RuntimeException("Talep reddedilirken hata oluştu", e);
        }
    }
}