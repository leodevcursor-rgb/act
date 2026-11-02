package br.com.asa.actsquad.repo;

import br.com.asa.actsquad.domain.TermsAcceptance;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TermsAcceptanceRepository extends JpaRepository<TermsAcceptance, UUID> {

  Optional<TermsAcceptance> findByUserIdAndTermsVersion(UUID userId, String termsVersion);

  boolean existsByUserIdAndTermsVersion(UUID userId, String termsVersion);
}
