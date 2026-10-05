package com.world.i2v.i2vauthapi.service;

import com.world.i2v.i2vauthapi.entity.Notification;
import com.world.i2v.i2vauthapi.entity.User;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
public class EmailService {

    private final JavaMailSender mailSender;
    private final TemplateEngine templateEngine;

    @Value("${app.mail.from:i2v.consulting.helpdesk@gmail.com}")
    private String fromEmail;

    @Value("${app.base-url:http://localhost:3000}")
    private String baseUrl;

    public EmailService(JavaMailSender mailSender, TemplateEngine templateEngine) {
        this.mailSender = mailSender;
        this.templateEngine = templateEngine;
    }

    @Async
    public void sendWelcomeEmail(User user) {
        try {
            Context context = new Context();
            context.setVariable("name", user.getFullName());
            context.setVariable("email", user.getEmail());
            context.setVariable("baseUrl", baseUrl);
            context.setVariable("year", java.time.Year.now().getValue());

            String htmlContent = templateEngine.process("email/welcome", context);

            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(fromEmail);
            helper.setTo(user.getEmail());
            helper.setSubject("🎉 Welcome to I2V Auth System!");
            helper.setText(htmlContent, true);

            mailSender.send(message);
            log.info("Welcome email sent successfully to: {}", user.getEmail());
        } catch (MessagingException e) {
            log.error("Failed to send welcome email to: {}", user.getEmail(), e);
        }
    }

    @Async
    public void sendPasswordResetEmail(User user, String token) {
        try {
            String resetLink = baseUrl + "/reset-password?token=" + token;

            Context context = new Context();
            context.setVariable("name", user.getFullName());
            context.setVariable("resetLink", resetLink);
            context.setVariable("expiryHours", 24);
            context.setVariable("baseUrl", baseUrl);
            context.setVariable("year", java.time.Year.now().getValue());

            String htmlContent = templateEngine.process("email/password-reset", context);

            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(fromEmail);
            helper.setTo(user.getEmail());
            helper.setSubject("🔐 Password Reset Request - I2V Auth System");
            helper.setText(htmlContent, true);

            mailSender.send(message);
            log.info("Password reset email sent successfully to: {}", user.getEmail());
        } catch (MessagingException e) {
            log.error("Failed to send password reset email to: {}", user.getEmail(), e);
        }
    }

    @Async
    public void sendAccountDeactivationEmail(User user, String reason) {
        try {
            Context context = new Context();
            context.setVariable("name", user.getFullName());
            context.setVariable("reason", reason != null ? reason : "No reason provided");
            context.setVariable("date", java.time.LocalDate.now().toString());
            context.setVariable("baseUrl", baseUrl);
            context.setVariable("year", java.time.Year.now().getValue());

            String htmlContent = templateEngine.process("email/account-deactivated", context);

            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(fromEmail);
            helper.setTo(user.getEmail());
            helper.setSubject("⚠️ Account Deactivated - I2V Auth System");
            helper.setText(htmlContent, true);

            mailSender.send(message);
            log.info("Account deactivation email sent to: {}", user.getEmail());
        } catch (MessagingException e) {
            log.error("Failed to send deactivation email to: {}", user.getEmail(), e);
        }
    }

    @Async
    public void sendAccountReactivationEmail(User user, String reason) {
        try {
            Context context = new Context();
            context.setVariable("name", user.getFullName());
            context.setVariable("reason", reason != null ? reason : "No reason provided");
            context.setVariable("date", java.time.LocalDate.now().toString());
            context.setVariable("baseUrl", baseUrl);
            context.setVariable("year", java.time.Year.now().getValue());

            String htmlContent = templateEngine.process("email/account-reactivated", context);

            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(fromEmail);
            helper.setTo(user.getEmail());
            helper.setSubject("✅ Account Reactivated - I2V Auth System");
            helper.setText(htmlContent, true);

            mailSender.send(message);
            log.info("Account reactivation email sent to: {}", user.getEmail());
        } catch (MessagingException e) {
            log.error("Failed to send reactivation email to: {}", user.getEmail(), e);
        }
    }

    @Async
    public void sendNotificationEmail(Notification notification) {
        try {
            Context context = new Context();
            context.setVariable("title", notification.getTitle());
            context.setVariable("category", notification.getCategory().name());
            context.setVariable("priority", notification.getPriority().name());
            context.setVariable("content", notification.getContent());
            context.setVariable("actionUrl", notification.getActionUrl());

            String htmlContent = templateEngine.process("email/notification", context);

            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(fromEmail);
            helper.setTo(notification.getUser().getEmail());
            helper.setSubject(notification.getTitle());
            helper.setText(htmlContent, true);

            mailSender.send(message);
            log.info("Notification email sent successfully to: {}", notification.getUser().getEmail());
        } catch (MessagingException e) {
            log.error("Failed to send notification email", e);
        }
    }

    @Async
    public void sendCustomHtmlEmail(String to, String subject, String templateName,
                                    Map<String, Object> variables) {
        try {
            Context context = new Context();
            variables.forEach(context::setVariable);
            context.setVariable("baseUrl", baseUrl);
            context.setVariable("year", java.time.Year.now().getValue());

            String htmlContent = templateEngine.process(templateName, context);

            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(fromEmail);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(htmlContent, true);

            mailSender.send(message);
            log.info("Custom HTML email sent successfully to: {}", to);
        } catch (MessagingException e) {
            log.error("Failed to send custom HTML email to: {}", to, e);
        }
    }

