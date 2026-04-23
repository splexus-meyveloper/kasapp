package org.example.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.example.dto.request.CashUpdateRequestDto;
import org.example.dto.request.CheckUpdateRequestDto;
import org.example.dto.request.NoteUpdateRequestDto;
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
import org.example.entity.Check;
import org.example.entity.Note;
import org.example.repository.CheckRepository;
import org.example.repository.NoteRepository;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ChangeRequestService {

    private final ChangeRequestRepository changeRequestRepository;
    private final CashTransactionRepository cashTransactionRepository;
    private final CheckRepository checkRepository;
    private final NoteRepository noteRepository;
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

            if ("CHECK".equals(request.getEntityType())
                    && request.getActionType() == ChangeRequestAction.UPDATE) {

                Check existing = checkRepository.findById(request.getEntityId())
                        .orElseThrow(() -> new RuntimeException("Çek bulunamadı"));

                if (!existing.getCompanyId().equals(companyId)) {
                    throw new RuntimeException("Bu kayıt üzerinde işlem yetkiniz yok");
                }

                CheckUpdateRequestDto newData = objectMapper.readValue(
                        request.getNewData(),
                        CheckUpdateRequestDto.class
                );

                existing.setAmount(newData.amount());
                existing.setDescription(newData.description());
                existing.setDueDate(newData.dueDate());

                if (newData.checkNo() != null && !newData.checkNo().isBlank()) {

                    boolean exists = checkRepository.existsByCheckNoAndCompanyId(
                            newData.checkNo(),
                            companyId
                    );

                    if (exists && !existing.getCheckNo().equals(newData.checkNo())) {
                        throw new RuntimeException("Bu çek numarası zaten kullanılıyor");
                    }

                    existing.setCheckNo(newData.checkNo());
                }

                if (newData.bank() != null && !newData.bank().isBlank()) {
                    try {
                        existing.setBank(org.example.skills.enums.Banka.valueOf(newData.bank().toUpperCase()));
                    } catch (Exception e) {
                        throw new RuntimeException("Geçersiz banka değeri: " + newData.bank());
                    }
                }

                checkRepository.save(existing);
            }

            if ("NOTE".equals(request.getEntityType())
                    && request.getActionType() == ChangeRequestAction.UPDATE) {

                Note existing = noteRepository.findById(request.getEntityId())
                        .orElseThrow(() -> new RuntimeException("Senet bulunamadı"));

                if (!existing.getCompanyId().equals(companyId)) {
                    throw new RuntimeException("Bu kayıt üzerinde işlem yetkiniz yok");
                }

                NoteUpdateRequestDto newData = objectMapper.readValue(
                        request.getNewData(),
                        NoteUpdateRequestDto.class
                );

                if (newData.amount() != null) {
                    existing.setAmount(newData.amount());
                }
                if (newData.description() != null) {
                    existing.setDescription(newData.description());
                }
                existing.setDueDate(newData.dueDate());

                noteRepository.save(existing);
            }

            request.setStatus(ChangeRequestStatus.APPROVED);
            request.setApprovedBy(adminId);
            request.setApprovedAt(LocalDateTime.now());

            request = changeRequestRepository.save(request);

            AuditAction approveAction = switch (request.getEntityType()) {
                case "CASH" -> AuditAction.CASH_UPDATE_REQUEST_APPROVED;
                case "CHECK" -> AuditAction.CHECK_UPDATE_REQUEST_APPROVED;
                case "NOTE" -> AuditAction.NOTE_UPDATE_REQUEST_APPROVED;
                default -> throw new RuntimeException("Desteklenmeyen entity type: " + request.getEntityType());
            };

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
                    approveAction,
                    request.getEntityType(),
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

            AuditAction rejectAction = switch (request.getEntityType()) {
                case "CASH" -> AuditAction.CASH_UPDATE_REQUEST_REJECTED;
                case "CHECK" -> AuditAction.CHECK_UPDATE_REQUEST_REJECTED;
                case "NOTE" -> AuditAction.NOTE_UPDATE_REQUEST_REJECTED;
                default -> throw new RuntimeException("Desteklenmeyen entity type: " + request.getEntityType());
            };

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
                    rejectAction,
                    request.getEntityType(),
                    request.getEntityId(),
                    adminId,
                    request.getCompanyId(),
                    payload
            );

        } catch (Exception e) {
            throw new RuntimeException("Talep reddedilirken hata oluştu", e);
        }
    }

    @Transactional
    public void createCheckUpdateRequest(Long checkId,
                                         CheckUpdateRequestDto dto,
                                         Long userId,
                                         Long companyId) {
        try {
            Check existing = checkRepository.findById(checkId)
                    .orElseThrow(() -> new RuntimeException("Çek bulunamadı"));

            if (!existing.getCompanyId().equals(companyId)) {
                throw new RuntimeException("Bu kayıt üzerinde işlem yetkiniz yok");
            }

            String oldDataJson = objectMapper.writeValueAsString(existing);

            CheckUpdateRequestDto newData = new CheckUpdateRequestDto(
                    dto.checkNo(),
                    dto.amount(),
                    dto.description(),
                    dto.bank(),
                    dto.dueDate()
            );

            String newDataJson = objectMapper.writeValueAsString(newData);

            ChangeRequest request = ChangeRequest.builder()
                    .companyId(companyId)
                    .entityType("CHECK")
                    .entityId(existing.getId())
                    .actionType(ChangeRequestAction.UPDATE)
                    .oldData(oldDataJson)
                    .newData(newDataJson)
                    .requestedBy(userId)
                    .requestedAt(LocalDateTime.now())
                    .status(ChangeRequestStatus.PENDING)
                    .build();

            request = changeRequestRepository.save(request);

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
                    AuditAction.CHECK_UPDATE_REQUEST_CREATED,
                    "CHECK",
                    existing.getId(),
                    userId,
                    companyId,
                    payload
            );

        } catch (Exception e) {
            throw new RuntimeException("Çek güncelleme talebi oluşturulurken hata oluştu", e);
        }
    }

    @Transactional
    public void createNoteUpdateRequest(Long noteId,
                                        NoteUpdateRequestDto dto,
                                        Long userId,
                                        Long companyId) {
        try {
            Note existing = noteRepository.findById(noteId)
                    .orElseThrow(() -> new RuntimeException("Senet bulunamadı"));

            if (!existing.getCompanyId().equals(companyId)) {
                throw new RuntimeException("Bu kayıt üzerinde işlem yetkiniz yok");
            }

            String oldDataJson = objectMapper.writeValueAsString(existing);

            NoteUpdateRequestDto newData = new NoteUpdateRequestDto(
                    dto.amount(),
                    dto.description(),
                    dto.dueDate()
            );

            String newDataJson = objectMapper.writeValueAsString(newData);

            ChangeRequest request = ChangeRequest.builder()
                    .companyId(companyId)
                    .entityType("NOTE")
                    .entityId(existing.getId())
                    .actionType(ChangeRequestAction.UPDATE)
                    .oldData(oldDataJson)
                    .newData(newDataJson)
                    .requestedBy(userId)
                    .requestedAt(LocalDateTime.now())
                    .status(ChangeRequestStatus.PENDING)
                    .build();

            request = changeRequestRepository.save(request);

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
                    AuditAction.NOTE_UPDATE_REQUEST_CREATED,
                    "NOTE",
                    existing.getId(),
                    userId,
                    companyId,
                    payload
            );

        } catch (Exception e) {
            throw new RuntimeException("Senet güncelleme talebi oluşturulurken hata oluştu", e);
        }
    }
}