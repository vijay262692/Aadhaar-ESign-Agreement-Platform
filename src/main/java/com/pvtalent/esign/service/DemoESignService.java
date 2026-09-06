package com.pvtalent.esign.service;

import com.pvtalent.esign.model.Agreement;
import com.pvtalent.esign.model.Party;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class DemoESignService implements ESignService {
    @Override
    public String createSigningRequest(Agreement agreement, Party party, String callbackUrl) {
        return "DEMO-" + party.name() + "-" + UUID.randomUUID();
    }

    @Override
    public boolean verifyCallback(String transactionId, String providerAuditReference) {
        return transactionId != null && !transactionId.isBlank();
    }
}
