package org.example.dto.response;

import org.example.skills.enums.ERole;
import org.example.skills.enums.BranchType;

import java.util.List;

public record LoginResponse(
        String token,
        String username,
        ERole role,
        Long companyId,
        String companyName,
        BranchType branchType,
        List<String> permissions
) {}


