/**
 * Copyright 2021-2025 OpenAIRE AMKE
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package gr.madgik.catalogue.openaire;

import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import javax.mail.*;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class MailerService implements Mailer {

    private static final Logger logger = LoggerFactory.getLogger(MailerService.class);
    private Session session;

    @Value("${mail.smtp.auth}")
    String auth;

    @Value("${mail.smtp.host}")
    String host;

    @Value("${mail.smtp.from}")
    String from;

    @Value("${mail.smtp.user}")
    String user;

    @Value("${mail.smtp.password}")
    String password;

    @Value("${mail.smtp.protocol}")
    String protocol;

    @Value("${mail.smtp.port}")
    String port;

    @Value("${mail.smtp.ssl.enable}")
    String ssl;

    @Value("${emails.send:true}")
    boolean enableEmails;

    @PostConstruct
    private void postConstruct() {
        Properties sessionProps = new Properties();
        sessionProps.setProperty("mail.transport.protocol", protocol);
        sessionProps.setProperty("mail.smtp.auth", auth);
        sessionProps.setProperty("mail.smtp.host", host);
        sessionProps.setProperty("mail.smtp.password", password);
        sessionProps.setProperty("mail.smtp.port", port);
        sessionProps.setProperty("mail.smtp.ssl.enable", ssl);
        sessionProps.setProperty("mail.smtp.user", user);
        sessionProps.setProperty("mail.smtp.from", from);
        session = Session.getInstance(sessionProps, new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(user, password);
            }
        });
    }

    @Async
    @Override
    public void sendMail(List<String> to, List<String> cc, String subject, String text) throws MessagingException {
        if (enableEmails) {
            Transport transport = null;
            try {
                transport = session.getTransport();
                InternetAddress sender = new InternetAddress(from);
                Message message = new MimeMessage(session);
                message.setFrom(sender);
                if (to != null) {
                    message.setRecipients(Message.RecipientType.TO, createAddresses(to));
                }
                if (cc != null) {
                    message.setRecipients(Message.RecipientType.CC, createAddresses(cc));
                }
                message.setRecipient(Message.RecipientType.BCC, sender);
                message.setSubject(subject);
                message.setText(text);
                transport.connect();
                Transport.send(message);
            } catch (MessagingException e) {
                logger.error("ERROR", e);
            } finally {
                if (transport != null) {
                    transport.close();
                }
            }
        }
    }

    @Override
    public void sendMail(List<String> to, String subject, String text) throws MessagingException {
        sendMail(to, null, subject, text);
    }

    @Override
    public void sendMail(String to, String cc, String subject, String text) throws MessagingException {
        List<String> addrTo = new ArrayList<>();
        List<String> addrCc = new ArrayList<>();
        if (to != null) {
            addrTo.addAll(Arrays.stream(to.split(",")).filter(Objects::nonNull).collect(Collectors.toList()));
        }
        if (cc != null) {
            addrTo.addAll(Arrays.stream(cc.split(",")).filter(Objects::nonNull).collect(Collectors.toList()));
        }
        sendMail(addrTo, addrCc, subject, text);
    }

    @Override
    public void sendMail(String to, String subject, String text) throws MessagingException {
        sendMail(to, null, subject, text);
    }

    private InternetAddress[] createAddresses(List<String> emailAddresses) throws AddressException {
        InternetAddress[] addresses = new InternetAddress[emailAddresses.size()];
        for (int i = 0; i < emailAddresses.size(); i++) {
            addresses[i] = new InternetAddress(emailAddresses.get(i));
        }
        return addresses;
    }
}
