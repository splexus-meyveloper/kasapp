package org.example.service;

import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DateUtil;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.example.dto.request.BankaHesapOlusturRequest;
import org.example.dto.response.BankaHesapResponse;
import org.example.dto.response.BankaIslemKoduResponse;
import org.example.dto.response.BankaIslemResponse;
import org.example.entity.BankaHesap;
import org.example.entity.BankaIslem;
import org.example.entity.BankaIslemKoduCustom;
import org.example.exception.ErrorType;
import org.example.exception.KasappException;
import org.example.repository.BankaHesapRepository;
import org.example.repository.BankaIslemKoduCustomRepository;
import org.example.repository.BankaIslemRepository;
import org.example.skills.enums.BankaIslemKodu;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
public class BankaService {

    private static final Logger log = LoggerFactory.getLogger(BankaService.class);

    private final BankaHesapRepository hesapRepo;
    private final BankaIslemRepository islemRepo;
    private final BankaIslemKoduCustomRepository customKodRepo;

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

    @Transactional
    public int excelYukle(Long hesapId, Long companyId, Long userId,
                          MultipartFile file) throws IOException {

        BankaHesap hesap = getHesap(hesapId, companyId);
        List<BankaIslem> islemler = new ArrayList<>();

        try (Workbook wb = new XSSFWorkbook(file.getInputStream())) {
            Sheet sheet = wb.getSheetAt(0);

            for (int rowIdx = 1; rowIdx <= sheet.getLastRowNum(); rowIdx++) {
                Row row = sheet.getRow(rowIdx);
                if (row == null) {
                    continue;
                }

                String tarihStr = strVal(row.getCell(0));
                String aciklama = strVal(row.getCell(1));
                String tutarStr = strVal(row.getCell(2));
                String kodStr = strVal(row.getCell(3));

                if (kodStr.isBlank() || tutarStr.isBlank()) {
                    continue;
                }

                // Önce built-in enum'da ara, bulamazsan custom kodlarda ara
                BankaIslemKodu islemKodu = BankaIslemKodu.fromKod(kodStr.trim());
                BankaIslemKodu.Direction direction = null;

                if (islemKodu != null) {
                    direction = islemKodu.getDirection();
                } else {
                    // Custom kod kontrolü
                    var customKod = customKodRepo.findByCompanyId(companyId)
                            .stream()
                            .filter(c -> c.getKod().equals(kodStr.trim()))
                            .findFirst()
                            .orElse(null);
                    if (customKod == null) continue;
                    direction = "IN".equals(customKod.getDirection())
                            ? BankaIslemKodu.Direction.IN
                            : BankaIslemKodu.Direction.OUT;
                }

                BigDecimal tutar;
                try {
                    tutar = new BigDecimal(tutarStr.replace(",", "."));
                } catch (NumberFormatException e) {
                    continue;
                }

                if (tutar.compareTo(BigDecimal.ZERO) <= 0) {
                    continue;
                }

                islemler.add(BankaIslem.builder()
                        .hesap(hesap)
                        .aciklama(aciklama)
                        .tutar(tutar)
                        .islemKodu(islemKodu)
                        .customKodStr(islemKodu == null ? kodStr.trim() : null)
                        .direction(direction)
                        .islemTarihi(parseDate(tarihStr))
                        .companyId(companyId)
                        .yuklemeYapanId(userId)
                        .yuklemeTarihi(LocalDateTime.now())
                        .build());
            }
        }

        if (!islemler.isEmpty()) {
            islemRepo.saveAll(islemler);
        }
        return islemler.size();
    }

    public List<BankaIslemResponse> islemleriGetir(Long hesapId, Long companyId,
                                                   int page, int size) {
        getHesap(hesapId, companyId);
        PageRequest pr = PageRequest.of(page, size);
        return islemRepo.findRowsByHesapIdAndCompanyId(hesapId, companyId, pr)
                .stream()
                .map(this::toIslemResponse)
                .filter(Objects::nonNull)
                .toList();
    }

    @Transactional
    public int islemleriTemizle(Long hesapId, Long companyId) {
        getHesap(hesapId, companyId);
        int silinen = Math.toIntExact(islemRepo.countByHesapIdAndCompanyId(hesapId, companyId));
        islemRepo.deleteByHesapIdAndCompanyId(hesapId, companyId);
        return silinen;
    }

    @Transactional
    public void islemSil(Long hesapId, Long islemId, Long companyId) {
        getHesap(hesapId, companyId);
        BankaIslem islem = islemRepo.findByIdAndHesapIdAndCompanyId(islemId, hesapId, companyId)
                .orElseThrow(() -> new KasappException(ErrorType.BANKA_HESAP_BULUNAMADI));
        islemRepo.delete(islem);
    }

    /** Built-in enum kodları + şirkete özel custom kodları birleştirerek döner */
    public List<BankaIslemKoduResponse> islemKodlariGetir(Long companyId) {
        List<BankaIslemKoduResponse> builtIn = Arrays.stream(BankaIslemKodu.values())
                .map(k -> new BankaIslemKoduResponse(null, k.getKod(), k.getAciklama(), k.getDirection().name()))
                .toList();

        List<BankaIslemKoduResponse> custom = customKodRepo.findByCompanyId(companyId)
                .stream()
                .map(k -> new BankaIslemKoduResponse(k.getId(), k.getKod(), k.getAciklama(), k.getDirection()))
                .toList();

        return Stream.concat(builtIn.stream(), custom.stream()).toList();
    }

