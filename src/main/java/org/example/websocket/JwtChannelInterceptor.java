package org.example.websocket;

import com.auth0.jwt.interfaces.DecodedJWT;
import lombok.RequiredArgsConstructor;
import org.example.security.CustomUserDetails;
import org.example.skills.Jwt.JwtService;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;

import java.security.Principal;
import java.util.ArrayList;
import java.util.List;

@Component
@RequiredArgsConstructor
public class JwtChannelInterceptor implements ChannelInterceptor {

    private final JwtService jwtService;

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {

        StompHeaderAccessor accessor =
                MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);

        if (accessor == null) return message;

        if (StompCommand.CONNECT.equals(accessor.getCommand())) {

            String authHeader = accessor.getFirstNativeHeader("Authorization");

            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                throw new IllegalArgumentException("WebSocket bağlantısı için Authorization header gerekli.");
            }

            String token = authHeader.substring(7);
            DecodedJWT jwt = jwtService.verify(token);

            String username  = jwt.getSubject();
            Long userId      = jwt.getClaim("userId").asLong();
            Long companyId   = jwt.getClaim("companyId").asLong();
            String role      = jwt.getClaim("role").asString();
            List<String> permissions = jwt.getClaim("permissions").asList(String.class);

            List<SimpleGrantedAuthority> authorities = new ArrayList<>();
            if (role != null)        authorities.add(new SimpleGrantedAuthority("ROLE_" + role));
            if (permissions != null) permissions.forEach(p -> authorities.add(new SimpleGrantedAuthority(p)));

            CustomUserDetails userDetails = new CustomUserDetails(userId, companyId, username, role, authorities);

            Principal principal = new UsernamePasswordAuthenticationToken(userDetails, null, authorities);
            accessor.setUser(principal);
        }

        if (StompCommand.SUBSCRIBE.equals(accessor.getCommand())) {

            Principal principal = accessor.getUser();

            if (principal == null) {
                throw new IllegalStateException("Kimliği doğrulanmamış kullanıcı abone olamaz.");
            }

            String destination = accessor.getDestination();

            if (destination != null && destination.startsWith("/topic/company-")) {
                String requestedId = destination.replace("/topic/company-", "");

                if (principal instanceof UsernamePasswordAuthenticationToken authToken) {
                    Object details = authToken.getPrincipal();
                    if (details instanceof CustomUserDetails ud) {
                        String ownCompanyId = String.valueOf(ud.getCompanyId());
                        if (!ownCompanyId.equals(requestedId)) {
                            throw new IllegalArgumentException(
                                    "Başka bir şirketin kanalına abone olamazsınız. " +
                                            "İstenen: " + requestedId + ", Sahip: " + ownCompanyId
                            );
                        }
                    }
                }
            }
        }

        return message;
    }
}