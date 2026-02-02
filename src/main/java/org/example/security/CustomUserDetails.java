package org.example.security;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;

public class CustomUserDetails implements UserDetails {

    private final Long id;
    private final Long companyId;
    private final String username;
    private final Collection<? extends GrantedAuthority> authorities;

    public CustomUserDetails(Long id,
                             Long companyId,
                             String username,
                             Collection<? extends GrantedAuthority> authorities) {
        this.id = id;
        this.companyId = companyId;
        this.username = username;
        this.authorities = authorities;
    }

    // ✅ Custom alanlar
    public Long getId() {
        return id;
    }

    public Long getCompanyId() {
        return companyId;
    }

    // ✅ UserDetails zorunluları
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    @Override
    public String getPassword() {
        return null; // JWT’de gerek yok
    }

    @Override
    public String getUsername() {
        return username;
    }

    @Override public boolean isAccountNonExpired() { return true; }
    @Override public boolean isAccountNonLocked() { return true; }
    @Override public boolean isCredentialsNonExpired() { return true; }
    @Override public boolean isEnabled() { return true; }
}
