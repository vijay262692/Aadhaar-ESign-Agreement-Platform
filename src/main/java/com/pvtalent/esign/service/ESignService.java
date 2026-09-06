package com.pvtalent.esign.service;

import com.pvtalent.esign.model.Agreement;
import com.pvtalent.esign.model.Party;

public interface ESignService {
    String createSigningRequest(Agreement agreement, Party party, String callbackUrl);
    boolean verifyCallback(String transactionId, String providerAuditReference);
}
