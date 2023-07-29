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

public class MailService {
	
	private final static String MAIL_HOST = "smtp.mail.yahoo.com";
	private final static String YAHOO_MAIL_APP_PASSWORD = "hgcqlgrdhxdvgmjm";
	private final static String MAIL_TO = "mavin14534@yahoo.com";
	private final static String MAIL_FROM = "mavin14534@yahoo.com";


	public static void sendMail(String subject, String textBody, File portfolioPdfFile) {
		Properties properties = System.getProperties();

		properties.put("mail.smtp.host", MAIL_HOST);
		properties.put("mail.smtp.port", "587");
		properties.put("mail.smtp.starttls.enable", "true");
		properties.put("mail.smtp.auth", "true");

		Session session = Session.getInstance(properties, new javax.mail.Authenticator() {
			protected PasswordAuthentication getPasswordAuthentication() {
				return new PasswordAuthentication("mavin14534@yahoo.com", YAHOO_MAIL_APP_PASSWORD);
			}
		});

//		session.setDebug(true);
		try {
			Multipart multipart = new MimeMultipart();
			MimeBodyPart messageBodyPart = new MimeBodyPart();
			messageBodyPart.setText(textBody, "utf-8", "html");
			multipart.addBodyPart(messageBodyPart);

			MimeBodyPart attachmentBodyPart = new MimeBodyPart();
			attachmentBodyPart.attachFile(portfolioPdfFile, "application/pdf", null);
			multipart.addBodyPart(attachmentBodyPart);
			MimeMessage message = new MimeMessage(session);
			message.setContent(multipart);

			message.setFrom(new InternetAddress(MAIL_FROM));
			message.addRecipient(Message.RecipientType.TO, new InternetAddress(MAIL_TO));
			message.setSubject(subject);

			Transport.send(message);
		} catch (MessagingException | IOException mex) {
			mex.printStackTrace();
		}
	}
}
