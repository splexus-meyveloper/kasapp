package org.example.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.dto.request.BankaHesapOlusturRequest;
import org.example.dto.request.KategoriGuncelleRequest;
import org.example.dto.response.BankaHesapResponse;
import org.example.dto.response.BankaIslemKoduResponse;
import org.example.dto.response.BankaIslemResponse;
import org.example.dto.response.BankaKategoriOzet;
import org.example.entity.BankaIslemKoduCustom;
import org.example.security.CustomUserDetails;
import org.example.service.BankaService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/banka")
@RequiredArgsConstructor
public class BankaController {

    private final BankaService service;

    @PreAuthorize("hasAuthority('BANKA') or hasRole('ADMIN')")
    @PostMapping("/hesaplar")
    public ResponseEntity<BankaHesapResponse> hesapOlustur(
            @Valid @RequestBody BankaHesapOlusturRequest req,
            @AuthenticationPrincipal CustomUserDetails user) {
        return ResponseEntity.ok(
                service.hesapOlustur(req, user.getId(), user.getCompanyId()));
    }

    @PreAuthorize("hasAuthority('BANKA') or hasRole('ADMIN')")
    @GetMapping("/hesaplar")
    public List<BankaHesapResponse> hesaplariGetir(
            @AuthenticationPrincipal CustomUserDetails user) {
        return service.hesaplariGetir(user.getCompanyId());
    }

    @PreAuthorize("hasAuthority('BANKA') or hasRole('ADMIN')")
    @GetMapping("/hesaplar/{id}")
    public BankaHesapResponse hesapGetir(
            @PathVariable Long id,
            @AuthenticationPrincipal CustomUserDetails user) {
        return service.hesapGetir(id, user.getCompanyId());
    }

    /**
     * BANKA yetkisi olan herkes değil — sadece DOGRUDAN_ISLEM/ADMIN doğrudan silebilir.
     * BANKA yetkisi olup DOGRUDAN_ISLEM'i olmayan kullanıcılar artık bu uç noktayı değil,
     * /api/change-requests/delete/BANKA_HESAP/{id} üzerinden onaya giden bir talep açar
     * (diğer tüm modüllerle — kasa/çek/senet/masraf — aynı davranış).
     */
    @PreAuthorize("hasAuthority('DOGRUDAN_ISLEM') or hasRole('ADMIN')")
    @DeleteMapping("/hesaplar/{id}")
    public ResponseEntity<Void> hesapSil(
            @PathVariable Long id,
            @AuthenticationPrincipal CustomUserDetails user) {
        service.hesapSil(id, user.getCompanyId());
        return ResponseEntity.ok().build();
    }

    @PreAuthorize("hasAuthority('BANKA') or hasRole('ADMIN')")
    @PostMapping("/hesaplar/{id}/excel")
    public ResponseEntity<String> excelYukle(
            @PathVariable Long id,
            @RequestParam("file") MultipartFile file,
            @AuthenticationPrincipal CustomUserDetails user) throws IOException {
        int adet = service.excelYukle(id, user.getCompanyId(), user.getId(), file);
        return ResponseEntity.ok(adet + " islem basariyla yuklendi.");
    }

    /** Ham banka ekstresi (bankadan indirilen excel) — yön tutarın işaretinden belirlenir. */
    @PreAuthorize("hasAuthority('BANKA') or hasRole('ADMIN')")
    @PostMapping("/hesaplar/{id}/excel-ekstre")
    public ResponseEntity<String> excelEkstreYukle(
            @PathVariable Long id,
            @RequestParam("file") MultipartFile file,
            @AuthenticationPrincipal CustomUserDetails user) throws IOException {
        String sonuc = service.importBankaEkstresi(id, user.getCompanyId(), user.getId(), file);
        return ResponseEntity.ok(sonuc);
    }

