package swp391.fa25.lms.service;

import lombok.RequiredArgsConstructor;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class SupportService {

    private final JavaMailSender mailSender;

    private final String adminMail = "huytqhe186506@fpt.edu.vn";
    private final String managerMail = "tuna10a6@gmail.com";

    public void sendSupportEmail(String name, String email, String subject, String messageContent) {

        String body = """
                Bạn nhận được yêu cầu hỗ trợ mới từ khách hàng:
                
                Subject: %s

                Họ tên: %s
                Email: %s

                ----------------------------
                Nội dung:
                %s
                ----------------------------

                ToolMarket Support System
                """.formatted(subject,name, email, messageContent);

        SimpleMailMessage mail = new SimpleMailMessage();
        mail.setTo(managerMail);
        mail.setCc(adminMail);
        mail.setSubject("[ToolMarket Support] " + subject);
        mail.setText(body);

        mailSender.send(mail);
    }
}
