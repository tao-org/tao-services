/*
 * Copyright (C) 2018 CS ROMANIA
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation; either version 3 of the License, or (at your option)
 * any later version.
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for
 * more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, see http://www.gnu.org/licenses/
 */
package ro.cs.tao.services.admin.mail;

import ro.cs.tao.configuration.ConfigurationManager;

import javax.mail.*;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import java.util.Properties;
import java.util.logging.Logger;

public class MailSenderTLS {

    private static final Logger logger = Logger.getLogger(MailSenderTLS.class.getName());
    private String mailSmtpAuth;
    private String mailSmtpStartTlsEnable;
    private String mailSmptpHost;
    private String mailSmtpPort;
    private String mailFrom;
    private String mailUsername;
    private String mailPassword;

    public MailSenderTLS() {
        ConfigurationManager configManager = ConfigurationManager.getInstance();
        this.mailSmtpAuth = configManager.getValue("mail.smtp.auth");
        this.mailSmtpStartTlsEnable = configManager.getValue("mail.smtp.starttls.enable");
        this.mailSmptpHost = configManager.getValue("mail.smtp.host");
        this.mailSmtpPort = configManager.getValue("mail.smtp.port");
        this.mailFrom = configManager.getValue("mail.tao.from");
        this.mailUsername = configManager.getValue("mail.tao.username");
        this.mailPassword = configManager.getValue("mail.tao.password");
    }

   public void sendMail(String toAddress, String subject, String content) {
        Properties props = new Properties();
        props.put("mail.smtp.auth", mailSmtpAuth.toString());
        props.put("mail.smtp.starttls.enable", mailSmtpStartTlsEnable);
        props.put("mail.smtp.host", mailSmptpHost);
        props.put("mail.smtp.port", mailSmtpPort);

        Session session = Session.getInstance(props,
          new javax.mail.Authenticator() {
              protected PasswordAuthentication getPasswordAuthentication() {
                  return new PasswordAuthentication(mailUsername, mailPassword);
              }
          });

        try {
            Message message = new MimeMessage(session);
            message.setFrom(new InternetAddress(mailFrom));
            message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(toAddress));
            message.setSubject(subject);
            message.setContent(content, "text/html");

            Transport.send(message);

            logger.fine("Email sent to " + toAddress);

        } catch (MessagingException e) {
            throw new RuntimeException(e);
        }
    }
}
