package com.pvtalent.esign.controller;

import com.pvtalent.esign.dto.*;
import com.pvtalent.esign.model.Agreement;
import com.pvtalent.esign.model.Party;
import com.pvtalent.esign.service.AgreementPdfGenerator;
import com.pvtalent.esign.service.AgreementService;
import javax.validation.Valid;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;

@RestController
@RequestMapping("/api")
@CrossOrigin
public class AgreementController {
    private final AgreementService service; private final AgreementPdfGenerator pdfGenerator;
    @Value("${app.demo-signing-enabled:false}") private boolean demoSigningEnabled;
    @Value("${app.upload-dir:./uploads}") private String uploadDir;
    public AgreementController(AgreementService service, AgreementPdfGenerator pdfGenerator){this.service=service;this.pdfGenerator=pdfGenerator;}

    @PostMapping("/agreements")
    public Agreement create(@Valid @RequestBody CreateAgreementRequest request) throws Exception {
        Agreement a=service.create(request);
        String generated=pdfGenerator.generate(a,uploadDir);
        service.savePdf(a.getId(),"agreement.pdf",Files.readAllBytes(Paths.get(generated)));
        return service.get(a.getId());
    }
    @PostMapping("/agreements/{id}/send") public ResponseEntity<?> send(@PathVariable UUID id){try{service.sendClientInvitation(id);return ResponseEntity.ok(Map.of("message","Agreement generated and client invitation sent."));}catch(IllegalArgumentException ex){return ResponseEntity.badRequest().body(Map.of("message",ex.getMessage()));}catch(IllegalStateException ex){return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body(Map.of("message",ex.getMessage()));}}
    @GetMapping("/agreements") public List<Agreement> all(){return service.all();}
    @GetMapping("/agreements/{id}") public Agreement get(@PathVariable UUID id){return service.get(id);}
    @DeleteMapping("/agreements/{id}/consultant") public ResponseEntity<?> deleteForConsultant(@PathVariable UUID id){try{service.deleteForConsultant(id);return ResponseEntity.ok(Map.of("message","Agreement removed from consultant dashboard."));}catch(IllegalArgumentException ex){return ResponseEntity.badRequest().body(Map.of("message",ex.getMessage()));}}
    @GetMapping("/sign/{party}/{token}") public Agreement signingView(@PathVariable String party,@PathVariable String token){return service.getByToken(token,Party.valueOf(party.toUpperCase()));}
    @GetMapping("/sign/{party}/{token}/pdf") public ResponseEntity<byte[]> signingPdf(@PathVariable String party,@PathVariable String token)throws Exception{Agreement agreement=service.getByToken(token,Party.valueOf(party.toUpperCase()));return ResponseEntity.ok().contentType(MediaType.APPLICATION_PDF).header(HttpHeaders.CONTENT_DISPOSITION,"inline; filename=agreement.pdf").body(service.readPdf(agreement.getId()));}

    @PostMapping("/agreements/{id}/accept")
    public ResponseEntity<?> accept(@PathVariable UUID id,@RequestParam String party,@RequestHeader(value="X-Signing-Token",required=false) String signingToken,@RequestBody(required=false) Map<String,Object> body){
        try{
            Party signingParty=Party.valueOf(party.toUpperCase());
            if(signingToken==null||signingToken.isBlank())return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("message","Secure signing token is required."));
            Agreement tokenAgreement=service.getByToken(signingToken,signingParty);
            if(!id.equals(tokenAgreement.getId()))return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("message","Signing token does not match this agreement."));
            if(body==null || !Boolean.TRUE.equals(body.get("consent")))return ResponseEntity.badRequest().body(Map.of("message","Please accept the agreement before signing."));
            return ResponseEntity.ok(service.acceptAgreement(tokenAgreement,signingParty));
        }catch(IllegalArgumentException ex){return ResponseEntity.badRequest().body(Map.of("message",ex.getMessage()==null?"Invalid acceptance request.":ex.getMessage()));}catch(NoSuchElementException ex){return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("message","Invalid or expired signing link."));}
    }

    @PostMapping("/agreements/{id}/esign/start") public ResponseEntity<?> start(@PathVariable UUID id,@RequestParam String party,@RequestHeader(value="X-Signing-Token",required=false) String signingToken,@RequestBody(required=false) ESignStartRequest request){if(request==null||!request.isConsent())return ResponseEntity.badRequest().body(Map.of("message","Consent is required before electronic signing."));try{Party signingParty=Party.valueOf(party.toUpperCase());if(signingToken==null||signingToken.isBlank())return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("message","Secure signing token is required."));Agreement tokenAgreement=service.getByToken(signingToken,signingParty);if(!id.equals(tokenAgreement.getId()))return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("message","Signing token does not match this agreement."));return ResponseEntity.ok(service.startEsign(tokenAgreement,signingParty,request));}catch(IllegalArgumentException ex){return ResponseEntity.badRequest().body(Map.of("message",ex.getMessage()==null?"Invalid signing request.":ex.getMessage()));}catch(NoSuchElementException ex){return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("message","Invalid or expired signing link."));}}
    @PostMapping("/esign/callback") public Agreement callback(@RequestBody CallbackRequest request){return service.callback(request);}
    @PostMapping("/agreements/{id}/demo-sign") public ResponseEntity<?> demoSign(@PathVariable UUID id,@RequestParam String party,@RequestHeader(value="X-Signing-Token",required=false) String signingToken){if(!demoSigningEnabled)return ResponseEntity.status(HttpStatus.NOT_FOUND).build();try{Party signingParty=Party.valueOf(party.toUpperCase());if(signingToken==null||signingToken.isBlank())return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("message","Secure signing token is required."));Agreement tokenAgreement=service.getByToken(signingToken,signingParty);if(!id.equals(tokenAgreement.getId()))return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("message","Signing token does not match this agreement."));return ResponseEntity.ok(service.demoSign(id,signingParty));}catch(IllegalArgumentException ex){return ResponseEntity.badRequest().body(Map.of("message",ex.getMessage()));}catch(NoSuchElementException ex){return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("message","Invalid or expired signing link."));}}
    @GetMapping("/agreements/{id}/pdf") public ResponseEntity<byte[]> pdf(@PathVariable UUID id)throws Exception{return ResponseEntity.ok().contentType(MediaType.APPLICATION_PDF).header(HttpHeaders.CONTENT_DISPOSITION,"inline; filename=agreement.pdf").body(service.readPdf(id));}
}
