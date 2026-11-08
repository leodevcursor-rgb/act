package br.com.asa.actsquad.security;

import br.com.asa.actsquad.web.error.ApiException;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

public final class SecurityUtils {

  private SecurityUtils() {}

  public static UserPrincipal currentUser() {
    Authentication auth = SecurityContextHolder.getContext().getAuthentication();
    if (auth == null || !(auth.getPrincipal() instanceof UserPrincipal principal)) {
      throw new AccessDeniedException("Não autenticado");
    }
    return principal;
  }

  public static UUID currentUserId() {
    return currentUser().getId();
  }

  public static void requireTherapist() {
    if (!currentUser().isTherapist()) {
      throw new ApiException(HttpStatus.FORBIDDEN, "Apenas o terapeuta pode fazer esta ação");
    }
  }
}
