package controllers

import java.util.Properties

import javax.mail.internet.{InternetAddress, MimeMessage}
import javax.mail.{Message, Session}

object Mailer {
  val host = "smtp.gmail.com"
  val port = "587"

  val address = "timokramer1@me.com"
  val username = "timokramer0408@gmai.com"
  val password = "imyhyfwcinlmcfpo"

  def sendMail(text:String, subject:String) = {
    val properties = new Properties()
    properties.put("mail.smtp.port", port)
    properties.put("mail.smtp.auth", "true")
    properties.put("mail.smtp.starttls.enable", "true")

    val session = Session.getDefaultInstance(properties, null)
    val message = new MimeMessage(session)
    message.addRecipient(Message.RecipientType.TO, new InternetAddress(address));
    message.setSubject(subject)
    message.setContent(text, "text/html")

    val transport = session.getTransport("smtp")
    transport.connect(host, username, password)
    transport.sendMessage(message, message.getAllRecipients)
  }

  def main(args:Array[String]) = {
    sendMail("aaaa", "bbb")
  }
}