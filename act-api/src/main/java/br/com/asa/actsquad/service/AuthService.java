package br.com.asa.actsquad.service;

import br.com.asa.actsquad.domain.AppUser;
import br.com.asa.actsquad.domain.Patient;
import br.com.asa.actsquad.domain.Subscription;
import br.com.asa.actsquad.domain.SubscriptionStatus;
import br.com.asa.actsquad.domain.UserRole;
import br.com.asa.actsquad.repo.AppUserRepository;
import br.com.asa.actsquad.repo.PatientRepository;
import br.com.asa.actsquad.repo.SubscriptionRepository;
import br.com.asa.actsquad.repo.TermsAcceptanceRepository;
import br.com.asa.actsquad.security.JwtService;
import br.com.asa.actsquad.security.UserPrincipal;
import br.com.asa.actsquad.util.CpfUtil;
import br.com.asa.actsquad.web.dto.AuthDtos.AuthResponse;
import br.com.asa.actsquad.web.dto.AuthDtos.LoginRequest;
import br.com.asa.actsquad.web.dto.AuthDtos.RegisterRequest;
import br.com.asa.actsquad.web.dto.AuthDtos.UserResponse;
import br.com.asa.actsquad.web.error.ApiException;
import java.time.Instant;
import java.util.UUID;
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
  private final PatientRepository patientRepository;
  private final PatientService patientService;
  private final PasswordEncoder passwordEncoder;
  private final JwtService jwtService;
  private final String termsVersion;

  public AuthService(
      AppUserRepository userRepository,
      SubscriptionRepository subscriptionRepository,
      TermsAcceptanceRepository termsRepository,
      PatientRepository patientRepository,
      PatientService patientService,
      PasswordEncoder passwordEncoder,
      JwtService jwtService,
      @Value("${act.terms-version}") String termsVersion) {
    this.userRepository = userRepository;
    this.subscriptionRepository = subscriptionRepository;
    this.termsRepository = termsRepository;
    this.patientRepository = patientRepository;
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

    return toAuthResponse(user, null);
  }

  @Transactional(readOnly = true)
  public AuthResponse login(LoginRequest request) {
    String login = request.email() == null ? "" : request.email().trim();
    String password = request.password() == null ? "" : request.password();

    if (login.contains("@")) {
      AppUser user =
          userRepository
              .findByEmailIgnoreCaseAndDeletedAtIsNull(login)
              .orElseThrow(
                  () -> new ApiException(HttpStatus.UNAUTHORIZED, "Email ou senha inválidos"));
      if (user.getRole() == UserRole.patient) {
        throw new ApiException(HttpStatus.UNAUTHORIZED, "Email ou senha inválidos");
      }
      if (user.getPasswordHash() == null
          || !passwordEncoder.matches(password, user.getPasswordHash())) {
        throw new ApiException(HttpStatus.UNAUTHORIZED, "Email ou senha inválidos");
      }
      return toAuthResponse(user, null);
    }

    return loginPatient(login, password);
  }

  private AuthResponse loginPatient(String name, String password) {
    String cpf = CpfUtil.digitsOnly(password);
    if (!CpfUtil.isValid(cpf)) {
      throw new ApiException(HttpStatus.UNAUTHORIZED, "Nome ou CPF inválidos");
    }
    Patient patient =
        patientRepository
            .findByCpfDigits(cpf)
            .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "Nome ou CPF inválidos"));
    AppUser account = patient.getAccountUser();
    if (account == null || !account.isActive() || account.getRole() != UserRole.patient) {
      throw new ApiException(HttpStatus.UNAUTHORIZED, "Nome ou CPF inválidos");
    }
    String expectedName = account.getName() == null ? "" : account.getName().trim();
    if (!expectedName.equalsIgnoreCase(name.trim())) {
      throw new ApiException(HttpStatus.UNAUTHORIZED, "Nome ou CPF inválidos");
    }
    if (account.getPasswordHash() == null
        || !passwordEncoder.matches(cpf, account.getPasswordHash())) {
      throw new ApiException(HttpStatus.UNAUTHORIZED, "Nome ou CPF inválidos");
    }
    return toAuthResponse(account, patient.getId());
  }

  @Transactional(readOnly = true)
  public UserResponse me(UserPrincipal principal) {
    AppUser user =
        userRepository
            .findById(principal.getId())
            .filter(AppUser::isActive)
            .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "Usuário inválido"));
    UUID patientId = principal.getPatientId();
    String displayName = null;
    if (user.getRole() == UserRole.patient) {
      Patient patient =
          patientId == null
              ? patientRepository.findByAccountUserId(user.getId()).orElse(null)
              : patientRepository.findById(patientId).orElse(null);
      if (patient != null) {
        patientId = patient.getId();
        displayName = patient.getDisplayName();
      }
    }
    return toUserResponse(user, patientId, displayName);
  }

  private AuthResponse toAuthResponse(AppUser user, UUID patientId) {
    String displayName = null;
    if (patientId != null) {
      displayName =
          patientRepository.findById(patientId).map(Patient::getDisplayName).orElse(user.getName());
    }
    UserPrincipal principal = UserPrincipal.from(user, patientId);
    return new AuthResponse(
        jwtService.generateToken(principal), toUserResponse(user, patientId, displayName));
  }

  private UserResponse toUserResponse(AppUser user, UUID patientId, String displayName) {
    boolean accepted =
        user.getRole() == UserRole.patient
            || termsRepository.existsByUserIdAndTermsVersion(user.getId(), termsVersion);
    if (user.getRole() == UserRole.patient) {
      accepted = true;
    }
    String emailOut = user.getRole() == UserRole.patient ? null : user.getEmail();
    return new UserResponse(
        user.getId(),
        emailOut,
        user.getName(),
        user.getRole(),
        accepted,
        patientId,
        displayName);
  }
}
