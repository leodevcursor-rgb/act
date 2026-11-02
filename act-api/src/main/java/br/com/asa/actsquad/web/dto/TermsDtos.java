package br.com.asa.actsquad.web.dto;

import java.time.Instant;

public final class TermsDtos {

  private TermsDtos() {}

  public record AcceptTermsRequest(String termsVersion) {}

  public record TermsStatus(String currentVersion, boolean accepted, Instant acceptedAt) {}
}
