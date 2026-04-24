package org.example.dto.request;

import jakarta.validation.constraints.*;
import org.example.skills.enums.ERole;

public record AdminCreateUserRequest(

        @NotBlank(message = "Kullanıcı adı boş olamaz")
        @Size(min = 3, max = 50, message = "Kullanıcı adı 3-50 karakter arasında olmalıdır")
        @Pattern(
                regexp = "^[a-zA-Z0-9_\\.]+$",
                message = "Kullanıcı adı sadece harf, rakam, nokta ve alt çizgi içerebilir"
        )
        String username,

        // Alt kullanıcı şifresi: tam olarak 4 rakam
        @NotBlank(message = "Şifre boş olamaz")
        @Pattern(
                regexp = "^[0-9]{4}$",
                message = "Alt kullanıcı şifresi tam olarak 4 rakamdan oluşmalıdır"
        )
        String password,

        @Size(max = 50, message = "Ad 50 karakterden uzun olamaz")
        String name,

        @Size(max = 50, message = "Soyad 50 karakterden uzun olamaz")
        String surname,

        @Email(message = "Geçerli bir email adresi giriniz")
        @Size(max = 150, message = "Email 150 karakterden uzun olamaz")
        String email,

        @Pattern(
                regexp = "^[0-9+\\-\\s]{7,20}$",
                message = "Geçerli bir telefon numarası giriniz"
        )
        String phone,

        ERole role

) {}