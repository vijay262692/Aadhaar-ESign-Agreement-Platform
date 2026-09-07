package com.pvtalent.esign.service;

import com.pvtalent.esign.dto.CallbackRequest;
import com.pvtalent.esign.dto.CreateAgreementRequest;
import com.pvtalent.esign.dto.ESignStartRequest;
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
    private final EmailService emailService;

    @Value("${app.frontend-base-url:http://localhost:8080}")
    private String frontendBaseUrl;

    @Value("${app.upload-dir:./uploads}")
    private String uploadDir;

    public AgreementService(AgreementRepository repo, ESignService eSignService, EmailService emailService) {
        this.repo = repo;
        this.eSignService = eSignService;
        this.emailService = emailService;
    }

    /** Agreements visible in the consultant dashboard. Hidden agreements remain in the database. */
    public List<Agreement> all() {
        return repo.findAllByConsultantDeletedFalseOrConsultantDeletedIsNullOrderByCreatedAtDesc();
    }

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
        a.setConsultantEmail(r.getConsultantEmail());
        a.setCandidateSigningToken(randomToken());
        a.setConsultantSigningToken(randomToken());
        a.setStatus(AgreementStatus.AWAITING_CANDIDATE);

        Agreement saved = repo.save(a);
        emailService.sendClientInvitation(saved);
        return saved;
    }

    public Map<String,Object> links(Agreement a) {
        return Map.of(
            "candidateSigningUrl", frontendBaseUrl + "/sign.html?party=candidate&token=" + a.getCandidateSigningToken(),
            "consultantSigningUrl", frontendBaseUrl + "/sign.html?party=consultant&token=" + a.getConsultantSigningToken()
        );
    }

    public Map<String,Object> startEsign(Agreement a, Party party) {
        return startEsign(a, party, new ESignStartRequest());
    }

    public Map<String,Object> startEsign(Agreement a, Party party, ESignStartRequest request) {
        if (!request.isConsent()) {
            throw new IllegalArgumentException("Consent is required before electronic signing.");
        }

        if (party == Party.CONSULTANT && a.getCandidateSigningStatus() != SigningStatus.SIGNED) {
            throw new IllegalArgumentException("Client must review and sign before consultant signing can proceed.");
        }

        if (party == Party.CANDIDATE && a.getCandidateSigningStatus() == SigningStatus.SIGNED) {
            throw new IllegalArgumentException("Client has already signed this agreement.");
        }

        if (party == Party.CONSULTANT && a.getConsultantSigningStatus() == SigningStatus.SIGNED) {
            throw new IllegalArgumentException("Consultant has already signed this agreement.");
        }

        String callback = frontendBaseUrl + "/api/esign/callback";
        String tx = eSignService.createSigningRequest(a, party, callback);

        Map<String,Object> result = new LinkedHashMap<>();
        result.put("transactionId", tx);
        result.put("authentication", "AADHAAR_OTP");
        result.put("signingMethod", "ELECTRONIC_SIGNATURE");
        result.put("idProofOptional", true);
        result.put("message", "Aadhaar OTP eSign transaction created. Redirect the signer to the authorised provider signing URL.");
        return result;
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

    /**
     * Removes an agreement only from the consultant dashboard.
     * The agreement, PDF, and signing links remain stored and usable.
     */
    @Transactional
    public void deleteForConsultant(UUID id) {
        Agreement a = get(id);
        a.setConsultantDeleted(true);
        repo.save(a);
    }

    private Agreement applySignature(Agreement a, Party party, String tx, String docRef) {
        if (party == Party.CANDIDATE) {
            if (a.getCandidateSigningStatus() == SigningStatus.SIGNED) {
                throw new IllegalArgumentException("Client has already signed this agreement.");
            }
            a.setCandidateSigningStatus(SigningStatus.SIGNED);
            a.setCandidateEsignTransactionId(tx);
            a.setCandidateSignedAt(LocalDateTime.now());
        } else {
            if (a.getCandidateSigningStatus() != SigningStatus.SIGNED) {
                throw new IllegalArgumentException("Client must sign before consultant signing can proceed.");
            }
            if (a.getConsultantSigningStatus() == SigningStatus.SIGNED) {
                throw new IllegalArgumentException("Consultant has already signed this agreement.");
            }
            a.setConsultantSigningStatus(SigningStatus.SIGNED);
            a.setConsultantEsignTransactionId(tx);
            a.setConsultantSignedAt(LocalDateTime.now());
        }

        if (docRef != null && !docRef.isBlank()) a.setSignedDocumentReference(docRef);

        boolean fullySigned = a.isFullySigned();
        if (fullySigned) {
            a.setStatus(AgreementStatus.FULLY_SIGNED);
            a.setCompletedAt(LocalDateTime.now());
        } else {
            a.setStatus(AgreementStatus.AWAITING_CONSULTANT);
        }

        Agreement saved = repo.save(a);

        if (party == Party.CANDIDATE && !fullySigned) {
            emailService.sendConsultantInvitation(saved);
        }
        if (fullySigned) {
            emailService.sendFullyExecuted(saved);
        }

        return saved;
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