    @Async
    public void sendSimpleEmail(String to, String subject, String text) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(to);
            message.setSubject(subject);
            message.setText(text);
            mailSender.send(message);
            log.info("Simple email sent successfully to: {}", to);
        } catch (Exception e) {
            log.error("Failed to send simple email to: {}", to, e);
        }
    }

    @Async
    public void sendEmployeeStatusChangeEmail(User user, String oldStatus, String newStatus, String reason) {
        Map<String, Object> variables = new HashMap<>();
        variables.put("name", user.getFullName());
        variables.put("oldStatus", oldStatus);
        variables.put("newStatus", newStatus);
        variables.put("reason", reason != null ? reason : "No reason provided");
        variables.put("date", java.time.LocalDate.now().toString());

        String subject = "Employment Status Updated - I2V Auth System";
        sendCustomHtmlEmail(user.getEmail(), subject, "email/employment-status-change", variables);
    }

    @Async
    public void sendLeaveApprovalEmail(User user, String status, String leaveType,
                                       String startDate, String endDate, String remarks) {
        try {
            Context context = new Context();
            context.setVariable("name", user.getFullName());
            context.setVariable("status", status);
            context.setVariable("leaveType", leaveType);
            context.setVariable("startDate", startDate);
            context.setVariable("endDate", endDate);
            context.setVariable("remarks", remarks != null ? remarks : "No remarks");
            context.setVariable("totalDays", calculateDays(startDate, endDate));
            context.setVariable("isApproved", "APPROVED".equals(status));
            context.setVariable("baseUrl", baseUrl);
            context.setVariable("year", java.time.Year.now().getValue());

            String htmlContent = templateEngine.process("email/leave-notification", context);
            String subject = "Leave Request " + status + " - I2V Auth System";

            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(fromEmail);
            helper.setTo(user.getEmail());
            helper.setSubject(subject);
            helper.setText(htmlContent, true);

            mailSender.send(message);
            log.info("Leave {} email sent successfully to: {}", status.toLowerCase(), user.getEmail());
        } catch (MessagingException e) {
            log.error("Failed to send leave approval email to: {}", user.getEmail(), e);
        }
    }

    @Async
    public void sendAttendanceAlert(User user, String alertType, String message) {
        Map<String, Object> variables = new HashMap<>();
        variables.put("name", user.getFullName());
        variables.put("alertType", alertType);
        variables.put("message", message);

        sendCustomHtmlEmail(user.getEmail(),
                "Attendance Alert - I2V Auth System",
                "email/attendance-alert",
                variables);
    }

    private long calculateDays(String startDate, String endDate) {
        try {
            LocalDate start = LocalDate.parse(startDate);
            LocalDate end = LocalDate.parse(endDate);
            return ChronoUnit.DAYS.between(start, end) + 1;
        } catch (Exception e) {
            return 1;
        }
    }

    @Async
    public void sendResignationNotificationEmail(User user, String place, String reason,
                                                 String lastWorkingDate, String comments, String... managerEmails) {
        try {
            Context context = new Context();
            context.setVariable("name", user.getFullName());
            context.setVariable("place", place);
            context.setVariable("reason", reason);
            context.setVariable("lastWorkingDate", lastWorkingDate);
            context.setVariable("comments", comments != null ? comments : "No additional comments");
            context.setVariable("submittedDate", java.time.LocalDate.now().toString());
            context.setVariable("baseUrl", baseUrl);
            context.setVariable("year", java.time.Year.now().getValue());

            String htmlContent = templateEngine.process("email/resignation-notification", context);

            // Send email to employee
            sendMimeEmail(user.getEmail(), "📋 Resignation Submitted - I2V Auth System", htmlContent);
            
            // Send email to manager(s) if assigned
            if (managerEmails != null && managerEmails.length > 0) {
                for (String managerEmail : managerEmails) {
                    if (managerEmail != null && !managerEmail.trim().isEmpty()) {
                        Context managerContext = new Context();
                        managerContext.setVariable("name", "Manager");
                        managerContext.setVariable("employeeName", user.getFullName());
                        managerContext.setVariable("place", place);
                        managerContext.setVariable("reason", reason);
                        managerContext.setVariable("lastWorkingDate", lastWorkingDate);
                        managerContext.setVariable("comments", comments != null ? comments : "No additional comments");
                        managerContext.setVariable("submittedDate", java.time.LocalDate.now().toString());
                        managerContext.setVariable("baseUrl", baseUrl);
                        managerContext.setVariable("year", java.time.Year.now().getValue());
                        
                        String managerHtmlContent = templateEngine.process("email/resignation-notification-manager", managerContext);
                        sendMimeEmail(managerEmail, "📋 Team Member Resignation Submitted - I2V Auth System", managerHtmlContent);
                    }
                }
            }
        } catch (Exception e) {
            log.error("Failed to send resignation notification email", e);
        }
    }
    
    private void sendMimeEmail(String to, String subject, String htmlContent) throws MessagingException {
        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
        helper.setFrom(fromEmail);
        helper.setTo(to);
        helper.setSubject(subject);
        helper.setText(htmlContent, true);
        mailSender.send(message);
        log.info("Resignation notification email sent successfully to: {}", to);
    }
}