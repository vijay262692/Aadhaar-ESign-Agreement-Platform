package com.pvtalent.esign.dto;

public class CallbackRequest {
    private String agreementId;
    private String party;
    private String transactionId;
    private String signedDocumentReference;
    private boolean successful;
    private String providerAuditReference;

    public String getAgreementId(){return agreementId;}
    public void setAgreementId(String v){agreementId=v;}
    public String getParty(){return party;}
    public void setParty(String v){party=v;}
    public String getTransactionId(){return transactionId;}
    public void setTransactionId(String v){transactionId=v;}
    public String getSignedDocumentReference(){return signedDocumentReference;}
    public void setSignedDocumentReference(String v){signedDocumentReference=v;}
    public boolean isSuccessful(){return successful;}
    public void setSuccessful(boolean v){successful=v;}
    public String getProviderAuditReference(){return providerAuditReference;}
    public void setProviderAuditReference(String v){providerAuditReference=v;}
}
