package org.example.dto.request;

import jakarta.validation.constraints.*;

public record RegisterRequest(

        @NotBlank(message = "Şirket adı boş olamaz")
        @Size(min = 2, max = 100, message = "Şirket adı 2-100 karakter arasında olmalıdır")
        String companyName,

        @NotBlank(message = "Ad boş olamaz")
        @Size(min = 2, max = 50, message = "Ad 2-50 karakter arasında olmalıdır")
        String name,

        @NotBlank(message = "Soyad boş olamaz")
        @Size(min = 2, max = 50, message = "Soyad 2-50 karakter arasında olmalıdır")
        String surname,

        @NotBlank(message = "Email boş olamaz")
        @Email(message = "Geçerli bir email adresi giriniz")
        @Size(max = 150, message = "Email 150 karakterden uzun olamaz")
        String email,

        @NotBlank(message = "Telefon boş olamaz")
        @Pattern(
                regexp = "^[0-9+\\-\\s]{7,20}$",
                message = "Geçerli bir telefon numarası giriniz"
        )
        String phone,

        @NotBlank(message = "Kullanıcı adı boş olamaz")
        @Size(min = 3, max = 50, message = "Kullanıcı adı 3-50 karakter arasında olmalıdır")
        @Pattern(
                regexp = "^[a-zA-Z0-9_\\.]+$",
                message = "Kullanıcı adı sadece harf, rakam, nokta ve alt çizgi içerebilir"
        )
        String username,

        // Min 8 karakter, en az 1 büyük, 1 küçük, 1 rakam, 1 noktalama işareti
        @NotBlank(message = "Şifre boş olamaz")
        @Size(min = 8, max = 100, message = "Şifre en az 8 karakter olmalıdır")
        @Pattern(
                regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[!@#$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>\\/?]).{8,}$",
                message = "Şifre en az 1 büyük harf, 1 küçük harf, 1 rakam ve 1 noktalama işareti içermelidir"
        )
        String password

) {}