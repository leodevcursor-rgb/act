package br.com.asa.actsquad.service;

import br.com.asa.actsquad.domain.AppUser;
import br.com.asa.actsquad.domain.TermsAcceptance;
import br.com.asa.actsquad.repo.AppUserRepository;
import br.com.asa.actsquad.repo.TermsAcceptanceRepository;
import br.com.asa.actsquad.web.dto.TermsDtos.TermsStatus;
import br.com.asa.actsquad.web.error.ApiException;
import java.time.Instant;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TermsService {

  private final TermsAcceptanceRepository termsRepository;
  private final AppUserRepository userRepository;
  private final String termsVersion;

  public TermsService(
      TermsAcceptanceRepository termsRepository,
      AppUserRepository userRepository,
      @Value("${act.terms-version}") String termsVersion) {
    this.termsRepository = termsRepository;
    this.userRepository = userRepository;
    this.termsVersion = termsVersion;
  }

  @Transactional(readOnly = true)
  public TermsStatus status(UUID userId) {
    return termsRepository
        .findByUserIdAndTermsVersion(userId, termsVersion)
        .map(t -> new TermsStatus(termsVersion, true, t.getAcceptedAt()))
        .orElse(new TermsStatus(termsVersion, false, null));
  }

  @Transactional
  public TermsStatus accept(UUID userId, String requestedVersion) {
    String version =
        requestedVersion == null || requestedVersion.isBlank()
            ? termsVersion
            : requestedVersion.trim();
    if (!termsVersion.equals(version)) {
      throw new ApiException(
          HttpStatus.BAD_REQUEST, "Versão de termos inválida. Atual: " + termsVersion);
    }
    return termsRepository
        .findByUserIdAndTermsVersion(userId, version)
        .map(t -> new TermsStatus(version, true, t.getAcceptedAt()))
        .orElseGet(
            () -> {
              AppUser user =
                  userRepository
                      .findById(userId)
                      .filter(AppUser::isActive)
                      .orElseThrow(
                          () -> new ApiException(HttpStatus.UNAUTHORIZED, "Usuário inválido"));
              TermsAcceptance t = new TermsAcceptance();
              t.setUser(user);
              t.setTermsVersion(version);
              t.setAcceptedAt(Instant.now());
              termsRepository.save(t);
              return new TermsStatus(version, true, t.getAcceptedAt());
            });
  }
}
