package com.pvtalent.esign.dto;

public class SignRequest {
    private String party;
    private String transactionId;
    private String signedDocumentReference;

    public String getParty(){return party;}
    public void setParty(String v){party=v;}
    public String getTransactionId(){return transactionId;}
    public void setTransactionId(String v){transactionId=v;}
    public String getSignedDocumentReference(){return signedDocumentReference;}
    public void setSignedDocumentReference(String v){signedDocumentReference=v;}
}
