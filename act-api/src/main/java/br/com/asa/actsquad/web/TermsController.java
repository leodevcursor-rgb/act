package br.com.asa.actsquad.web;

import br.com.asa.actsquad.security.SecurityUtils;
import br.com.asa.actsquad.service.TermsService;
import br.com.asa.actsquad.web.dto.TermsDtos.AcceptTermsRequest;
import br.com.asa.actsquad.web.dto.TermsDtos.TermsStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/terms")
public class TermsController {

  private final TermsService termsService;

  public TermsController(TermsService termsService) {
    this.termsService = termsService;
  }

  @GetMapping
  public TermsStatus status() {
    return termsService.status(SecurityUtils.currentUserId());
  }

  @PostMapping("/accept")
  public TermsStatus accept(@RequestBody(required = false) AcceptTermsRequest request) {
    String version = request == null ? null : request.termsVersion();
    return termsService.accept(SecurityUtils.currentUserId(), version);
  }
}
