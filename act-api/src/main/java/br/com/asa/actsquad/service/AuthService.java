package br.com.asa.actsquad.service;

import br.com.asa.actsquad.domain.AppUser;
import br.com.asa.actsquad.domain.Subscription;
import br.com.asa.actsquad.domain.SubscriptionStatus;
import br.com.asa.actsquad.domain.UserRole;
import br.com.asa.actsquad.repo.AppUserRepository;
import br.com.asa.actsquad.repo.SubscriptionRepository;
import br.com.asa.actsquad.repo.TermsAcceptanceRepository;
import br.com.asa.actsquad.security.JwtService;
import br.com.asa.actsquad.security.UserPrincipal;
import br.com.asa.actsquad.web.dto.AuthDtos.AuthResponse;
import br.com.asa.actsquad.web.dto.AuthDtos.LoginRequest;
import br.com.asa.actsquad.web.dto.AuthDtos.RegisterRequest;
import br.com.asa.actsquad.web.dto.AuthDtos.UserResponse;
import br.com.asa.actsquad.web.error.ApiException;
import java.time.Instant;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

  private final AppUserRepository userRepository;
  private final SubscriptionRepository subscriptionRepository;
  private final TermsAcceptanceRepository termsRepository;
  private final PatientService patientService;
  private final PasswordEncoder passwordEncoder;
  private final JwtService jwtService;
  private final String termsVersion;

  public AuthService(
      AppUserRepository userRepository,
      SubscriptionRepository subscriptionRepository,
      TermsAcceptanceRepository termsRepository,
      PatientService patientService,
      PasswordEncoder passwordEncoder,
      JwtService jwtService,
      @Value("${act.terms-version}") String termsVersion) {
    this.userRepository = userRepository;
    this.subscriptionRepository = subscriptionRepository;
    this.termsRepository = termsRepository;
    this.patientService = patientService;
    this.passwordEncoder = passwordEncoder;
    this.jwtService = jwtService;
    this.termsVersion = termsVersion;
  }

  @Transactional
  public AuthResponse register(RegisterRequest request) {
    String email = request.email().trim().toLowerCase();
    if (userRepository.existsByEmailIgnoreCaseAndDeletedAtIsNull(email)) {
      throw new ApiException(HttpStatus.CONFLICT, "Email já cadastrado");
    }
    Instant now = Instant.now();
    AppUser user = new AppUser();
    user.setEmail(email);
    user.setPasswordHash(passwordEncoder.encode(request.password()));
    user.setName(request.name() == null || request.name().isBlank() ? null : request.name().trim());
    user.setRole(UserRole.therapist);
    user.setCreatedAt(now);
    user.setUpdatedAt(now);
    userRepository.save(user);

    Subscription subscription = new Subscription();
    subscription.setUser(user);
    subscription.setPlan("individual");
    subscription.setStatus(SubscriptionStatus.active);
    subscription.setStartsAt(now);
    subscription.setCreatedAt(now);
    subscription.setUpdatedAt(now);
    subscriptionRepository.save(subscription);

    patientService.createDefault(user);

    return toAuthResponse(user);
  }

  @Transactional(readOnly = true)
  public AuthResponse login(LoginRequest request) {
    AppUser user =
        userRepository
            .findByEmailIgnoreCaseAndDeletedAtIsNull(request.email().trim())
            .orElseThrow(
                () -> new ApiException(HttpStatus.UNAUTHORIZED, "Email ou senha inválidos"));
    if (user.getPasswordHash() == null
        || !passwordEncoder.matches(request.password(), user.getPasswordHash())) {
      throw new ApiException(HttpStatus.UNAUTHORIZED, "Email ou senha inválidos");
    }
    return toAuthResponse(user);
  }

  @Transactional(readOnly = true)
  public UserResponse me(UserPrincipal principal) {
    AppUser user =
        userRepository
            .findById(principal.getId())
            .filter(AppUser::isActive)
            .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "Usuário inválido"));
    return toUserResponse(user);
  }

  private AuthResponse toAuthResponse(AppUser user) {
    UserPrincipal principal = UserPrincipal.from(user);
    return new AuthResponse(jwtService.generateToken(principal), toUserResponse(user));
  }

  private UserResponse toUserResponse(AppUser user) {
    boolean accepted = termsRepository.existsByUserIdAndTermsVersion(user.getId(), termsVersion);
    return new UserResponse(user.getId(), user.getEmail(), user.getName(), user.getRole(), accepted);
  }
}
