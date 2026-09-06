package com.pvtalent.esign.model;

import javax.persistence.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "agreements")
public class Agreement {
    @Id
    @GeneratedValue(generator = "UUID")
    @org.hibernate.annotations.GenericGenerator(name = "UUID", strategy = "org.hibernate.id.UUIDGenerator")
    private UUID id;

    @Column(nullable=false, unique=true)
    private String agreementNumber;

    private String candidateFullName;
    private String candidateAddress;
    private String candidateIdProofType;
    private String candidateIdProofReference;
    private String candidateEmail;
    private String candidateMobile;
    private String totalFeeAmount;
    private String prePaymentAmount;
    private String balanceAmount;
    private String executionDate;

    @Enumerated(EnumType.STRING)
    private SigningStatus candidateSigningStatus = SigningStatus.PENDING;

    @Enumerated(EnumType.STRING)
    private SigningStatus consultantSigningStatus = SigningStatus.PENDING;

    @Enumerated(EnumType.STRING)
    private AgreementStatus status = AgreementStatus.DRAFT;

    @Column(unique=true)
    private String candidateSigningToken;

    @Column(unique=true)
    private String consultantSigningToken;

    private String candidateEsignTransactionId;
    private String consultantEsignTransactionId;
    private String signedDocumentReference;
    private LocalDateTime candidateSignedAt;
    private LocalDateTime consultantSignedAt;
    private LocalDateTime completedAt;
    private LocalDateTime createdAt;
    private String originalPdfPath;

    @PrePersist
    void created() { createdAt = LocalDateTime.now(); }

    public boolean isFullySigned() {
        return candidateSigningStatus == SigningStatus.SIGNED
            && consultantSigningStatus == SigningStatus.SIGNED;
    }

    public UUID getId(){return id;}
    public String getAgreementNumber(){return agreementNumber;}
    public void setAgreementNumber(String v){agreementNumber=v;}
    public String getCandidateFullName(){return candidateFullName;}
    public void setCandidateFullName(String v){candidateFullName=v;}
    public String getCandidateAddress(){return candidateAddress;}
    public void setCandidateAddress(String v){candidateAddress=v;}
    public String getCandidateIdProofType(){return candidateIdProofType;}
    public void setCandidateIdProofType(String v){candidateIdProofType=v;}
    public String getCandidateIdProofReference(){return candidateIdProofReference;}
    public void setCandidateIdProofReference(String v){candidateIdProofReference=v;}
    public String getCandidateEmail(){return candidateEmail;}
    public void setCandidateEmail(String v){candidateEmail=v;}
    public String getCandidateMobile(){return candidateMobile;}
    public void setCandidateMobile(String v){candidateMobile=v;}
    public String getTotalFeeAmount(){return totalFeeAmount;}
    public void setTotalFeeAmount(String v){totalFeeAmount=v;}
    public String getPrePaymentAmount(){return prePaymentAmount;}
    public void setPrePaymentAmount(String v){prePaymentAmount=v;}
    public String getBalanceAmount(){return balanceAmount;}
    public void setBalanceAmount(String v){balanceAmount=v;}
    public String getExecutionDate(){return executionDate;}
    public void setExecutionDate(String v){executionDate=v;}
    public SigningStatus getCandidateSigningStatus(){return candidateSigningStatus;}
    public void setCandidateSigningStatus(SigningStatus v){candidateSigningStatus=v;}
    public SigningStatus getConsultantSigningStatus(){return consultantSigningStatus;}
    public void setConsultantSigningStatus(SigningStatus v){consultantSigningStatus=v;}
    public AgreementStatus getStatus(){return status;}
    public void setStatus(AgreementStatus v){status=v;}
    public String getCandidateSigningToken(){return candidateSigningToken;}
    public void setCandidateSigningToken(String v){candidateSigningToken=v;}
    public String getConsultantSigningToken(){return consultantSigningToken;}
    public void setConsultantSigningToken(String v){consultantSigningToken=v;}
    public String getCandidateEsignTransactionId(){return candidateEsignTransactionId;}
    public void setCandidateEsignTransactionId(String v){candidateEsignTransactionId=v;}
    public String getConsultantEsignTransactionId(){return consultantEsignTransactionId;}
    public void setConsultantEsignTransactionId(String v){consultantEsignTransactionId=v;}
    public String getSignedDocumentReference(){return signedDocumentReference;}
    public void setSignedDocumentReference(String v){signedDocumentReference=v;}
    public LocalDateTime getCandidateSignedAt(){return candidateSignedAt;}
    public void setCandidateSignedAt(LocalDateTime v){candidateSignedAt=v;}
    public LocalDateTime getConsultantSignedAt(){return consultantSignedAt;}
    public void setConsultantSignedAt(LocalDateTime v){consultantSignedAt=v;}
    public LocalDateTime getCompletedAt(){return completedAt;}
    public void setCompletedAt(LocalDateTime v){completedAt=v;}
    public LocalDateTime getCreatedAt(){return createdAt;}
    public String getOriginalPdfPath(){return originalPdfPath;}
    public void setOriginalPdfPath(String v){originalPdfPath=v;}
}
