package br.com.asa.actsquad.web.dto;

import br.com.asa.actsquad.domain.UserRole;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.UUID;

public final class AuthDtos {

  private AuthDtos() {}

  public record RegisterRequest(
      @NotBlank @Email String email,
      @NotBlank @Size(min = 8, max = 100) String password,
      @Size(max = 120) String name) {}

  public record LoginRequest(@NotBlank @Email String email, @NotBlank String password) {}

  public record AuthResponse(String token, UserResponse user) {}

  public record UserResponse(
      UUID id, String email, String name, UserRole role, boolean termsAccepted) {}
}
