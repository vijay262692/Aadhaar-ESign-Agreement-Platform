package com.pvtalent.esign.repository;

import com.pvtalent.esign.model.Agreement;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.util.UUID;

public interface AgreementRepository extends JpaRepository<Agreement, UUID> {
    Optional<Agreement> findByCandidateSigningToken(String token);
    Optional<Agreement> findByConsultantSigningToken(String token);
}
