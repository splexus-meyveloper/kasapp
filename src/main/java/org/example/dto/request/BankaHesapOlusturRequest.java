package org.example.dto.request;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

@JsonIgnoreProperties(ignoreUnknown = true)
public record BankaHesapOlusturRequest(

        @JsonAlias({"hesapKodu", "accountCode", "kod"})
        @NotBlank(message = "Hesap kodu boş olamaz")
        String hesapKodu,

        @JsonAlias({"bankaAdi", "bankAdi", "banka", "bankName"})
        @NotBlank(message = "Banka adı boş olamaz")
        String bankaAdi,

        @JsonAlias({"hesapNumarasi", "hesapNo", "accountNumber", "hesapNum"})
        @NotBlank(message = "Hesap numarası boş olamaz")
        String hesapNumarasi,

        @JsonAlias({"baslangicBakiye", "bakiye", "balance", "startBalance", "initialBalance"})
        @NotNull(message = "Başlangıç bakiyesi boş olamaz")
        BigDecimal baslangicBakiye
) {}