package org.example.dto.response;

import org.example.skills.enums.ERole;

import java.util.List;

public record LoginResponse(
        String token,
        String username,
        ERole role,
        List<String> permissions
) {}


