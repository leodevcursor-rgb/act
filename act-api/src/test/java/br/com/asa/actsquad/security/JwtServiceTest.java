package br.com.asa.actsquad.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import br.com.asa.actsquad.config.JwtProperties;
import br.com.asa.actsquad.domain.UserRole;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class JwtServiceTest {

  @Test
  void generatesAndParsesToken() {
    JwtProperties props = new JwtProperties();
    props.setSecret("test-secret-with-at-least-32-characters-ok");
    JwtService service = new JwtService(props);

    UUID id = UUID.randomUUID();
    UserPrincipal principal = new UserPrincipal(id, "a@b.com", "x", UserRole.therapist, true, null);
    String token = service.generateToken(principal);

    assertTrue(service.isValid(token));
    assertEquals(id, service.parseUserId(token));
    assertFalse(service.isValid(token + "x"));
  }
}
