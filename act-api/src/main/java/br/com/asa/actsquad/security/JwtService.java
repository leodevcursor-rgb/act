package br.com.asa.actsquad.security;

import br.com.asa.actsquad.config.JwtProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.UUID;
import javax.crypto.SecretKey;
import org.springframework.stereotype.Service;

@Service
public class JwtService {

  private final JwtProperties properties;
  private final SecretKey key;

  public JwtService(JwtProperties properties) {
    this.properties = properties;
    byte[] bytes = properties.getSecret().getBytes(StandardCharsets.UTF_8);
    if (bytes.length < 32) {
      throw new IllegalStateException("ACT_JWT_SECRET must be at least 32 characters");
    }
    this.key = Keys.hmacShaKeyFor(bytes);
  }

  public String generateToken(UserPrincipal principal) {
    Date now = new Date();
    Date exp = new Date(now.getTime() + properties.getExpirationMs());
    var builder =
        Jwts.builder()
            .subject(principal.getId().toString())
            .claim("email", principal.getUsername())
            .claim("role", principal.getRole().name())
            .issuedAt(now)
            .expiration(exp);
    if (principal.getPatientId() != null) {
      builder.claim("patientId", principal.getPatientId().toString());
    }
    return builder.signWith(key).compact();
  }

  public UUID parsePatientId(String token) {
    String raw = parseClaims(token).get("patientId", String.class);
    return raw == null || raw.isBlank() ? null : UUID.fromString(raw);
  }

  public UUID parseUserId(String token) {
    return UUID.fromString(parseClaims(token).getSubject());
  }

  public String parseEmail(String token) {
    return parseClaims(token).get("email", String.class);
  }

  public boolean isValid(String token) {
    try {
      parseClaims(token);
      return true;
    } catch (Exception e) {
      return false;
    }
  }

  private Claims parseClaims(String token) {
    return Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload();
  }
}
