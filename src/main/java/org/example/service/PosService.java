package org.example.service;

import lombok.RequiredArgsConstructor;
import org.example.audit.Audit;
import org.example.dto.request.PosLogRequest;
import org.example.dto.response.PosTerminalGroupResponse;
import org.example.dto.response.PosLogResponse;
import org.example.dto.response.PosTerminalOption;
import org.example.entity.PosLog;
import org.example.entity.User;
import org.example.repository.PosLogRepository;
import org.example.repository.UserRepository;
import org.example.skills.enums.AuditAction;
import org.example.skills.enums.PosTerminal;
import org.example.skills.enums.PosType;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Comparator;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PosService {

    private final PosLogRepository posLogRepository;
    private final UserRepository userRepository;

    public List<PosTerminalGroupResponse> getTerminals() {
        return Arrays.stream(PosTerminal.values())
                .collect(Collectors.groupingBy(PosTerminal::getPosType))
                .entrySet()
                .stream()
                .sorted(Comparator.comparing(entry -> entry.getKey().name()))
                .map(entry -> new PosTerminalGroupResponse(
                        entry.getKey(),
                        entry.getValue()
                                .stream()
                                .map(t -> new PosTerminalOption(t, t.getLabel()))
                                .toList()
                ))
                .toList();
    }

    @Audit(action = AuditAction.POS_LOG)
    @Transactional
    public PosLogResponse logPos(PosLogRequest req, Long userId, Long companyId) {

        if (req.terminal().getPosType() != req.posType()) {
            throw new IllegalArgumentException("Secilen terminal bu POS tipine ait degil.");
        }

        PosLog log = PosLog.builder()
                .posType(req.posType())
                .terminal(req.terminal())
                .amount(req.amount())
                .description(req.description())
                .userId(userId)
                .companyId(companyId)
                .logDate(LocalDateTime.now())
                .build();

        posLogRepository.save(log);
        return toResponse(log);
    }

    public List<PosLogResponse> getLogs(Long companyId, Long userId, boolean admin, int page, int size) {
        PageRequest pageable = PageRequest.of(page, size, Sort.by(Sort.Order.desc("logDate")));
        if (!admin) {
            return posLogRepository
                    .findByCompanyIdAndUserIdOrderByLogDateDesc(companyId, userId, pageable)
                    .stream()
                    .map(this::toResponse)
                    .toList();
        }

        return posLogRepository
                .findByCompanyIdOrderByLogDateDesc(
                        companyId,
                        pageable)
                .map(this::toResponse)
                .toList();
    }

    private PosLogResponse toResponse(PosLog log) {
        String username = userRepository.findById(log.getUserId())
                .map(User::getUsername)
                .orElse("?");

        return new PosLogResponse(
                log.getId(),
                log.getPosType(),
                posTypeLabel(log.getPosType()),
                log.getTerminal(),
                log.getTerminal().getLabel(),
                log.getAmount(),
                log.getDescription(),
                username,
                log.getLogDate()
        );
    }

    private String posTypeLabel(PosType posType) {
        return switch (posType) {
            case ALTIKARDESLER_POS -> "Altikardesler POS";
            case TEDARIKCI_POS -> "Tedarikci POS";
            case YAZARKASA_POS -> "Yazarkasa POS";
        };
    }
}
