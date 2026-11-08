package br.com.asa.actsquad.service;

import br.com.asa.actsquad.domain.AppUser;
import br.com.asa.actsquad.domain.Patient;
import br.com.asa.actsquad.domain.PatientState;
import br.com.asa.actsquad.domain.UserRole;
import br.com.asa.actsquad.repo.AppUserRepository;
import br.com.asa.actsquad.repo.PatientRepository;
import br.com.asa.actsquad.repo.PatientStateRepository;
import br.com.asa.actsquad.security.UserPrincipal;
import br.com.asa.actsquad.util.CpfUtil;
import br.com.asa.actsquad.web.dto.PatientDtos.CreatePatientRequest;
import br.com.asa.actsquad.web.dto.PatientDtos.PatientDto;
import br.com.asa.actsquad.web.dto.PatientDtos.UpdatePatientRequest;
import br.com.asa.actsquad.web.error.ApiException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PatientService {

  public static final String DEFAULT_PATIENT_NAME = "Paciente padrão";

  private final PatientRepository patientRepository;
  private final PatientStateRepository patientStateRepository;
  private final AppUserRepository userRepository;
  private final PasswordEncoder passwordEncoder;

  public PatientService(
      PatientRepository patientRepository,
      PatientStateRepository patientStateRepository,
      AppUserRepository userRepository,
      PasswordEncoder passwordEncoder) {
    this.patientRepository = patientRepository;
    this.patientStateRepository = patientStateRepository;
    this.userRepository = userRepository;
    this.passwordEncoder = passwordEncoder;
  }

  /**
   * Lista pacientes não arquivados. Por segurança, garante que exista ao menos um paciente e que
   * exista um paciente padrão.
   */
  @Transactional
  public List<PatientDto> list(UUID therapistUserId) {
    List<Patient> active =
        new ArrayList<>(
            patientRepository.findByUserIdAndArchivedAtIsNullOrderByCreatedAtAsc(therapistUserId));
    if (active.isEmpty()) {
      active.add(createDefault(requireUser(therapistUserId)));
    } else if (active.stream().noneMatch(Patient::isDefault)) {
      Patient oldest = active.get(0);
      oldest.setDefault(true);
      oldest.setUpdatedAt(Instant.now());
      patientRepository.save(oldest);
    }
    active.sort(Comparator.comparing(Patient::getUpdatedAt).reversed());
    return active.stream().map(PatientService::toDto).toList();
  }

  @Transactional(readOnly = true)
  public PatientDto get(UserPrincipal principal, UUID patientId) {
    return toDto(requireAccessible(principal, patientId));
  }

  @Transactional
  public PatientDto create(UUID therapistUserId, CreatePatientRequest request) {
    AppUser therapist = requireUser(therapistUserId);
    if (therapist.getRole() != UserRole.therapist && therapist.getRole() != UserRole.admin) {
      throw new ApiException(HttpStatus.FORBIDDEN, "Apenas o terapeuta pode cadastrar pacientes");
    }

    String cpf = CpfUtil.digitsOnly(request.cpf());
    if (!CpfUtil.isValid(cpf)) {
      throw new ApiException(HttpStatus.BAD_REQUEST, "CPF inválido");
    }
    if (patientRepository.existsByCpfDigits(cpf)) {
      throw new ApiException(HttpStatus.CONFLICT, "Já existe um paciente com este CPF");
    }

    String displayName = request.displayName().trim();
    Instant now = Instant.now();

    AppUser account = new AppUser();
    account.setEmail("p." + cpf + "@patients.actsquad");
    account.setPasswordHash(passwordEncoder.encode(cpf));
    account.setName(displayName);
    account.setRole(UserRole.patient);
    account.setCreatedAt(now);
    account.setUpdatedAt(now);
    userRepository.save(account);

    Patient patient = new Patient();
    patient.setUser(therapist);
    patient.setDisplayName(displayName);
    patient.setNote(blankToNull(request.note()));
    patient.setCpfDigits(cpf);
    patient.setAccountUser(account);
    patient.setDefault(false);
    patient.setCreatedAt(now);
    patient.setUpdatedAt(now);
    patientRepository.save(patient);
    saveEmptyState(patient, now);
    return toDto(patient);
  }

  @Transactional
  public PatientDto update(UUID therapistUserId, UUID patientId, UpdatePatientRequest request) {
    Patient patient = requireOwned(therapistUserId, patientId);
    if (request.displayName() != null) {
      String name = request.displayName().trim();
      if (name.isEmpty()) {
        throw new ApiException(HttpStatus.BAD_REQUEST, "Nome do paciente não pode ser vazio");
      }
      patient.setDisplayName(name);
      AppUser account = patient.getAccountUser();
      if (account != null) {
        account.setName(name);
        account.setUpdatedAt(Instant.now());
        userRepository.save(account);
      }
    }
    if (request.note() != null) {
      patient.setNote(blankToNull(request.note()));
    }
    patient.setUpdatedAt(Instant.now());
    patientRepository.save(patient);
    return toDto(patient);
  }

  /**
   * Remove o paciente. Bloqueado se for o único paciente não arquivado. Se for o padrão e houver
   * outros, o mais antigo dos demais é promovido a padrão.
   */
  @Transactional
  public void delete(UUID therapistUserId, UUID patientId) {
    Patient patient = requireOwned(therapistUserId, patientId);
    List<Patient> active =
        patientRepository.findByUserIdAndArchivedAtIsNullOrderByCreatedAtAsc(therapistUserId);
    List<Patient> others = active.stream().filter(p -> !p.getId().equals(patientId)).toList();

    if (patient.getArchivedAt() == null && others.isEmpty()) {
      throw new ApiException(
          HttpStatus.CONFLICT, "Não é possível excluir o único paciente. Crie outro antes.");
    }

    Patient promote = null;
    if (patient.isDefault()) {
      if (others.isEmpty()) {
        throw new ApiException(
            HttpStatus.CONFLICT,
            "Não é possível excluir o paciente padrão sem outro paciente para promover.");
      }
      promote = others.get(0);
    }

    AppUser account = patient.getAccountUser();
    patientStateRepository.deleteById(patientId);
    patientRepository.delete(patient);
    patientRepository.flush();

    if (account != null) {
      account.setDeletedAt(Instant.now());
      account.setUpdatedAt(Instant.now());
      userRepository.save(account);
    }

    if (promote != null) {
      promote.setDefault(true);
      promote.setUpdatedAt(Instant.now());
      patientRepository.save(promote);
    }
  }

  @Transactional
  public JsonNode getState(UserPrincipal principal, UUID patientId) {
    Patient patient = requireAccessible(principal, patientId);
    return patientStateRepository
        .findById(patient.getId())
        .orElseGet(() -> saveEmptyState(patient, Instant.now()))
        .getState();
  }

  @Transactional
  public JsonNode putState(UserPrincipal principal, UUID patientId, JsonNode body) {
    if (principal.isPatient()) {
      throw new ApiException(
          HttpStatus.FORBIDDEN, "Conta somente leitura: não é possível alterar o caderno");
    }
    if (body == null || !body.isObject()) {
      throw new ApiException(HttpStatus.BAD_REQUEST, "O estado deve ser um objeto JSON");
    }
    Patient patient = requireOwned(principal.getId(), patientId);
    Instant now = Instant.now();
    PatientState state =
        patientStateRepository.findById(patient.getId()).orElseGet(() -> newState(patient));
    state.setState(body);
    state.setUpdatedAt(now);
    patientStateRepository.save(state);
    patient.setUpdatedAt(now);
    patientRepository.save(patient);
    return state.getState();
  }

  /** Cria o paciente padrão e seu estado vazio. Usado no registro e como salvaguarda. */
  @Transactional
  public Patient createDefault(AppUser user) {
    Instant now = Instant.now();
    Patient patient = new Patient();
    patient.setUser(user);
    patient.setDisplayName(DEFAULT_PATIENT_NAME);
    patient.setDefault(true);
    patient.setCreatedAt(now);
    patient.setUpdatedAt(now);
    patientRepository.save(patient);
    saveEmptyState(patient, now);
    return patient;
  }

  Patient requireAccessible(UserPrincipal principal, UUID patientId) {
    if (principal.isPatient()) {
      return patientRepository
          .findByIdAndAccountUserId(patientId, principal.getId())
          .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Paciente não encontrado"));
    }
    return requireOwned(principal.getId(), patientId);
  }

  Patient requireOwned(UUID therapistUserId, UUID patientId) {
    return patientRepository
        .findByIdAndUserId(patientId, therapistUserId)
        .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Paciente não encontrado"));
  }

  private PatientState saveEmptyState(Patient patient, Instant now) {
    PatientState state = newState(patient);
    state.setUpdatedAt(now);
    return patientStateRepository.save(state);
  }

  private static PatientState newState(Patient patient) {
    PatientState state = new PatientState();
    state.setPatient(patient);
    state.setState(JsonNodeFactory.instance.objectNode());
    return state;
  }

  private AppUser requireUser(UUID userId) {
    return userRepository
        .findById(userId)
        .filter(AppUser::isActive)
        .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "Usuário inválido"));
  }

  static PatientDto toDto(Patient p) {
    return new PatientDto(
        p.getId(),
        p.getDisplayName(),
        p.getNote(),
        p.isDefault(),
        p.getCreatedAt(),
        p.getUpdatedAt(),
        p.getArchivedAt());
  }

  private static String blankToNull(String note) {
    if (note == null || note.isBlank()) {
      return null;
    }
    return note.trim();
  }
}
