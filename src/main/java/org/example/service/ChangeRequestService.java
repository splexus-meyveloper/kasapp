package org.example.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.example.dto.request.CashUpdateRequestDto;
import org.example.dto.response.ChangeRequestResponseDto;
import org.example.entity.CashTransaction;
import org.example.entity.ChangeRequest;
import org.example.repository.CashTransactionRepository;
import org.example.repository.ChangeRequestRepository;
import org.example.skills.enums.ChangeRequestAction;
import org.example.skills.enums.ChangeRequestStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ChangeRequestService {

    private final ChangeRequestRepository changeRequestRepository;
    private final CashTransactionRepository cashTransactionRepository;
    private final ObjectMapper objectMapper;

    @Transactional
    public void createCashUpdateRequest(Long cashId,
                                        CashUpdateRequestDto dto,
                                        Long userId,
                                        Long companyId) throws Exception {

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
                .entityType("CASH")
                .entityId(existing.getId())
                .actionType(ChangeRequestAction.UPDATE)
                .oldData(oldDataJson)
                .newData(newDataJson)
                .requestedBy(userId)
                .requestedAt(LocalDateTime.now())
                .status(ChangeRequestStatus.PENDING)
                .build();

        changeRequestRepository.save(request);
    }

    @Transactional(readOnly = true)
    public List<ChangeRequestResponseDto> getPendingRequests() {
        return changeRequestRepository.findByStatusOrderByRequestedAtDesc(ChangeRequestStatus.PENDING)
                .stream()
                .map(req -> new ChangeRequestResponseDto(
                        req.getId(),
                        req.getEntityType(),
                        req.getEntityId(),
                        req.getActionType(),
                        req.getOldData(),
                        req.getNewData(),
                        req.getRequestedBy(),
                        req.getRequestedAt(),
                        req.getStatus(),
                        req.getApprovedBy(),
                        req.getApprovedAt()
                ))
                .toList();
    }

    @Transactional
    public void approveRequest(Long requestId, Long adminId, Long companyId) throws Exception {

        ChangeRequest request = changeRequestRepository.findById(requestId)
                .orElseThrow(() -> new RuntimeException("Onay kaydı bulunamadı"));

        if (request.getStatus() != ChangeRequestStatus.PENDING) {
            throw new RuntimeException("Bu talep zaten işlenmiş");
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

        changeRequestRepository.save(request);
    }

    @Transactional
    public void rejectRequest(Long requestId, Long adminId) {

        ChangeRequest request = changeRequestRepository.findById(requestId)
                .orElseThrow(() -> new RuntimeException("Onay kaydı bulunamadı"));

        if (request.getStatus() != ChangeRequestStatus.PENDING) {
            throw new RuntimeException("Bu talep zaten işlenmiş");
        }

        request.setStatus(ChangeRequestStatus.REJECTED);
        request.setApprovedBy(adminId);
        request.setApprovedAt(LocalDateTime.now());

        changeRequestRepository.save(request);
    }
}