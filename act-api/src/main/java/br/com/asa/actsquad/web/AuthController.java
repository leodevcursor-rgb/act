package br.com.asa.actsquad.web;

import br.com.asa.actsquad.security.SecurityUtils;
import br.com.asa.actsquad.service.AuthService;
import br.com.asa.actsquad.web.dto.AuthDtos.AuthResponse;
import br.com.asa.actsquad.web.dto.AuthDtos.LoginRequest;
import br.com.asa.actsquad.web.dto.AuthDtos.RegisterRequest;
import br.com.asa.actsquad.web.dto.AuthDtos.UserResponse;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
public class AuthController {

  private final AuthService authService;

  public AuthController(AuthService authService) {
    this.authService = authService;
  }

  @PostMapping("/auth/register")
  public AuthResponse register(@Valid @RequestBody RegisterRequest request) {
    return authService.register(request);
  }

  @PostMapping("/auth/login")
  public AuthResponse login(@Valid @RequestBody LoginRequest request) {
    return authService.login(request);
  }

  @GetMapping("/me")
  public UserResponse me() {
    return authService.me(SecurityUtils.currentUser());
  }
}
