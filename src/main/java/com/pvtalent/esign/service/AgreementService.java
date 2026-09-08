package com.pvtalent.esign.service;

import com.pvtalent.esign.dto.CallbackRequest;
import com.pvtalent.esign.dto.CreateAgreementRequest;
import com.pvtalent.esign.dto.ESignStartRequest;
import com.pvtalent.esign.model.*;
import com.pvtalent.esign.repository.AgreementRepository;
import org.springframework.beans.factory.annotation.Autowired;
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
    private final AgreementPdfGenerator pdfGenerator;

    @Autowired private AgreementPdfSignatureService pdfSignatureService;

    @Value("${app.frontend-base-url:http://localhost:8080}") private String frontendBaseUrl;
    @Value("${app.upload-dir:./uploads}") private String uploadDir;

    public AgreementService(AgreementRepository repo, ESignService eSignService, EmailService emailService, AgreementPdfGenerator pdfGenerator) {
        this.repo = repo; this.eSignService = eSignService; this.emailService = emailService; this.pdfGenerator = pdfGenerator;
    }
    public List<Agreement> all() { return repo.findAllByConsultantDeletedFalseOrConsultantDeletedIsNullOrderByCreatedAtDesc(); }
    public Agreement get(UUID id) { return repo.findById(id).orElseThrow(() -> new NoSuchElementException("Agreement not found")); }
    public Agreement getByToken(String token, Party party) {
        return (party == Party.CANDIDATE ? repo.findByCandidateSigningToken(token) : repo.findByConsultantSigningToken(token))
            .orElseThrow(() -> new NoSuchElementException("Invalid or expired signing link"));
    }

    public Agreement create(CreateAgreementRequest r) {
        Agreement a = new Agreement();
        a.setAgreementNumber("PVTP/2026-2027/CANDIDATE/" + UUID.randomUUID().toString().substring(0,8).toUpperCase());
        a.setCandidateFullName(r.getCandidateFullName()); a.setCandidateAddress(r.getCandidateAddress());
        a.setCandidateIdProofType(r.getCandidateIdProofType()); a.setCandidateIdProofReference(r.getCandidateIdProofReference());
        a.setTotalFeeAmount(r.getTotalFeeAmount()); a.setPrePaymentAmount(r.getPrePaymentAmount()); a.setBalanceAmount(r.getBalanceAmount());
        a.setExecutionDate(r.getExecutionDate()); a.setCandidateEmail(r.getCandidateEmail()); a.setCandidateMobile(r.getCandidateMobile());
        a.setConsultantEmail(r.getConsultantEmail()); a.setCandidateSigningToken(randomToken()); a.setConsultantSigningToken(randomToken());
        a.setStatus(AgreementStatus.AWAITING_CANDIDATE);
        return repo.save(a);
    }

    public void sendClientInvitation(UUID id) {
        Agreement a = get(id);
        ensurePdfAvailable(a);
        if (a.getCandidateEmail() == null || a.getCandidateEmail().isBlank()) throw new IllegalArgumentException("Client email is required before sending the agreement.");
        if (!emailService.sendClientInvitation(a)) throw new IllegalStateException("Agreement was created, but the client email could not be sent.");
    }

    public Map<String,Object> links(Agreement a) { return Map.of("candidateSigningUrl", frontendBaseUrl + "/sign.html?party=candidate&token=" + a.getCandidateSigningToken(), "consultantSigningUrl", frontendBaseUrl + "/sign.html?party=consultant&token=" + a.getConsultantSigningToken()); }
    public Map<String,Object> startEsign(Agreement a, Party party) { return startEsign(a, party, new ESignStartRequest()); }
    public Map<String,Object> startEsign(Agreement a, Party party, ESignStartRequest request) {
        if (!request.isConsent()) throw new IllegalArgumentException("Consent is required before electronic signing.");
        ensurePdfAvailable(a);
        if (party == Party.CONSULTANT && a.getCandidateSigningStatus() != SigningStatus.SIGNED) throw new IllegalArgumentException("Client must review and sign before consultant signing can proceed.");
        if (party == Party.CANDIDATE && a.getCandidateSigningStatus() == SigningStatus.SIGNED) throw new IllegalArgumentException("Client has already signed this agreement.");
        if (party == Party.CONSULTANT && a.getConsultantSigningStatus() == SigningStatus.SIGNED) throw new IllegalArgumentException("Consultant has already signed this agreement.");
        String tx = eSignService.createSigningRequest(a, party, frontendBaseUrl + "/api/esign/callback");
        Map<String,Object> result = new LinkedHashMap<>(); result.put("transactionId", tx); result.put("authentication", "AADHAAR_OTP"); result.put("signingMethod", "ELECTRONIC_SIGNATURE"); result.put("idProofOptional", true); result.put("message", "Aadhaar OTP eSign transaction created. Redirect the signer to the authorised provider signing URL."); return result;
    }

    @Transactional public Agreement acceptAgreement(Agreement a, Party party) { return applySignature(a, party, "ACCEPTED-" + UUID.randomUUID(), "ACCEPTANCE_BUTTON"); }
    @Transactional public Agreement callback(CallbackRequest r) { Agreement a=get(UUID.fromString(r.getAgreementId())); if(!r.isSuccessful() || !eSignService.verifyCallback(r.getTransactionId(),r.getProviderAuditReference())) throw new IllegalArgumentException("eSign callback verification failed"); return applySignature(a,Party.valueOf(r.getParty().toUpperCase()),r.getTransactionId(),r.getSignedDocumentReference()); }
    @Transactional public Agreement demoSign(UUID id, Party party) { return applySignature(get(id),party,"DEMO-"+UUID.randomUUID(),null); }
    @Transactional public void deleteForConsultant(UUID id) { Agreement a=get(id); a.setConsultantDeleted(true); repo.save(a); }

    private Agreement applySignature(Agreement a, Party party, String tx, String docRef) {
        if (party == Party.CANDIDATE) {
            if (a.getCandidateSigningStatus() == SigningStatus.SIGNED) throw new IllegalArgumentException("Client has already signed this agreement.");
            a.setCandidateSigningStatus(SigningStatus.SIGNED); a.setCandidateEsignTransactionId(tx); a.setCandidateSignedAt(LocalDateTime.now());
        } else {
            if (a.getCandidateSigningStatus() != SigningStatus.SIGNED) throw new IllegalArgumentException("Client must sign before consultant signing can proceed.");
            if (a.getConsultantSigningStatus() == SigningStatus.SIGNED) throw new IllegalArgumentException("Consultant has already signed this agreement.");
            a.setConsultantSigningStatus(SigningStatus.SIGNED); a.setConsultantEsignTransactionId(tx); a.setConsultantSignedAt(LocalDateTime.now());
        }
        if (docRef != null && !docRef.isBlank()) a.setSignedDocumentReference(docRef);
        boolean fullySigned=a.isFullySigned();
        if(fullySigned){a.setStatus(AgreementStatus.FULLY_SIGNED);a.setCompletedAt(LocalDateTime.now());} else a.setStatus(AgreementStatus.AWAITING_CONSULTANT);
        Agreement saved=repo.save(a);
        try { pdfSignatureService.appendSignatureRecord(saved, party); } catch (Exception ignored) { }
        if(party==Party.CANDIDATE && !fullySigned){ ensurePdfAvailable(saved); emailService.sendConsultantInvitation(saved); }
        if(fullySigned){ ensurePdfAvailable(saved); emailService.sendFullyExecuted(saved); }
        return saved;
    }

    public void savePdf(UUID id,String filename,byte[] bytes)throws Exception{ Agreement a=get(id); String lower=filename==null?"":filename.toLowerCase(Locale.ROOT); if(!lower.endsWith(".pdf"))throw new IllegalArgumentException("Please upload the final agreement as a PDF."); if(bytes==null||bytes.length==0)throw new IllegalArgumentException("Empty PDF file"); Path dir=Paths.get(uploadDir).toAbsolutePath().normalize(); Files.createDirectories(dir); String safe=UUID.randomUUID()+"-"+filename.replaceAll("[^a-zA-Z0-9._-]","_"); Path out=dir.resolve(safe); Files.write(out,bytes,StandardOpenOption.CREATE_NEW); a.setOriginalPdfPath(out.toString()); repo.save(a); }

    public synchronized byte[] readPdf(UUID id)throws Exception{
        Agreement a=get(id);
        if(a.getOriginalPdfPath()!=null&&!a.getOriginalPdfPath().isBlank()){
            Path path=Paths.get(a.getOriginalPdfPath());
            if(Files.exists(path)) return Files.readAllBytes(path);
        }
        return rebuildMissingPdf(a);
    }

    private byte[] rebuildMissingPdf(Agreement a) throws Exception {
        String generated=pdfGenerator.generate(a,uploadDir);
        a.setOriginalPdfPath(generated);
        repo.save(a);
        if(a.getCandidateSigningStatus()==SigningStatus.SIGNED) pdfSignatureService.appendSignatureRecord(a,Party.CANDIDATE);
        if(a.getConsultantSigningStatus()==SigningStatus.SIGNED) pdfSignatureService.appendSignatureRecord(a,Party.CONSULTANT);
        Path finalPath=Paths.get(generated);
        return Files.readAllBytes(finalPath);
    }

    private void ensurePdfAvailable(Agreement a) {
        try {
            if(a.getOriginalPdfPath()==null||a.getOriginalPdfPath().isBlank()||!Files.exists(Paths.get(a.getOriginalPdfPath()))) rebuildMissingPdf(a);
        } catch(Exception ex) { throw new IllegalStateException("Agreement PDF could not be generated: "+ex.getMessage(), ex); }
    }

    private String randomToken(){return UUID.randomUUID().toString().replace("-","")+UUID.randomUUID().toString().replace("-","");}
}
