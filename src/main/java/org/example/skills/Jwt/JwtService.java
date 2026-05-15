package org.example.skills.Jwt;

import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.interfaces.DecodedJWT;
import org.example.entity.Company;
import org.example.entity.User;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.Date;
import java.util.List;

@Service
public class JwtService {

    private final Algorithm algorithm;
    private final String issuer;
    private final JWTVerifier verifier;

    private static final long TOKEN_VALIDITY_MS = 1000L * 60 * 60 * 8; // 8 saat

    public JwtService(
            @Value("${kasadeneme3.jwt.secret-key}") String secret,
            @Value("${kasadeneme3.jwt.issuer}") String issuer,
            Environment environment
    ) {
        if (secret == null || secret.isBlank()) {
            boolean devProfile = Arrays.asList(environment.getActiveProfiles()).contains("dev");
            if (!devProfile) {
                throw new IllegalStateException("KASADENEME3_JWT_SECRET_KEY must be set outside dev profile");
            }
            secret = "DEV_ONLY_CHANGE_IN_PRODUCTION_MIN32CH";
        }
        this.algorithm = Algorithm.HMAC256(secret);
        this.issuer = issuer;
        this.verifier = JWT.require(algorithm)
                .withIssuer(issuer)
                .build();
    }

    public String generateToken(User user, Company company, List<String> permissions) {
        return JWT.create()
                .withIssuer(issuer)
                .withSubject(user.getUsername())
                .withClaim("userId", user.getId())
                .withClaim("companyId", user.getCompanyId())
                .withClaim("companyName", company.getName())
                .withClaim("branchType", company.getBranchType() != null ? company.getBranchType().name() : null)
                .withClaim("role", user.getRole().name())
                .withClaim("permissions", permissions)
                .withIssuedAt(new Date())
                .withExpiresAt(new Date(System.currentTimeMillis() + TOKEN_VALIDITY_MS))
                .sign(algorithm);
    }

    public DecodedJWT verify(String token) {
        return verifier.verify(token);
    }
}
