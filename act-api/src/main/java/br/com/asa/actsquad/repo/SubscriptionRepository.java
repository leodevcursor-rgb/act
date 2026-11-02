package br.com.asa.actsquad.repo;

import br.com.asa.actsquad.domain.Subscription;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SubscriptionRepository extends JpaRepository<Subscription, UUID> {}
