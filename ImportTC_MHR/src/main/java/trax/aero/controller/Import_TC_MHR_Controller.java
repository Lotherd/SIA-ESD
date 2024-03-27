package trax.aero.controller;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.logging.Logger;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;
import org.apache.commons.mail.Email;
import org.apache.commons.mail.SimpleEmail;
import trax.aero.logger.LogManager;
import trax.aero.pojo.INT6_SND;
import trax.aero.pojo.INT6_TRAX;
import trax.aero.pojo.OperationSND;
import trax.aero.pojo.OperationTRAX;
import trax.aero.pojo.OpsLineEmail;
import trax.aero.pojo.OrderSND;
import trax.aero.pojo.OrderTRAX;

public class Import_TC_MHR_Controller {
    EntityManagerFactory factory;
    static String errors = "";
    static Logger logger = LogManager.getLogger("ImportTC_MHR");
    static String fromEmail = System.getProperty("fromEmail");
    static String host = System.getProperty("fromHost");
    static String port = System.getProperty("fromPort");
    static String toEmail = System.getProperty("ImportTC_MHR_toEmail");
    public Import_TC_MHR_Controller() {
        factory = Persistence.createEntityManagerFactory("TraxQADS");
        factory.createEntityManager();
    }
    public static void addError(String error) {
        errors = errors.concat(error + System.lineSeparator() + System.lineSeparator());
    }
    public static String getError() {
        return errors;
    }
    /*public static void sendEMailRequest(ArrayList<INT6_SND> arrayReq) {
	    try {
	      String requests = "";

	      for (INT6_SND req : arrayReq) {
	        for (OrderSND r : req.getOrder()) {
	          for (OperationSND op : r.getOperations()) {
	            String tcNumber = op.getTcNumber();
	            requests =
	              requests +
	              " (Task Card: " +
	              tcNumber +
	              ", WO: " +
	              r.getTraxWO() +
	              "),";
	          }
	        }
	      }

	      ArrayList<String> emailsList = new ArrayList<String>(
	        Arrays.asList(toEmail.split(","))
	      );

	      Email email = new SimpleEmail();
	      email.setHostName(host);
	      email.setSmtpPort(Integer.valueOf(port));
	      email.setFrom(fromEmail);
	      email.setSubject("Import_TC_MHR Interface encountered a Error");

	      for (String emails : emailsList) {
	        email.addTo(emails);
	      }
	      email.setMsg(
	        "Request that failed: " +
	        requests +
	        " has encountered an issue. " +
	        "Issues found at:\n" +
	        errors
	      );
	      email.send();
	    } catch (Exception e) {
	      logger.severe(e.toString());
	      logger.severe("Email not found");
	    } finally {
	      errors = "";
	    }
	  }*/
    public static void sendEMailRequest(ArrayList < INT6_SND > arrayReq) {
        if (toEmail == null || toEmail.trim().isEmpty()) {
            logger.severe("Email address (toEmail) is not configured. Please check the system properties.");
            return;
        }
        try {
            String requests = "";
            for (INT6_SND req: arrayReq) {
                for (OrderSND r: req.getOrder()) {
                    for (OperationSND op: r.getOperations()) {
                        String tcNumber = op.getTcNumber();
                        requests = requests + " (Task Card: " + tcNumber + ", WO: " + r.getTraxWO() + "),";
                    }
                }
            }
            ArrayList < String > emailsList = new ArrayList < > (Arrays.asList(toEmail.split(",")));
            Email email = new SimpleEmail();
            email.setHostName(host);
            email.setSmtpPort(Integer.parseInt(port));
            email.setAuthentication("apikey", "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");
            email.setFrom(fromEmail);
            email.setSubject("Import_TC_MHR Interface encountered an Error");
            for (String emailAddress: emailsList) {
                email.addTo(emailAddress.trim());
            }
            email.setMsg("Request that failed: " + requests + " has encountered an issue. Issues found at:\n" + errors);
            email.send();
            logger.info("Email sent successfully to: " + String.join(", ", emailsList));
        } catch (Exception e) {
            logger.severe("Failed to send email due to: " + e.toString());
        } finally {
            errors = "";
        }
    }
    public static void sendEmailResponse(INT6_TRAX response) {
    	  if (toEmail == null || toEmail.trim().isEmpty()) {
    	        logger.severe("Email address (toEmail) is not configured. Please check the system properties.");
    	        return;
    	    }
        try {
            StringBuilder responses = new StringBuilder();
            for (OrderTRAX r: response.getOrder()) {
                responses.append("SAP Order Number: ").append(r.getRfoNo()).append(",");
            }
            ArrayList < String > emailsList = new ArrayList < String > (Arrays.asList(toEmail.split(",")));
            Email email = new SimpleEmail();
            email.setHostName(host);
            email.setSmtpPort(Integer.valueOf(port));
            email.setAuthentication("apikey", "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");
            email.setFrom(fromEmail);
            email.setSubject("Import_TC_MHR Interface encountered a Error");
            for (String emails: emailsList) {
                email.addTo(emails);
            }
            email.setMsg("Responses that failed: " + responses + " has encountered an issue. " + "Enter records manually. " + "Issues found at:\n" + errors);
            email.send();
            logger.info("Email sent successfully to: " + String.join(", ", emailsList));
        } catch (Exception e) {
            logger.severe(e.toString());
            logger.severe("Email not found");
        } finally {
            errors = "";
        }
    }
    public static void sendEmailOpsLine(String OperationNumber, OrderTRAX order, OperationTRAX operation, OpsLineEmail opsLineEmails) {
    	if (toEmail == null || toEmail.trim().isEmpty()) {
            logger.severe("Email address (toEmail) is not configured. Please check the system properties.");
            return;
        }
        try {
            String date = new Date().toString();
            ArrayList < String > emailsList = new ArrayList < String > (Arrays.asList(toEmail.split(",")));
            Email email = new SimpleEmail();
            email.setHostName(host);
            email.setSmtpPort(Integer.valueOf(port));
            email.setAuthentication("apikey", "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");
            email.setFrom(fromEmail);
            if (opsLineEmails.getFlag() != null && !opsLineEmails.getFlag().isEmpty() && (opsLineEmails.getFlag().equalsIgnoreCase("Y") || opsLineEmails.getFlag().equalsIgnoreCase("I"))) {
                email.setSubject("Failure to update Import_TC_MHR WO: " + order.getWo() + " Task Card: " + operation.getTaskCard());
                email.setMsg("TRAX WO Number: " + order.getWo() + " ,TRAX Task Card Number: " + operation.getTaskCard() + " ,Date & Time of Transaction: " + date + " ,SPA Order Number: " + order.getRfoNo() + " ,Operation Number: " + operation.getOpsNo() + " ,Error Code: " + order.getExceptionId() + " ,Remarks: " + order.getExceptionDetail());
            } else {
                email.setSubject("Failure to update Order Details WO: " + order.getWo() + " Task Card: " + operation.getTaskCard());
                email.setMsg("TRAX WO Number: " + order.getWo() + " ,TRAX Task Card Number: " + operation.getTaskCard() + " ,Date & Time of Transaction: " + date + " ,SPA Order Number: " + order.getRfoNo() + " ,Operation Number: " + operation.getOpsNo() + " ,Error Code: " + order.getExceptionId() + " ,Remarks: " + order.getExceptionDetail());
            }
            for (String emails: emailsList) {
                if (opsLineEmails.getEmail() == null || opsLineEmails.getEmail().isEmpty() || opsLineEmails.getEmail().equalsIgnoreCase("ERROR")) {
                    email.addTo(emails);
                } else {
                    email.addTo(opsLineEmails.getEmail());
                }
            }
            email.send();
            logger.info("Email sent successfully to: " + String.join(", ", emailsList));
        } catch (Exception e) {
            logger.severe(e.toString());
            logger.severe("Email not found");
        } finally {
            errors = "";
        }
    }
    public static void sendEmailService(String outcome) {
    	if (toEmail == null || toEmail.trim().isEmpty()) {
            logger.severe("Email address (toEmail) is not configured. Please check the system properties.");
            return;
        }
        try {
            ArrayList < String > emailsList = new ArrayList < String > (Arrays.asList(toEmail.split(",")));
            Email email = new SimpleEmail();
            email.setHostName(host);
            email.setSmtpPort(Integer.valueOf(port));
            email.setAuthentication("apikey", "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");
            email.setFrom(fromEmail);
            email.setSubject("Import_TC_MHR Interface encountered a Error");
            for (String emails: emailsList) {
                email.addTo(emails);
            }
            email.setMsg("Input" + " has encountered an issue. " + "Enter records manually. " + "Issues found at:\n" + errors);
            email.send();
        } catch (Exception e) {
            logger.severe(e.toString());
            logger.severe("Email not found");
        } finally {
            errors = "";
        }
    }
}