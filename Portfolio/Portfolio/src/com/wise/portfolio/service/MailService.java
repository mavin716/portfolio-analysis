package com.wise.portfolio.service;

import java.io.File;
import java.io.IOException;
import java.util.Properties;

import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Multipart;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

public class MailService {

	protected static final Logger logger = LogManager.getLogger(MailService.class);

	public static void sendMail(String subject, String textBody, File portfolioPdfFile) {
		try {

			Properties properties = System.getProperties();

			properties.put("mail.smtp.host", AppProperties.getProperty("mailHost"));
			properties.put("mail.smtp.port", "587");
			properties.put("mail.smtp.starttls.enable", "true");
			properties.put("mail.smtp.auth", "true");

			Session session = Session.getInstance(properties, new javax.mail.Authenticator() {
				protected PasswordAuthentication getPasswordAuthentication() {
					return new PasswordAuthentication(AppProperties.getProperty("yahooMailAppAccount"),
							AppProperties.getProperty("yahooMailAppPassword"));
				}
			});

//		session.setDebug(true);
			Multipart multipart = new MimeMultipart();
			MimeBodyPart messageBodyPart = new MimeBodyPart();
			messageBodyPart.setText(textBody, "utf-8", "html");
			multipart.addBodyPart(messageBodyPart);

			MimeBodyPart attachmentBodyPart = new MimeBodyPart();
			attachmentBodyPart.attachFile(portfolioPdfFile, "application/pdf", null);
			multipart.addBodyPart(attachmentBodyPart);
			MimeMessage message = new MimeMessage(session);
			message.setContent(multipart);

			message.setFrom(new InternetAddress(AppProperties.getProperty("mailFrom")));
			message.addRecipient(Message.RecipientType.TO, new InternetAddress(AppProperties.getProperty("mailTo")));
			message.setSubject(subject);

			Transport.send(message);
		} catch (MessagingException | IOException mex) {
			logger.error("Exception sending mail:  " + mex.getMessage(), mex);
		}
	}
}
