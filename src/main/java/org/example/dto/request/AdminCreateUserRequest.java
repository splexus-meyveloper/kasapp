package org.example.dto.request;

import jakarta.validation.constraints.*;
import org.example.skills.enums.ERole;

public record AdminCreateUserRequest(

        @NotBlank(message = "Kullanıcı adı boş olamaz")
        @Size(min = 3, max = 50, message = "Kullanıcı adı 3-50 karakter arasında olmalıdır")
        @Pattern(
                regexp = "^[a-zA-Z0-9_\\.çÇğĞıİöÖşŞüÜ]+$",
                message = "Kullanıcı adı sadece harf, rakam, nokta ve alt çizgi içerebilir"
        )
        String username,

        @NotBlank(message = "Şifre boş olamaz")
        @Pattern(regexp = "^[0-9]{4}$", message = "Şifre tam olarak 4 rakam olmalıdır")
        String password,

        // Opsiyonel alanlar — frontend göndermeyebilir
        String name,
        String surname,
        String email,
        String phone,
        ERole role,

        // Hangi şubeye ekleneceği — null ise admin'in kendi şubesi
        Long companyId

) {}
