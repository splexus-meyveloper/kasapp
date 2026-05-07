package org.example.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.dto.request.CashUpdateRequestDto;
import org.example.dto.request.CheckUpdateRequestDto;
import org.example.dto.request.NoteUpdateRequestDto;
import org.example.dto.request.PosUpdateRequestDto;
import org.example.dto.response.ChangeRequestResponseDto;
import org.example.entity.CashTransaction;
import org.example.entity.ChangeRequest;
import org.example.entity.Note;
import org.example.entity.Check;
import org.example.entity.PosLog;
import org.example.exception.ErrorType;
import org.example.exception.KasappException;
import org.example.repository.CashTransactionRepository;
import org.example.repository.ChangeRequestRepository;
import org.example.repository.CheckRepository;
import org.example.repository.NoteRepository;
import org.example.repository.PosLogRepository;
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
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChangeRequestService {

    private final ChangeRequestRepository changeRequestRepository;
    private final CashTransactionRepository cashTransactionRepository;
    private final CheckRepository checkRepository;
    private final NoteRepository noteRepository;
    private final PosLogRepository posLogRepository;
    private final UserRepository userRepository;
    private final ObjectMapper objectMapper;
    private final AuditService auditService;

    // ── CASH güncelleme talebi ────────────────────────────────────────────
    @Transactional
    public void createCashUpdateRequest(Long cashId, CashUpdateRequestDto dto,
                                        Long userId, Long companyId) {
        CashTransaction existing = cashTransactionRepository.findById(cashId)
                .orElseThrow(() -> new KasappException(ErrorType.CASH_TRANSACTION_NOT_FOUND));

        if (!existing.getCompanyId().equals(companyId))
            throw new KasappException(ErrorType.ACCESS_DENIED);

        ChangeRequest request = buildAndSaveRequest(
                "CASH", existing.getId(), companyId, userId,
                existing, new CashUpdateRequestDto(dto.amount(), dto.description())
        );

        logAudit(AuditAction.CASH_UPDATE_REQUEST_CREATED, "CASH",
                existing.getId(), userId, companyId, request);
    }

    // ── CHECK güncelleme talebi ───────────────────────────────────────────
    @Transactional
    public void createCheckUpdateRequest(Long checkId, CheckUpdateRequestDto dto,
                                         Long userId, Long companyId) {
        Check existing = checkRepository.findById(checkId)
                .orElseThrow(() -> new KasappException(ErrorType.CHECK_NOT_FOUND));

        if (!existing.getCompanyId().equals(companyId))
            throw new KasappException(ErrorType.ACCESS_DENIED);

        ChangeRequest request = buildAndSaveRequest(
                "CHECK", existing.getId(), companyId, userId,
                existing, new CheckUpdateRequestDto(dto.checkNo(), dto.amount(),
                        dto.description(), dto.bank(), dto.dueDate())
        );

        logAudit(AuditAction.CHECK_UPDATE_REQUEST_CREATED, "CHECK",
                existing.getId(), userId, companyId, request);
    }

    // ── NOTE güncelleme talebi ────────────────────────────────────────────
    @Transactional
    public void createNoteUpdateRequest(Long noteId, NoteUpdateRequestDto dto,
                                        Long userId, Long companyId) {
        Note existing = noteRepository.findById(noteId)
                .orElseThrow(() -> new KasappException(ErrorType.NOTE_NOT_FOUND));

        if (!existing.getCompanyId().equals(companyId))
            throw new KasappException(ErrorType.ACCESS_DENIED);

        ChangeRequest request = buildAndSaveRequest(
                "NOTE", existing.getId(), companyId, userId,
                existing, new NoteUpdateRequestDto(dto.amount(), dto.description(), dto.dueDate())
        );

        logAudit(AuditAction.NOTE_UPDATE_REQUEST_CREATED, "NOTE",
                existing.getId(), userId, companyId, request);
    }

    @Transactional
    public void createPosUpdateRequest(Long posLogId, PosUpdateRequestDto dto,
                                       Long userId, Long companyId) {
        PosLog existing = posLogRepository.findById(posLogId)
                .orElseThrow(() -> new KasappException(ErrorType.POS_LOG_NOT_FOUND));

        if (!existing.getCompanyId().equals(companyId))
            throw new KasappException(ErrorType.ACCESS_DENIED);

        if (!existing.getUserId().equals(userId)) {
            throw new KasappException(ErrorType.ACCESS_DENIED);
        }

        validatePosSelection(dto.posType(), dto.terminal());

        ChangeRequest request = buildAndSaveRequest(
                "POS", existing.getId(), companyId, userId,
                existing, new PosUpdateRequestDto(dto.posType(), dto.terminal(),
                        dto.amount(), dto.description(), dto.logDate())
        );

        logAudit(AuditAction.POS_UPDATE_REQUEST_CREATED, "POS",
                existing.getId(), userId, companyId, request);
    }

    // ── Bekleyen talepler — N+1 düzeltildi ───────────────────────────────
    @Transactional(readOnly = true)
    public List<ChangeRequestResponseDto> getPendingRequests(Long companyId) {

        List<ChangeRequest> requests = changeRequestRepository
                .findByCompanyIdAndStatusOrderByRequestedAtDesc(companyId, ChangeRequestStatus.PENDING);

        if (requests.isEmpty()) return List.of();

        // ✅ Tek sorguda tüm kullanıcı adlarını çek — N+1 yok
        Set<Long> userIds = requests.stream()
                .map(ChangeRequest::getRequestedBy)
                .collect(Collectors.toSet());

        Map<Long, String> usernameMap = userRepository.findUsernamesByIds(userIds);

        return requests.stream()
                .map(req -> new ChangeRequestResponseDto(
                        req.getId(),
                        req.getEntityType(),
                        req.getEntityId(),
                        req.getActionType(),
                        req.getOldData(),
                        req.getNewData(),
                        req.getRequestedBy(),
                        usernameMap.getOrDefault(req.getRequestedBy(), "Bilinmiyor"),
                        req.getRequestedAt(),
                        req.getStatus(),
                        req.getApprovedBy(),
                        req.getApprovedAt()
                ))
                .toList();
    }

    // ── Onay ─────────────────────────────────────────────────────────────
    @Transactional
    public void approveRequest(Long requestId, Long adminId, Long companyId) {

        ChangeRequest request = changeRequestRepository.findById(requestId)
                .orElseThrow(() -> new KasappException(ErrorType.CHANGE_REQUEST_NOT_FOUND));

        if (request.getStatus() != ChangeRequestStatus.PENDING)
            throw new KasappException(ErrorType.CHANGE_REQUEST_ALREADY_PROCESSED);

        if (!request.getCompanyId().equals(companyId))
            throw new KasappException(ErrorType.CHANGE_REQUEST_ACCESS_DENIED);

        applyChange(request, companyId);

        request.setStatus(ChangeRequestStatus.APPROVED);
        request.setApprovedBy(adminId);
        request.setApprovedAt(LocalDateTime.now());
        changeRequestRepository.save(request);

        AuditAction action = resolveApproveAction(request.getEntityType());
        logAudit(action, request.getEntityType(), request.getEntityId(),
                adminId, companyId, request);
    }

    // ── Red ───────────────────────────────────────────────────────────────
    @Transactional
    public void rejectRequest(Long requestId, Long adminId, Long companyId) {

        ChangeRequest request = changeRequestRepository.findById(requestId)
                .orElseThrow(() -> new KasappException(ErrorType.CHANGE_REQUEST_NOT_FOUND));

        if (request.getStatus() != ChangeRequestStatus.PENDING)
            throw new KasappException(ErrorType.CHANGE_REQUEST_ALREADY_PROCESSED);

        if (!request.getCompanyId().equals(companyId))
            throw new KasappException(ErrorType.CHANGE_REQUEST_ACCESS_DENIED);

        request.setStatus(ChangeRequestStatus.REJECTED);
        request.setApprovedBy(adminId);
        request.setApprovedAt(LocalDateTime.now());
        changeRequestRepository.save(request);

        AuditAction action = resolveRejectAction(request.getEntityType());
        logAudit(action, request.getEntityType(), request.getEntityId(),
                adminId, request.getCompanyId(), request);
    }

    // ── Yardımcılar ───────────────────────────────────────────────────────

    private ChangeRequest buildAndSaveRequest(String entityType, Long entityId,
                                              Long companyId, Long userId,
                                              Object oldObj, Object newObj) {
        try {
            if (changeRequestRepository.existsByCompanyIdAndEntityTypeAndEntityIdAndStatus(
                    companyId, entityType, entityId, ChangeRequestStatus.PENDING)) {
                throw new KasappException(ErrorType.CHANGE_REQUEST_ALREADY_PENDING);
            }

            String oldJson = objectMapper.writeValueAsString(oldObj);
            String newJson = objectMapper.writeValueAsString(newObj);

            ChangeRequest request = ChangeRequest.builder()
                    .companyId(companyId)
                    .entityType(entityType)
                    .entityId(entityId)
                    .actionType(ChangeRequestAction.UPDATE)
                    .oldData(oldJson)
                    .newData(newJson)
                    .requestedBy(userId)
                    .requestedAt(LocalDateTime.now())
                    .status(ChangeRequestStatus.PENDING)
                    .build();

            return changeRequestRepository.save(request);

        } catch (Exception e) {
            if (e instanceof KasappException ke) {
                throw ke;
            }
            log.error("ChangeRequest oluşturulurken JSON hatası. entityType={}, entityId={}",
                    entityType, entityId, e);
            throw new KasappException(ErrorType.CHANGE_REQUEST_CREATE_FAILED);
        }
    }

    private void applyChange(ChangeRequest request, Long companyId) {
        try {
            switch (request.getEntityType()) {
                case "CASH" -> {
                    CashTransaction existing = cashTransactionRepository
                            .findById(request.getEntityId())
                            .orElseThrow(() -> new KasappException(ErrorType.CASH_TRANSACTION_NOT_FOUND));

                    if (!existing.getCompanyId().equals(companyId))
                        throw new KasappException(ErrorType.ACCESS_DENIED);

                    CashUpdateRequestDto newData = objectMapper.readValue(
                            request.getNewData(), CashUpdateRequestDto.class);
                    existing.setAmount(newData.amount());
                    existing.setDescription(newData.description());
                    cashTransactionRepository.save(existing);
                }
                case "CHECK" -> {
                    Check existing = checkRepository
                            .findById(request.getEntityId())
                            .orElseThrow(() -> new KasappException(ErrorType.CHECK_NOT_FOUND));

                    if (!existing.getCompanyId().equals(companyId))
                        throw new KasappException(ErrorType.ACCESS_DENIED);

                    CheckUpdateRequestDto newData = objectMapper.readValue(
                            request.getNewData(), CheckUpdateRequestDto.class);

                    existing.setAmount(newData.amount());
                    existing.setDescription(newData.description());
                    existing.setDueDate(newData.dueDate());

                    if (newData.checkNo() != null && !newData.checkNo().isBlank()) {
                        boolean exists = checkRepository.existsByCheckNoAndCompanyId(
                                newData.checkNo(), companyId);
                        if (exists && !existing.getCheckNo().equals(newData.checkNo()))
                            throw new RuntimeException("Bu çek numarası zaten kullanılıyor");
                        existing.setCheckNo(newData.checkNo());
                    }

                    if (newData.bank() != null && !newData.bank().isBlank()) {
                        existing.setBank(org.example.skills.enums.Banka.valueOf(
                                newData.bank().toUpperCase()));
                    }

                    checkRepository.save(existing);
                }
                case "NOTE" -> {
                    Note existing = noteRepository
                            .findById(request.getEntityId())
                            .orElseThrow(() -> new KasappException(ErrorType.NOTE_NOT_FOUND));

                    if (!existing.getCompanyId().equals(companyId))
                        throw new KasappException(ErrorType.ACCESS_DENIED);

                    NoteUpdateRequestDto newData = objectMapper.readValue(
                            request.getNewData(), NoteUpdateRequestDto.class);

                    if (newData.amount() != null) existing.setAmount(newData.amount());
                    if (newData.description() != null) existing.setDescription(newData.description());
                    existing.setDueDate(newData.dueDate());
                    noteRepository.save(existing);
                }
                case "POS" -> {
                    PosLog existing = posLogRepository
                            .findById(request.getEntityId())
                            .orElseThrow(() -> new KasappException(ErrorType.POS_LOG_NOT_FOUND));

                    if (!existing.getCompanyId().equals(companyId))
                        throw new KasappException(ErrorType.ACCESS_DENIED);

                    PosUpdateRequestDto newData = objectMapper.readValue(
                            request.getNewData(), PosUpdateRequestDto.class);

                    validatePosSelection(newData.posType(), newData.terminal());

                    existing.setPosType(newData.posType());
                    existing.setTerminal(newData.terminal());
                    existing.setAmount(newData.amount());
                    existing.setDescription(newData.description());
                    if (newData.logDate() != null) {
                        existing.setLogDate(newData.logDate());
                    }

                    posLogRepository.save(existing);
                }
                default -> throw new KasappException(ErrorType.UNSUPPORTED_ENTITY_TYPE);
            }
        } catch (KasappException e) {
            throw e; // KasappException'ları olduğu gibi geçir
        } catch (Exception e) {
            log.error("Değişiklik uygulanırken hata. requestId={}, entityType={}",
                    request.getId(), request.getEntityType(), e);
            throw new KasappException(ErrorType.CHANGE_REQUEST_APPROVE_FAILED);
        }
    }

    private void logAudit(AuditAction action, String entityType, Long entityId,
                          Long userId, Long companyId, ChangeRequest request) {
        try {
            Map<String, Object> payload = new HashMap<>();
            payload.put("requestId",  request.getId());
            payload.put("entityType", request.getEntityType());
            payload.put("entityId",   request.getEntityId());

            if (request.getOldData() != null)
                payload.put("oldData", objectMapper.readValue(request.getOldData(), Object.class));
            if (request.getNewData() != null)
                payload.put("newData", objectMapper.readValue(request.getNewData(), Object.class));

            auditService.log(action, entityType, entityId, userId, companyId, payload);

        } catch (Exception e) {
            // Audit log hatası ana işlemi durdurmamalı — sadece logla
            log.error("Audit log yazılırken hata. action={}, entityId={}", action, entityId, e);
        }
    }

    private AuditAction resolveApproveAction(String entityType) {
        return switch (entityType) {
            case "CASH"  -> AuditAction.CASH_UPDATE_REQUEST_APPROVED;
            case "CHECK" -> AuditAction.CHECK_UPDATE_REQUEST_APPROVED;
            case "NOTE"  -> AuditAction.NOTE_UPDATE_REQUEST_APPROVED;
            case "POS"   -> AuditAction.POS_UPDATE_REQUEST_APPROVED;
            default      -> throw new KasappException(ErrorType.UNSUPPORTED_ENTITY_TYPE);
        };
    }

    private void validatePosSelection(org.example.skills.enums.PosType posType,
                                      org.example.skills.enums.PosTerminal terminal) {
        if (posType == null || terminal == null) {
            throw new IllegalArgumentException("POS tipi ve terminal secilmelidir.");
        }
        if (terminal.getPosType() != posType) {
            throw new IllegalArgumentException("Secilen terminal bu POS tipine ait degil.");
        }
    }

    private AuditAction resolveRejectAction(String entityType) {
        return switch (entityType) {
            case "CASH"  -> AuditAction.CASH_UPDATE_REQUEST_REJECTED;
            case "CHECK" -> AuditAction.CHECK_UPDATE_REQUEST_REJECTED;
            case "NOTE"  -> AuditAction.NOTE_UPDATE_REQUEST_REJECTED;
            case "POS"   -> AuditAction.POS_UPDATE_REQUEST_REJECTED;
            default      -> throw new KasappException(ErrorType.UNSUPPORTED_ENTITY_TYPE);
        };
    }
}
