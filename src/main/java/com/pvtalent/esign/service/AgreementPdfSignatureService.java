package com.pvtalent.esign.service;

import com.pvtalent.esign.model.Agreement;
import com.pvtalent.esign.model.Party;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.springframework.stereotype.Service;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Service
public class AgreementPdfSignatureService {
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("dd MMM yyyy, hh:mm a 'IST'");

    public void appendSignatureRecord(Agreement agreement, Party party) throws Exception {
        if (agreement.getOriginalPdfPath() == null || agreement.getOriginalPdfPath().isBlank()) return;
        Path path = Paths.get(agreement.getOriginalPdfPath());
        if (!Files.exists(path)) return;

        String name = party == Party.CANDIDATE ? agreement.getCandidateFullName() : "BaluRaju P V";
        LocalDateTime signedAt = party == Party.CANDIDATE ? agreement.getCandidateSignedAt() : agreement.getConsultantSignedAt();
        if (signedAt == null) signedAt = LocalDateTime.now();

        try (PDDocument doc = PDDocument.load(path.toFile())) {
            if (doc.getNumberOfPages() < 3) return;
            PDPage page = doc.getPage(2);
            float x = party == Party.CANDIDATE ? 303f : 60.5f;
            float y = PDRectangle.A4.getHeight() - 635f;

            try (PDPageContentStream cs = new PDPageContentStream(doc, page, PDPageContentStream.AppendMode.APPEND, true, true)) {
                text(cs, "Signed through PV Talent Partners Agreement", x, y, 8.5f, PDType1Font.HELVETICA_BOLD);
                text(cs, "eSign by " + safe(name), x, y - 13f, 8.5f, PDType1Font.HELVETICA);
                text(cs, signedAt.format(FORMATTER), x, y - 26f, 8.5f, PDType1Font.HELVETICA);
            }
            doc.save(path.toFile());
        }
    }

    private void text(PDPageContentStream cs, String value, float x, float y, float size, PDType1Font font) throws Exception {
        cs.beginText();
        cs.setFont(font, size);
        cs.newLineAtOffset(x, y);
        cs.showText(safePdf(value));
        cs.endText();
    }

    private String safe(String value) { return value == null || value.isBlank() ? "Unknown" : value.trim(); }
    private String safePdf(String value) { return safe(value).replace("₹", "Rs."); }
}
