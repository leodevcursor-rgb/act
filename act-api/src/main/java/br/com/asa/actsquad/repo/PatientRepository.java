package br.com.asa.actsquad.repo;

import br.com.asa.actsquad.domain.Patient;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PatientRepository extends JpaRepository<Patient, UUID> {

  List<Patient> findByUserIdAndArchivedAtIsNullOrderByCreatedAtAsc(UUID userId);

  Optional<Patient> findByIdAndUserId(UUID id, UUID userId);
}
