package gr.madgik.catalogue.openaire;

import javax.mail.MessagingException;
import java.util.List;

public interface Mailer {

    void sendMail(List<String> to, List<String> cc, String subject, String text) throws MessagingException;

    void sendMail(List<String> to, String subject, String text) throws MessagingException;

    void sendMail(String to, String cc, String subject, String text) throws MessagingException;

    void sendMail(String to, String subject, String text) throws MessagingException;
}
