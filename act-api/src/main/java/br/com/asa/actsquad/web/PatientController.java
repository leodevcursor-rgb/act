package br.com.asa.actsquad.web;

import br.com.asa.actsquad.security.SecurityUtils;
import br.com.asa.actsquad.service.PatientService;
import br.com.asa.actsquad.web.dto.PatientDtos.CreatePatientRequest;
import br.com.asa.actsquad.web.dto.PatientDtos.PatientDto;
import br.com.asa.actsquad.web.dto.PatientDtos.UpdatePatientRequest;
import com.fasterxml.jackson.databind.JsonNode;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/patients")
public class PatientController {

  private final PatientService patientService;

  public PatientController(PatientService patientService) {
    this.patientService = patientService;
  }

  @GetMapping
  public List<PatientDto> list() {
    SecurityUtils.requireTherapist();
    return patientService.list(SecurityUtils.currentUserId());
  }

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  public PatientDto create(@Valid @RequestBody CreatePatientRequest request) {
    SecurityUtils.requireTherapist();
    return patientService.create(SecurityUtils.currentUserId(), request);
  }

  @GetMapping("/{id}")
  public PatientDto get(@PathVariable UUID id) {
    return patientService.get(SecurityUtils.currentUser(), id);
  }

  @PatchMapping("/{id}")
  public PatientDto update(@PathVariable UUID id, @Valid @RequestBody UpdatePatientRequest request) {
    SecurityUtils.requireTherapist();
    return patientService.update(SecurityUtils.currentUserId(), id, request);
  }

  @DeleteMapping("/{id}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void delete(@PathVariable UUID id) {
    SecurityUtils.requireTherapist();
    patientService.delete(SecurityUtils.currentUserId(), id);
  }

  @GetMapping("/{id}/state")
  public JsonNode getState(@PathVariable UUID id) {
    return patientService.getState(SecurityUtils.currentUser(), id);
  }

  @PutMapping("/{id}/state")
  public JsonNode putState(@PathVariable UUID id, @RequestBody JsonNode body) {
    return patientService.putState(SecurityUtils.currentUser(), id, body);
  }
}