    @PreAuthorize("hasAuthority('DOGRUDAN_ISLEM') or hasRole('ADMIN')")
    @DeleteMapping("/hesaplar/{id}/islemler")
    public ResponseEntity<String> islemleriTemizle(
            @PathVariable Long id,
            @AuthenticationPrincipal CustomUserDetails user) {
        int silinen = service.islemleriTemizle(id, user.getCompanyId());
        return ResponseEntity.ok(silinen + " islem silindi.");
    }

    @PreAuthorize("hasAuthority('DOGRUDAN_ISLEM') or hasRole('ADMIN')")
    @DeleteMapping("/hesaplar/{id}/islemler/{islemId}")
    public ResponseEntity<Void> islemSil(
            @PathVariable Long id,
            @PathVariable Long islemId,
            @AuthenticationPrincipal CustomUserDetails user) {
        service.islemSil(id, islemId, user.getCompanyId());
        return ResponseEntity.noContent().build();
    }

    /** Bir banka işleminin kategorisini (kodunu) elle değiştir. Yön değişmez. */
    @PreAuthorize("hasAuthority('BANKA') or hasRole('ADMIN')")
    @PutMapping("/hesaplar/{id}/islemler/{islemId}/kategori")
    public ResponseEntity<Void> islemKategoriGuncelle(
            @PathVariable Long id,
            @PathVariable Long islemId,
            @RequestBody KategoriGuncelleRequest req,
            @AuthenticationPrincipal CustomUserDetails user) {
        service.updateIslemKategori(id, islemId, user.getCompanyId(), req.kod());
        return ResponseEntity.noContent().build();
    }

    /** Kategori bazında giriş/çıkış/net özeti (opsiyonel start/end = yyyy-MM-dd). */
    @PreAuthorize("hasAuthority('BANKA') or hasRole('ADMIN')")
    @GetMapping("/hesaplar/{id}/kategori-ozet")
    public List<BankaKategoriOzet> kategoriOzet(
            @PathVariable Long id,
            @RequestParam(required = false) String start,
            @RequestParam(required = false) String end,
            @AuthenticationPrincipal CustomUserDetails user) {
        return service.kategoriOzet(id, user.getCompanyId(), start, end);
    }

    @PreAuthorize("hasAuthority('BANKA') or hasRole('ADMIN')")
    @GetMapping("/hesaplar/{id}/islemler")
    public List<BankaIslemResponse> islemleriGetir(
            @PathVariable Long id,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size,
            @AuthenticationPrincipal CustomUserDetails user) {
        return service.islemleriGetir(id, user.getCompanyId(), page, size);
    }

    @PreAuthorize("hasAuthority('BANKA') or hasRole('ADMIN')")
    @GetMapping("/islem-kodlari")
    public List<BankaIslemKoduResponse> islemKodlariGetir(
            @AuthenticationPrincipal CustomUserDetails user) {
        return service.islemKodlariGetir(user.getCompanyId());
    }

    @PreAuthorize("hasAuthority('BANKA') or hasRole('ADMIN')")
    @PostMapping("/islem-kodlari")
    public ResponseEntity<BankaIslemKoduCustom> islemKoduEkle(
            @RequestBody Map<String, String> body,
            @AuthenticationPrincipal CustomUserDetails user) {
        String kod       = body.getOrDefault("kod", "").trim();
        String aciklama  = body.getOrDefault("aciklama", "").trim();
        String direction = body.getOrDefault("direction", "OUT").trim();
        if (kod.isEmpty()) return ResponseEntity.badRequest().build();
        return ResponseEntity.ok(service.islemKoduEkle(kod, aciklama, direction, user.getCompanyId()));
    }

    @PreAuthorize("hasAuthority('BANKA') or hasRole('ADMIN')")
    @DeleteMapping("/islem-kodlari/{id}")
    public ResponseEntity<Void> islemKoduSil(
            @PathVariable Long id,
            @AuthenticationPrincipal CustomUserDetails user) {
        service.islemKoduSil(id, user.getCompanyId());
        return ResponseEntity.ok().build();
    }
}
