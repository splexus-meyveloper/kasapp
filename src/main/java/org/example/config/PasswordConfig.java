package org.example.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
public class PasswordConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        // Strength 10 (default) → makul güvenlik/hız dengesi
        // Admin şifreleri (8+ karakter) için yeterince güçlü
        // Alt kullanıcı 4 rakamlı şifreler zaten kısa,
        // 10 strength fazla yük oluşturmaz ama 12 gibi yüksek değer
        // düşük donanımda login yavaşlatır
        return new BCryptPasswordEncoder(10);
    }
}