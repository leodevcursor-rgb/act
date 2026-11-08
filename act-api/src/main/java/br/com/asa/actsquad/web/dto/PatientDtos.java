package br.com.asa.actsquad.web.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.UUID;

public final class PatientDtos {

  private PatientDtos() {}

  public record CreatePatientRequest(
      @NotBlank @Size(min = 1, max = 60) String displayName,
      @NotBlank @Size(min = 11, max = 14) String cpf,
      @Size(max = 120) String note) {}

  /** Campos ausentes (null) permanecem inalterados; note "" limpa a nota. */
  public record UpdatePatientRequest(
      @Size(min = 1, max = 60) String displayName, @Size(max = 120) String note) {}

  public record PatientDto(
      UUID id,
      String displayName,
      String note,
      @JsonProperty("isDefault") boolean isDefault,
      Instant createdAt,
      Instant updatedAt,
      Instant archivedAt) {}
}
