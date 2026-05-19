package org.example.repository;

import org.example.entity.Check;
import org.example.skills.enums.Banka;
import org.example.skills.enums.CheckStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;


public interface CheckRepository extends JpaRepository<Check,Long> {
    boolean existsByCheckNoAndCompanyId(
            String checkNo,
            Long companyId
    );

    List<Check> findByCompanyId(Long companyId);

    Optional<Check> findByCheckNoAndBankAndDueDateAndCompanyId(
            String checkNo,
            Banka bank,
            LocalDate dueDate,
            Long companyId
    );

    Optional<Check> findByIdAndCompanyId(Long id, Long companyId);

    @Query("""
        SELECT COALESCE(SUM(c.amount),0)
        FROM Check c
        WHERE c.status='PORTFOYDE'
        AND c.companyId=:companyId
""")
    BigDecimal getPortfolioTotal(Long companyId);

    List<Check> findByStatusAndCompanyId(
            CheckStatus status,
            Long companyId
    );

    // Tüm aktif (portföyde + sorunlu) çekler — bakiye hesabına dahil edilmez
    @Query("""
        SELECT c FROM Check c
        WHERE c.companyId = :companyId
          AND c.status IN ('PORTFOYDE', 'KARSILISIZ', 'PROTESTOLU')
        ORDER BY c.createdAt DESC
    """)
    List<Check> findActiveByCompanyId(Long companyId);

    // Belirli statülere göre listele
    @Query("""
        SELECT c FROM Check c
        WHERE c.companyId = :companyId
          AND c.status IN :statuses
        ORDER BY c.createdAt DESC
    """)
    List<Check> findByStatusesAndCompanyId(
            Long companyId,
            java.util.Collection<CheckStatus> statuses
    );

    // Tüm çekler (filtreli liste için) — sayfalama ile
    @Query("""
        SELECT c FROM Check c
        WHERE c.companyId = :companyId
        ORDER BY c.createdAt DESC
    """)
    Page<Check> findAllByCompanyIdOrderByCreatedAtDesc(Long companyId, Pageable pageable);

    // Merkez admin — tüm şubelerin çekleri
    @Query("SELECT c FROM Check c ORDER BY c.createdAt DESC")
    Page<Check> findAllOrderByCreatedAtDesc(Pageable pageable);

    // Vadesi yaklaşan çekler (x gün içinde)
    @Query("""
    SELECT c FROM Check c
    WHERE c.companyId = :companyId
      AND c.dueDate >= :today
      AND c.dueDate <= :limitDate
    ORDER BY c.dueDate ASC
""")
    List<Check> findUpcomingDue(Long companyId, LocalDate today, LocalDate limitDate);

    // Tarih aralığında çek toplamı
    @Query("""
    SELECT COALESCE(SUM(c.amount), 0)
    FROM Check c
    WHERE c.companyId = :companyId
      AND c.dueDate >= :start
      AND c.dueDate <= :end
""")
    BigDecimal sumByDateRange(Long companyId, LocalDate start, LocalDate end);

    @Query("""
        SELECT COALESCE(SUM(c.amount), 0)
        FROM Check c
        WHERE c.companyId = :companyId
          AND c.createdBy = :userId
          AND c.createdAt >= :start
          AND c.createdAt < :end
    """)
    BigDecimal sumTodayByUser(Long companyId, Long userId,
                              LocalDateTime start, LocalDateTime end);
}