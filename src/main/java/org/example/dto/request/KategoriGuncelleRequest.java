package org.example.dto.request;

/** Banka işlemi için elle kategori (kod) değiştirme isteği. Boş kod = kategorisiz. */
public record KategoriGuncelleRequest(String kod) {
}
