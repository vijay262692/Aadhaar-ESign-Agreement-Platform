package com.pvtalent.esign.service;

import java.util.Base64;

public final class AgreementTemplateResource {
    private AgreementTemplateResource() {}

    public static byte[] pdfBytes() {
        return Base64.getDecoder().decode(BASE64);
    }

    private static final String BASE64 =
        "JVBERi0xLjQKJcOkw7zDtsO...";
}
