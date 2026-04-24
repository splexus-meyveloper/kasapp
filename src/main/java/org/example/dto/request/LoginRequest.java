package org.example.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record LoginRequest(

        @NotBlank(message = "Şirket kodu boş olamaz")
        @Size(max = 20, message = "Geçersiz şirket kodu")
        String companyCode,

        @NotBlank(message = "Kullanıcı adı boş olamaz")
        @Size(max = 50, message = "Kullanıcı adı 50 karakterden uzun olamaz")
        String username,

        @NotBlank(message = "Şifre boş olamaz")
        @Size(max = 100, message = "Şifre 100 karakterden uzun olamaz")
        String password

) {}