    @Transactional
    public BankaIslemKoduCustom islemKoduEkle(String kod, String aciklama,
                                               String direction, Long companyId) {
        // Built-in enum ile çakışma kontrolü
        if (BankaIslemKodu.fromKod(kod.trim()) != null) {
            throw new KasappException(ErrorType.VALIDATION_ERROR);
        }
        if (customKodRepo.existsByKodAndCompanyId(kod.trim(), companyId)) {
            throw new KasappException(ErrorType.VALIDATION_ERROR);
        }
        String dir = "IN".equalsIgnoreCase(direction) ? "IN" : "OUT";
        BankaIslemKoduCustom entity = BankaIslemKoduCustom.builder()
                .kod(kod.trim())
                .aciklama(aciklama != null ? aciklama.trim() : "")
                .direction(dir)
                .companyId(companyId)
                .createdAt(LocalDateTime.now())
                .build();
        return customKodRepo.save(entity);
    }

    @Transactional
    public void islemKoduSil(Long id, Long companyId) {
        BankaIslemKoduCustom entity = customKodRepo.findByIdAndCompanyId(id, companyId)
                .orElseThrow(() -> new KasappException(ErrorType.BANKA_HESAP_BULUNAMADI));
        customKodRepo.delete(entity);
    }

    private BankaHesap getHesap(Long hesapId, Long companyId) {
        return hesapRepo.findByIdAndCompanyId(hesapId, companyId)
                .orElseThrow(() -> new KasappException(ErrorType.BANKA_HESAP_BULUNAMADI));
    }

    private BigDecimal guncelBakiye(BankaHesap hesap) {
        BigDecimal net = islemRepo.netHareket(hesap.getId());
        return hesap.getBaslangicBakiye().add(net == null ? BigDecimal.ZERO : net);
    }

    private BankaHesapResponse toHesapResponse(BankaHesap hesap) {
        return new BankaHesapResponse(
                hesap.getId(),
                hesap.getHesapKodu(),
                hesap.getBankaAdi(),
                hesap.getHesapNumarasi(),
                hesap.getBaslangicBakiye(),
                guncelBakiye(hesap),
                hesap.getOlusturmaTarihi());
    }

    private BankaIslemResponse toIslemResponse(BankaIslemRepository.BankaIslemRow row) {
        BankaIslemKodu islemKodu = parseIslemKodu(row.getIslemKoduRaw());

        // Enum'da bulunamadı — custom kod (direction DB'den okunur)
        if (islemKodu == null) {
            String rawDir       = row.getDirectionRaw();
            String customKodStr = row.getCustomKodStrRaw();

            if (rawDir == null || rawDir.isBlank()) {
                log.warn("Banka islem row skipped — no direction. id={}", row.getId());
                return null;
            }
            String dir = rawDir.trim().toUpperCase();
            if (!dir.equals("IN") && !dir.equals("OUT")) {
                log.warn("Banka islem row skipped — invalid direction. id={}, dir={}", row.getId(), dir);
                return null;
            }

            String displayKod = (customKodStr != null && !customKodStr.isBlank()) ? customKodStr : "—";
            return new BankaIslemResponse(
                    row.getId(),
                    row.getAciklama(),
                    row.getTutar(),
                    displayKod,
                    displayKod,
                    dir,
                    parseStoredDate(row.getIslemTarihiRaw()));
        }

        BankaIslemKodu.Direction direction = parseDirection(row.getDirectionRaw(), islemKodu);
        return new BankaIslemResponse(
                row.getId(),
                row.getAciklama(),
                row.getTutar(),
                islemKodu.getAciklama(),
                islemKodu.getKod(),
                direction.name(),
                parseStoredDate(row.getIslemTarihiRaw()));
    }

    private String strVal(Cell cell) {
        if (cell == null) {
            return "";
        }

        return switch (cell.getCellType()) {
            case STRING -> cell.getStringCellValue().trim();
            case NUMERIC -> {
                if (DateUtil.isCellDateFormatted(cell)) {
                    yield cell.getLocalDateTimeCellValue().toLocalDate().toString();
                }
                yield String.valueOf(cell.getNumericCellValue());
            }
            case BOOLEAN -> String.valueOf(cell.getBooleanCellValue());
            default -> "";
        };
    }

    private LocalDate parseDate(String value) {
        if (value == null || value.isBlank()) {
            return LocalDate.now();
        }

        String[] patterns = {
                "dd-MM-yyyy",
                "dd/MM/yyyy",
                "dd.MM.yyyy",
                "yyyy-MM-dd",
                "d-M-yyyy",
                "d/M/yyyy",
                "d.M.yyyy"
        };

        for (String pattern : patterns) {
            try {
                return LocalDate.parse(value, DateTimeFormatter.ofPattern(pattern));
            } catch (Exception ignored) {
            }
        }

        return LocalDate.now();
    }

    private LocalDate parseStoredDate(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }

        try {
            return LocalDate.parse(value);
        } catch (Exception ignored) {
            return parseDate(value);
        }
    }

    private BankaIslemKodu parseIslemKodu(String rawKod) {
        if (rawKod == null || rawKod.isBlank()) {
            return null;
        }

        try {
            return BankaIslemKodu.valueOf(rawKod.trim());
        } catch (IllegalArgumentException ignored) {
            return BankaIslemKodu.fromKod(rawKod);
        }
    }

    private BankaIslemKodu.Direction parseDirection(String rawDirection, BankaIslemKodu islemKodu) {
        if (rawDirection != null && !rawDirection.isBlank()) {
            try {
                return BankaIslemKodu.Direction.valueOf(rawDirection.trim());
            } catch (IllegalArgumentException ignored) {
            }
        }

        return islemKodu.getDirection();
    }
}
