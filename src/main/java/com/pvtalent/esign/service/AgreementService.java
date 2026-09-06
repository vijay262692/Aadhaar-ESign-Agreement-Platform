package com.pvtalent.esign.service;

import com.pvtalent.esign.dto.CallbackRequest;
import com.pvtalent.esign.dto.CreateAgreementRequest;
import com.pvtalent.esign.dto.SignRequest;
import com.pvtalent.esign.model.*;
import com.pvtalent.esign.repository.AgreementRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.file.*;
import java.time.LocalDateTime;
import java.util.*;

@Service
public class AgreementService {
    private final AgreementRepository repo;
    private final ESignService eSignService;

    @Value("${app.frontend-base-url:http://localhost:8080}")
    private String frontendBaseUrl;

    @Value("${app.upload-dir:./uploads}")
    private String uploadDir;

    public AgreementService(AgreementRepository repo, ESignService eSignService) {
        this.repo = repo;
        this.eSignService = eSignService;
    }

    public List<Agreement> all() { return repo.findAll(); }

    public Agreement get(UUID id) {
        return repo.findById(id).orElseThrow(() -> new NoSuchElementException("Agreement not found"));
    }

    public Agreement getByToken(String token, Party party) {
        return (party == Party.CANDIDATE
            ? repo.findByCandidateSigningToken(token)
            : repo.findByConsultantSigningToken(token))
            .orElseThrow(() -> new NoSuchElementException("Invalid or expired signing link"));
    }

    public Agreement create(CreateAgreementRequest r) {
        Agreement a = new Agreement();
        a.setAgreementNumber("PVTP/2026-2027/CANDIDATE/" +
                UUID.randomUUID().toString().substring(0,8).toUpperCase());
        a.setCandidateFullName(r.getCandidateFullName());
        a.setCandidateAddress(r.getCandidateAddress());
        a.setCandidateIdProofType(r.getCandidateIdProofType());
        a.setCandidateIdProofReference(r.getCandidateIdProofReference());
        a.setTotalFeeAmount(r.getTotalFeeAmount());
        a.setPrePaymentAmount(r.getPrePaymentAmount());
        a.setBalanceAmount(r.getBalanceAmount());
        a.setExecutionDate(r.getExecutionDate());
        a.setCandidateEmail(r.getCandidateEmail());
        a.setCandidateMobile(r.getCandidateMobile());
        a.setCandidateSigningToken(randomToken());
        a.setConsultantSigningToken(randomToken());
        a.setStatus(AgreementStatus.AWAITING_CANDIDATE);
        return repo.save(a);
    }

    public Map<String,Object> links(Agreement a) {
        return Map.of(
            "candidateSigningUrl", frontendBaseUrl + "/sign.html?party=candidate&token=" + a.getCandidateSigningToken(),
            "consultantSigningUrl", frontendBaseUrl + "/sign.html?party=consultant&token=" + a.getConsultantSigningToken()
        );
    }

    public Map<String,Object> startEsign(Agreement a, Party party) {
        String callback = frontendBaseUrl + "/api/esign/callback";
        String tx = eSignService.createSigningRequest(a, party, callback);
        return Map.of("transactionId", tx, "message", "Redirect the user to the provider's signing URL here.");
    }

    @Transactional
    public Agreement callback(CallbackRequest r) {
        Agreement a = get(UUID.fromString(r.getAgreementId()));
        if (!r.isSuccessful() || !eSignService.verifyCallback(r.getTransactionId(), r.getProviderAuditReference())) {
            throw new IllegalArgumentException("eSign callback verification failed");
        }
        return applySignature(a, Party.valueOf(r.getParty().toUpperCase()), r.getTransactionId(), r.getSignedDocumentReference());
    }

    @Transactional
    public Agreement demoSign(UUID id, Party party) {
        Agreement a = get(id);
        return applySignature(a, party, "DEMO-" + UUID.randomUUID(), null);
    }

    private Agreement applySignature(Agreement a, Party party, String tx, String docRef) {
        if (party == Party.CANDIDATE) {
            a.setCandidateSigningStatus(SigningStatus.SIGNED);
            a.setCandidateEsignTransactionId(tx);
            a.setCandidateSignedAt(LocalDateTime.now());
        } else {
            a.setConsultantSigningStatus(SigningStatus.SIGNED);
            a.setConsultantEsignTransactionId(tx);
            a.setConsultantSignedAt(LocalDateTime.now());
        }
        if (docRef != null && !docRef.isBlank()) a.setSignedDocumentReference(docRef);

        if (a.isFullySigned()) {
            a.setStatus(AgreementStatus.FULLY_SIGNED);
            a.setCompletedAt(LocalDateTime.now());
        } else if (a.getCandidateSigningStatus() == SigningStatus.SIGNED) {
            a.setStatus(AgreementStatus.AWAITING_CONSULTANT);
        } else {
            a.setStatus(AgreementStatus.AWAITING_CANDIDATE);
        }
        return repo.save(a);
    }

    public void savePdf(UUID id, String filename, byte[] bytes) throws Exception {
        Agreement a = get(id);
        Path dir = Paths.get(uploadDir).toAbsolutePath().normalize();
        Files.createDirectories(dir);
        String safe = UUID.randomUUID() + "-" + filename.replaceAll("[^a-zA-Z0-9._-]", "_");
        Path out = dir.resolve(safe);
        Files.write(out, bytes, StandardOpenOption.CREATE_NEW);
        a.setOriginalPdfPath(out.toString());
        repo.save(a);
    }

    public byte[] readPdf(UUID id) throws Exception {
        Agreement a = get(id);
        if (a.getOriginalPdfPath() == null) throw new NoSuchElementException("No PDF uploaded");
        return Files.readAllBytes(Paths.get(a.getOriginalPdfPath()));
    }

    private String randomToken() {
        return UUID.randomUUID().toString().replace("-", "") +
               UUID.randomUUID().toString().replace("-", "");
    }
}
