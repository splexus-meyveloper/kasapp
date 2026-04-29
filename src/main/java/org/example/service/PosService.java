package org.example.service;

import lombok.RequiredArgsConstructor;
import org.example.dto.request.PosLogRequest;
import org.example.dto.response.PosLogResponse;
import org.example.dto.response.PosTerminalInfo;
import org.example.entity.PosLog;
import org.example.entity.User;
import org.example.repository.PosLogRepository;
import org.example.repository.UserRepository;
import org.example.skills.enums.PosTerminal;
import org.example.skills.enums.PosType;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PosService {

    private final PosLogRepository posLogRepository;
    private final UserRepository   userRepository;

    // ── Terminal listelerini döndür (frontend için) ────────────────
    public Map<PosType, List<PosTerminalInfo>> getTerminals() {
        return Arrays.stream(PosTerminal.values())
                .map(t -> new PosTerminalInfo(t, t.getLabel(), t.getPosType()))
                .collect(Collectors.groupingBy(PosTerminalInfo::posType));
    }

    // ── POS kaydı oluştur ─────────────────────────────────────────
    @Transactional
    public PosLogResponse logPos(PosLogRequest req, Long userId, Long companyId) {

        // Terminal, seçilen posType ile uyuşuyor mu?
        if (req.terminal().getPosType() != req.posType()) {
            throw new IllegalArgumentException(
                    "Seçilen terminal bu POS tipine ait değil.");
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

    // ── Log listesi (sadece admin) ────────────────────────────────
    public List<PosLogResponse> getLogs(Long companyId, int page, int size) {
        return posLogRepository
                .findByCompanyIdOrderByLogDateDesc(
                        companyId,
                        PageRequest.of(page, size, Sort.by(Sort.Order.desc("logDate"))))
                .map(this::toResponse)
                .toList();
    }

    // ── Yardımcı ─────────────────────────────────────────────────
    private PosLogResponse toResponse(PosLog log) {
        String username = userRepository.findById(log.getUserId())
                .map(User::getUsername)
                .orElse("?");

        return new PosLogResponse(
                log.getId(),
                log.getPosType(),
                log.getPosType() == PosType.ALTIKARDESLER_POS ? "Altıkardeşler POS" : "Tedarikçi POS",
                log.getTerminal(),
                log.getTerminal().getLabel(),
                log.getAmount(),
                log.getDescription(),
                username,
                log.getLogDate()
        );
    }
}