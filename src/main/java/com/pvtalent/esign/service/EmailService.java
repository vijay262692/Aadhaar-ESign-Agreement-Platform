package com.pvtalent.esign.service;

import com.pvtalent.esign.model.Agreement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import javax.mail.internet.MimeMessage;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class EmailService {
    private static final Logger log = LoggerFactory.getLogger(EmailService.class);

    private final ObjectProvider<JavaMailSender> mailSenderProvider;
    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${app.frontend-base-url:http://localhost:8080}")
    private String frontendBaseUrl;

    @Value("${app.mail-from:}")
    private String mailFrom;

    @Value("${app.email-provider:BREVO}")
    private String emailProvider;

    @Value("${app.brevo-api-url:https://api.brevo.com/v3/smtp/email}")
    private String brevoApiUrl;

    @Value("${app.brevo-api-key:}")
    private String brevoApiKey;

    public EmailService(ObjectProvider<JavaMailSender> mailSenderProvider) {
        this.mailSenderProvider = mailSenderProvider;
    }

    public boolean sendClientInvitation(Agreement a) {
        String link = frontendBaseUrl + "/sign.html?party=candidate&token=" + a.getCandidateSigningToken();
        return send(a.getCandidateEmail(),
                "PV Talent Partners - Agreement " + a.getAgreementNumber() + " requires your signature",
                "Dear " + safe(a.getCandidateFullName()) + ","
                    + "<br><br>PV Talent Partners has prepared an agreement for your review and signature."
                    + "<br><br><b>Agreement:</b> " + safe(a.getAgreementNumber())
                    + "<br><b>Total Fee:</b> " + safe(a.getTotalFeeAmount())
                    + "<br><br>Please review the agreement and complete your electronic signature using the secure link below."
                    + "<br><br><a href=\"" + link + "\">Review & Sign Agreement</a>"
                    + "<br><br>This agreement becomes fully executed only after both the client and consultant have signed."
                    + "<br><br>Regards,<br>PV Talent Partners");
    }

    public boolean sendConsultantInvitation(Agreement a) {
        String link = frontendBaseUrl + "/sign.html?party=consultant&token=" + a.getConsultantSigningToken();
        return send(a.getConsultantEmail(),
                "PV Talent Partners - Agreement " + a.getAgreementNumber() + " is ready for your signature",
                "The client has reviewed and signed the agreement."
                    + "<br><br><b>Agreement:</b> " + safe(a.getAgreementNumber())
                    + "<br><b>Client:</b> " + safe(a.getCandidateFullName())
                    + "<br><br>Please review the agreement and complete your signature using the secure link below."
                    + "<br><br><a href=\"" + link + "\">Review & Sign as Consultant</a>"
                    + "<br><br>The agreement becomes fully executed after your signature is recorded."
                    + "<br><br>Regards,<br>PV Talent Partners");
    }

    public void sendFullyExecuted(Agreement a) {
        String link = frontendBaseUrl + "/sign.html?party=candidate&token=" + a.getCandidateSigningToken();
        String subject = "PV Talent Partners - Agreement " + a.getAgreementNumber() + " fully executed";
        String body = "The agreement has now been signed by both parties and is fully executed."
                + "<br><br><b>Agreement:</b> " + safe(a.getAgreementNumber())
                + "<br><b>Client:</b> " + safe(a.getCandidateFullName())
                + "<br><br><a href=\"" + link + "\">View Agreement</a>"
                + "<br><br>Regards,<br>PV Talent Partners";

        send(a.getCandidateEmail(), subject, body);
        if (a.getConsultantEmail() != null && !a.getConsultantEmail().isBlank()) {
            send(a.getConsultantEmail(), subject, body);
        }
    }

    private boolean send(String to, String subject, String html) {
        if (to == null || to.isBlank()) {
            log.warn("Agreement email skipped because recipient email is empty.");
            return false;
        }

        if ("BREVO".equalsIgnoreCase(emailProvider)) {
            return sendViaBrevo(to, subject, html);
        }

        return sendViaSmtp(to, subject, html);
    }

    private boolean sendViaBrevo(String to, String subject, String html) {
        if (brevoApiKey == null || brevoApiKey.isBlank()) {
            log.warn("Brevo email is not configured. Set BREVO_API_KEY on the deployment.");
            return false;
        }
        if (mailFrom == null || mailFrom.isBlank()) {
            log.warn("Email sender is not configured. Set MAIL_FROM on the deployment.");
            return false;
        }

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setAccept(List.of(MediaType.APPLICATION_JSON));
            headers.set("api-key", brevoApiKey);

            Map<String, Object> sender = new LinkedHashMap<>();
            sender.put("name", "PV Talent Partners");
            sender.put("email", mailFrom);

            Map<String, Object> recipient = new LinkedHashMap<>();
            recipient.put("email", to);

            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("sender", sender);
            payload.put("to", List.of(recipient));
            payload.put("subject", subject);
            payload.put("htmlContent", html);

            ResponseEntity<String> response = restTemplate.postForEntity(
                    brevoApiUrl,
                    new HttpEntity<>(payload, headers),
                    String.class);

            if (response.getStatusCode().is2xxSuccessful()) {
                log.info("Agreement email sent to {} via Brevo", to);
                return true;
            }

            log.error("Brevo email failed for {} with status {}", to, response.getStatusCodeValue());
            return false;
        } catch (Exception ex) {
            log.error("Unable to send agreement email to {} via Brevo", to, ex);
            return false;
        }
    }

    private boolean sendViaSmtp(String to, String subject, String html) {
        JavaMailSender mailSender = mailSenderProvider.getIfAvailable();
        if (mailSender == null) {
            log.warn("SMTP email is not configured.");
            return false;
        }

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, false, "UTF-8");
            if (mailFrom != null && !mailFrom.isBlank()) helper.setFrom(mailFrom);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(html, true);
            mailSender.send(message);
            log.info("Agreement email sent to {} via SMTP", to);
            return true;
        } catch (Exception ex) {
            log.error("Unable to send agreement email to {} via SMTP", to, ex);
            return false;
        }
    }

    private String safe(String value) {
        if (value == null) return "";
        return value.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }
}
