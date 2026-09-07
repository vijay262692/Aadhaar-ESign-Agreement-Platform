package com.pvtalent.esign.controller;

import com.pvtalent.esign.dto.*;
import com.pvtalent.esign.model.Agreement;
import com.pvtalent.esign.model.Party;
import com.pvtalent.esign.service.AgreementService;
import javax.validation.Valid;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.*;

@RestController
@RequestMapping("/api")
@CrossOrigin
public class AgreementController {
    private final AgreementService service;

    @Value("${app.demo-signing-enabled:false}")
    private boolean demoSigningEnabled;

    public AgreementController(AgreementService service) { this.service = service; }

    @PostMapping("/agreements")
    public Agreement create(@Valid @RequestBody CreateAgreementRequest request) {
        return service.create(request);
    }

    @GetMapping("/agreements")
    public List<Agreement> all() { return service.all(); }

    @GetMapping("/agreements/{id}")
    public Agreement get(@PathVariable UUID id) { return service.get(id); }

    @DeleteMapping("/agreements/{id}/consultant")
    public ResponseEntity<?> deleteForConsultant(@PathVariable UUID id) {
        try {
            service.deleteForConsultant(id);
            return ResponseEntity.ok(Map.of("message", "Agreement removed from consultant dashboard."));
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(Map.of("message", ex.getMessage()));
        }
    }

    @GetMapping("/sign/{party}/{token}")
    public Agreement signingView(@PathVariable String party, @PathVariable String token) {
        return service.getByToken(token, Party.valueOf(party.toUpperCase()));
    }

    /** Public signing-page PDF endpoint. Access is authorized by the secure signing token. */
    @GetMapping("/sign/{party}/{token}/pdf")
    public ResponseEntity<byte[]> signingPdf(@PathVariable String party, @PathVariable String token) throws Exception {
        Agreement agreement = service.getByToken(token, Party.valueOf(party.toUpperCase()));
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=agreement.pdf")
                .body(service.readPdf(agreement.getId()));
    }

    @PostMapping("/agreements/{id}/esign/start")
    public ResponseEntity<?> start(
            @PathVariable UUID id,
            @RequestParam String party,
            @RequestHeader(value = "X-Signing-Token", required = false) String signingToken,
            @RequestBody(required = false) ESignStartRequest request) {

        if (request == null || !request.isConsent()) {
            return ResponseEntity.badRequest().body(
                Map.of("message", "Consent is required before electronic signing.")
            );
        }

        try {
            Party signingParty = Party.valueOf(party.toUpperCase());
            if (signingToken == null || signingToken.isBlank()) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("message", "Secure signing token is required."));
            }

            Agreement tokenAgreement = service.getByToken(signingToken, signingParty);
            if (!id.equals(tokenAgreement.getId())) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(Map.of("message", "Signing token does not match this agreement."));
            }

            return ResponseEntity.ok(
                service.startEsign(tokenAgreement, signingParty, request)
            );
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(
                Map.of("message", ex.getMessage() == null ? "Invalid signing request." : ex.getMessage())
            );
        } catch (NoSuchElementException ex) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("message", "Invalid or expired signing link."));
        }
    }

    @PostMapping("/esign/callback")
    public Agreement callback(@RequestBody CallbackRequest request) {
        return service.callback(request);
    }

    @PostMapping("/agreements/{id}/demo-sign")
    public ResponseEntity<?> demoSign(@PathVariable UUID id, @RequestParam String party) {
        if (!demoSigningEnabled) return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        return ResponseEntity.ok(service.demoSign(id, Party.valueOf(party.toUpperCase())));
    }

    @PostMapping("/agreements/{id}/pdf")
    public Map<String,String> upload(@PathVariable UUID id, @RequestParam MultipartFile file) throws Exception {
        if (file.isEmpty()) throw new IllegalArgumentException("Empty file");
        service.savePdf(id, file.getOriginalFilename() == null ? "agreement.pdf" : file.getOriginalFilename(), file.getBytes());
        return Map.of("message", "PDF uploaded");
    }

    /** Consultant/admin PDF access from the authenticated dashboard. */
    @GetMapping("/agreements/{id}/pdf")
    public ResponseEntity<byte[]> pdf(@PathVariable UUID id) throws Exception {
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=agreement.pdf")
                .body(service.readPdf(id));
    }
}
