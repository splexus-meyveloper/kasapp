package org.example.service;

import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.example.dto.request.BankaHesapOlusturRequest;
import org.example.dto.response.BankaHesapResponse;
import org.example.dto.response.BankaIslemKoduResponse;
import org.example.dto.response.BankaIslemResponse;
import org.example.entity.BankaHesap;
import org.example.entity.BankaIslem;
import org.example.exception.ErrorType;
import org.example.exception.KasappException;
import org.example.repository.BankaHesapRepository;
import org.example.repository.BankaIslemRepository;
import org.example.skills.enums.BankaIslemKodu;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Service
@RequiredArgsConstructor
public class BankaService {

    private final BankaHesapRepository hesapRepo;
    private final BankaIslemRepository islemRepo;

    // ─── HESAP İŞLEMLERİ ──────────────────────────────────────────

    @Transactional
    public BankaHesapResponse hesapOlustur(BankaHesapOlusturRequest req,
                                           Long userId, Long companyId) {
        if (hesapRepo.existsByHesapKoduAndCompanyId(req.hesapKodu(), companyId)) {
            throw new KasappException(ErrorType.BANKA_HESAP_MEVCUT);
        }

        BankaHesap hesap = BankaHesap.builder()
                .hesapKodu(req.hesapKodu())
                .bankaAdi(req.bankaAdi().toUpperCase())
                .hesapNumarasi(req.hesapNumarasi())
                .baslangicBakiye(req.baslangicBakiye())
                .companyId(companyId)
                .olusturanId(userId)
                .olusturmaTarihi(LocalDateTime.now())
                .aktif(true)
                .build();

        hesapRepo.save(hesap);
        return toHesapResponse(hesap);
    }

    public List<BankaHesapResponse> hesaplariGetir(Long companyId) {
        return hesapRepo.findByCompanyIdAndAktifTrue(companyId)
                .stream()
                .map(this::toHesapResponse)
                .toList();
    }

    public BankaHesapResponse hesapGetir(Long hesapId, Long companyId) {
        return toHesapResponse(getHesap(hesapId, companyId));
    }

    @Transactional
    public void hesapSil(Long hesapId, Long companyId) {
        BankaHesap hesap = getHesap(hesapId, companyId);
        hesap.setAktif(false);
        hesapRepo.save(hesap);
    }

    // ─── EXCEL YÜKLEME ────────────────────────────────────────────

    @Transactional
    public int excelYukle(Long hesapId, Long companyId, Long userId,
                          MultipartFile file) throws IOException {

        BankaHesap hesap = getHesap(hesapId, companyId);
        List<BankaIslem> islemler = new ArrayList<>();

        try (Workbook wb = new XSSFWorkbook(file.getInputStream())) {
            Sheet sheet = wb.getSheetAt(0);

            for (int rowIdx = 1; rowIdx <= sheet.getLastRowNum(); rowIdx++) {
                Row row = sheet.getRow(rowIdx);
                if (row == null) continue;

                String aciklama  = strVal(row.getCell(0));
                String tutarStr  = strVal(row.getCell(1));
                String kodStr    = strVal(row.getCell(2));
                String tarihStr  = strVal(row.getCell(3));

                if (kodStr.isBlank() || tutarStr.isBlank()) continue;

                int kodInt;
                try {
                    kodInt = (int) Double.parseDouble(kodStr);
                } catch (NumberFormatException e) {
                    continue;
                }

                BankaIslemKodu islemKodu;
                try {
                    islemKodu = BankaIslemKodu.fromKod(kodInt);
                } catch (IllegalArgumentException e) {
                    continue;
                }

                BigDecimal tutar;
                try {
                    tutar = new BigDecimal(tutarStr.replace(",", "."));
                } catch (NumberFormatException e) {
                    continue;
                }

                if (tutar.compareTo(BigDecimal.ZERO) <= 0) continue;

                islemler.add(BankaIslem.builder()
                        .hesap(hesap)
                        .aciklama(aciklama)
                        .tutar(tutar)
                        .islemKodu(islemKodu)
                        .direction(islemKodu.getDirection())
                        .islemTarihi(parseDate(tarihStr))
                        .companyId(companyId)
                        .yuklemeYapanId(userId)
                        .yuklemeTarihi(LocalDateTime.now())
                        .build());
            }
        }

        if (!islemler.isEmpty()) islemRepo.saveAll(islemler);
        return islemler.size();
    }

