package br.com.asa.actsquad.repo;

import br.com.asa.actsquad.domain.PatientState;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PatientStateRepository extends JpaRepository<PatientState, UUID> {}
