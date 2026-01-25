package org.example.skills.Jwt;

import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.interfaces.DecodedJWT;
import org.example.entity.User;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;

@Service
public class JwtService {

    private final Algorithm algorithm;
    private final String issuer;
    private final JWTVerifier verifier;

    public JwtService(
            @Value("${kasadeneme3.jwt.secret-key}") String secret,
            @Value("${kasadeneme3.jwt.issuer}") String issuer
    ) {
        this.algorithm = Algorithm.HMAC256(secret);
        this.issuer = issuer;
        this.verifier = JWT.require(algorithm)
                .withIssuer(issuer)
                .build();
    }

    public String generateToken(User user, List<String> permissions) {
        return JWT.create()
                .withIssuer(issuer)
                .withSubject(user.getUsername())
                .withClaim("userId", user.getId())
                .withClaim("companyId", user.getCompanyId())
                .withClaim("role", user.getRole().name())
                .withClaim("permissions", permissions)
                .withIssuedAt(new Date())
                .withExpiresAt(new Date(System.currentTimeMillis() + 1000L * 60 * 60 * 24 * 30)) // 30 gün
                .sign(algorithm);
    }

    public DecodedJWT verify(String token) {
        return verifier.verify(token);
    }
}
