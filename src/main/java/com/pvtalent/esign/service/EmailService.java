package com.pvtalent.esign.service;

import com.pvtalent.esign.model.Agreement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import javax.mail.internet.MimeMessage;

@Service
public class EmailService {
    private static final Logger log = LoggerFactory.getLogger(EmailService.class);

    private final ObjectProvider<JavaMailSender> mailSenderProvider;

    @Value("${app.frontend-base-url:http://localhost:8080}")
    private String frontendBaseUrl;

    @Value("${app.mail-from:}")
    private String mailFrom;

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

        JavaMailSender mailSender = mailSenderProvider.getIfAvailable();
        if (mailSender == null) {
            log.warn("Email is not configured. Set MAIL_HOST/MAIL_USERNAME/MAIL_PASSWORD on the deployment.");
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
            log.info("Agreement email sent to {}", to);
            return true;
        } catch (Exception ex) {
            log.error("Unable to send agreement email to {}", to, ex);
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
