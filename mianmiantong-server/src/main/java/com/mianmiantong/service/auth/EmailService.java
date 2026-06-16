package com.mianmiantong.service.auth;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import java.io.UnsupportedEncodingException;
import java.util.Date;
import java.util.Map;

@Slf4j
@Service
public class EmailService {

    private static final String BRAND_NAME = "面面通";
    private static final String BRAND_DESC = "AI 模拟面试平台";

    private static final Map<String, String> ACTION_LABELS = Map.of(
        "register", "账号注册",
        "reset", "密码重置"
    );

    private final JavaMailSender mailSender;
    private final String from;

    public EmailService(JavaMailSender mailSender,
                        @Value("${spring.mail.username}") String from) {
        this.mailSender = mailSender;
        this.from = from;
    }

    public void sendVerificationCode(String to, String type, String code) {
        String actionLabel = ACTION_LABELS.getOrDefault(type, "邮箱验证");
        String subject = BRAND_NAME + "邮箱验证码";
        String text = buildPlainText(actionLabel, code);

        String html = """
            <div style="margin:0;padding:0;background:#f7f7f5;">
              <div style="max-width:520px;margin:0 auto;padding:24px 16px;font-family:-apple-system,BlinkMacSystemFont,'Segoe UI',Arial,sans-serif;color:#1f2933;line-height:1.6;">
                <div style="background:#ffffff;border:1px solid #e7e5df;border-radius:10px;padding:24px;">
                  <p style="margin:0 0 4px;font-size:18px;font-weight:700;color:#151515;">%s</p>
                  <p style="margin:0 0 24px;font-size:13px;color:#6f7478;">%s</p>
                  <p style="margin:0 0 12px;font-size:14px;color:#33383d;">你正在进行%s，请使用以下验证码完成邮箱验证：</p>
                  <p style="margin:0 0 16px;font-size:30px;font-weight:700;letter-spacing:4px;color:#151515;">%s</p>
                  <p style="margin:0;font-size:13px;color:#6f7478;">验证码有效期为 5 分钟。若不是你本人操作，可以忽略这封邮件。</p>
                </div>
                <p style="margin:14px 0 0;text-align:center;font-size:12px;color:#8a8f94;">此邮件由系统自动发送，请勿直接回复。</p>
              </div>
            </div>
            """.formatted(BRAND_NAME, BRAND_DESC, actionLabel, code);

        try {
            MimeMessage message = mailSender.createMimeMessage();
            message.setSentDate(new Date());
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(from, BRAND_NAME);
            helper.setReplyTo(from);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(text, html);
            mailSender.send(message);
            log.info("验证码邮件已发送: to={}, type={}", to, type);
        } catch (MessagingException | UnsupportedEncodingException e) {
            log.error("邮件发送失败: to={}, type={}", to, type, e);
            throw new RuntimeException("邮件发送失败，请稍后再试", e);
        }
    }

    private static String buildPlainText(String actionLabel, String code) {
        return """
            %s邮箱验证码

            你正在进行%s，请使用以下验证码完成邮箱验证：

            %s

            验证码有效期为 5 分钟。若不是你本人操作，可以忽略这封邮件。

            %s
            """.formatted(BRAND_NAME, actionLabel, code, BRAND_DESC);
    }

}