    // ─── İŞLEM LİSTESİ ───────────────────────────────────────────

    public List<BankaIslemResponse> islemleriGetir(Long hesapId, Long companyId,
                                                   int page, int size) {
        getHesap(hesapId, companyId);
        PageRequest pr = PageRequest.of(page, size,
                Sort.by(Sort.Order.desc("yuklemeTarihi")));
        return islemRepo
                .findByHesapIdAndCompanyIdOrderByIslemTarihiDescYuklemeTarihiDesc(hesapId, companyId, pr)
                .map(this::toIslemResponse)
                .toList();
    }

    // ─── İŞLEM KODLARI ───────────────────────────────────────────

    public List<BankaIslemKoduResponse> islemKodlariGetir() {
        return Arrays.stream(BankaIslemKodu.values())
                .map(k -> new BankaIslemKoduResponse(
                        k.getKod(), k.getAciklama(), k.getDirection().name()))
                .toList();
    }

    // ─── YARDIMCI ─────────────────────────────────────────────────

    private BankaHesap getHesap(Long hesapId, Long companyId) {
        return hesapRepo.findByIdAndCompanyId(hesapId, companyId)
                .orElseThrow(() -> new KasappException(ErrorType.BANKA_HESAP_BULUNAMADI));
    }

    private BigDecimal guncelBakiye(BankaHesap hesap) {
        BigDecimal net = islemRepo.netHareket(hesap.getId());
        return hesap.getBaslangicBakiye().add(net == null ? BigDecimal.ZERO : net);
    }

    private BankaHesapResponse toHesapResponse(BankaHesap h) {
        return new BankaHesapResponse(
                h.getId(), h.getHesapKodu(), h.getBankaAdi(),
                h.getHesapNumarasi(), h.getBaslangicBakiye(),
                guncelBakiye(h), h.getOlusturmaTarihi());
    }

    private BankaIslemResponse toIslemResponse(BankaIslem i) {
        return new BankaIslemResponse(
                i.getId(), i.getAciklama(), i.getTutar(),
                i.getIslemKodu().getAciklama(), i.getIslemKodu().getKod(),
                i.getDirection().name(), i.getIslemTarihi());
    }

    private String strVal(Cell cell) {
        if (cell == null) return "";
        return switch (cell.getCellType()) {
            case STRING  -> cell.getStringCellValue().trim();
            case NUMERIC -> {
                if (DateUtil.isCellDateFormatted(cell))
                    yield cell.getLocalDateTimeCellValue().toLocalDate().toString();
                yield String.valueOf(cell.getNumericCellValue());
            }
            case BOOLEAN -> String.valueOf(cell.getBooleanCellValue());
            default      -> "";
        };
    }

    private LocalDate parseDate(String s) {
        if (s == null || s.isBlank()) return LocalDate.now();

        String[] patterns = {
                "dd-MM-yyyy",
                "dd/MM/yyyy",
                "dd.MM.yyyy",   // 12.12.2026
                "yyyy-MM-dd",
                "d-M-yyyy",
                "d/M/yyyy",
                "d.M.yyyy"      // 1.1.2026
        };

        for (String pattern : patterns) {
            try {
                return LocalDate.parse(s, java.time.format.DateTimeFormatter.ofPattern(pattern));
            } catch (Exception ignored) {}
        }

        return LocalDate.now();
    }
}