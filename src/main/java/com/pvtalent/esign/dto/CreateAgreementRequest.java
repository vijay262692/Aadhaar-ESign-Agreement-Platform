package com.pvtalent.esign.dto;

import javax.validation.constraints.Email;
import javax.validation.constraints.NotBlank;

public class CreateAgreementRequest {
    @NotBlank private String candidateFullName;
    private String candidateAddress;
    private String candidateIdProofType;
    private String candidateIdProofReference;
    @NotBlank private String totalFeeAmount;
    @NotBlank private String prePaymentAmount;
    @NotBlank private String balanceAmount;
    @NotBlank private String executionDate;
    @NotBlank @Email private String candidateEmail;
    @NotBlank private String candidateMobile;
    @Email private String consultantEmail;

    public String getCandidateFullName(){return candidateFullName;}
    public void setCandidateFullName(String v){candidateFullName=v;}
    public String getCandidateAddress(){return candidateAddress;}
    public void setCandidateAddress(String v){candidateAddress=v;}
    public String getCandidateIdProofType(){return candidateIdProofType;}
    public void setCandidateIdProofType(String v){candidateIdProofType=v;}
    public String getCandidateIdProofReference(){return candidateIdProofReference;}
    public void setCandidateIdProofReference(String v){candidateIdProofReference=v;}
    public String getTotalFeeAmount(){return totalFeeAmount;}
    public void setTotalFeeAmount(String v){totalFeeAmount=v;}
    public String getPrePaymentAmount(){return prePaymentAmount;}
    public void setPrePaymentAmount(String v){prePaymentAmount=v;}
    public String getBalanceAmount(){return balanceAmount;}
    public void setBalanceAmount(String v){balanceAmount=v;}
    public String getExecutionDate(){return executionDate;}
    public void setExecutionDate(String v){executionDate=v;}
    public String getCandidateEmail(){return candidateEmail;}
    public void setCandidateEmail(String v){candidateEmail=v;}
    public String getCandidateMobile(){return candidateMobile;}
    public void setCandidateMobile(String v){candidateMobile=v;}
    public String getConsultantEmail(){return consultantEmail;}
    public void setConsultantEmail(String v){consultantEmail=v;}
}
