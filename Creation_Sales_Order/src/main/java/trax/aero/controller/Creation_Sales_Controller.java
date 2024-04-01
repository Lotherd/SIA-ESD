package trax.aero.controller;



import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.logging.Logger;

import trax.aero.logger.LogManager;
import trax.aero.pojo.INT7_SND;
import trax.aero.pojo.INT7_TRAX;
import trax.aero.pojo.OpsLineEmail;
import trax.aero.pojo.OrderSND;

import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;
import org.apache.commons.mail.Email;
import org.apache.commons.mail.SimpleEmail;

public class Creation_Sales_Controller {
	EntityManagerFactory factory;
	static String errors = "";
	static Logger logger = LogManager.getLogger("CreationSales");
	static String fromEmail = System.getProperty("fromEmail");
	static String host = System.getProperty("fromHost");
	static String port = System.getProperty("fromPort");
	static String toEmail = System.getProperty("Sales_Creation_toEmail");

	public Creation_Sales_Controller() {
		factory = Persistence.createEntityManagerFactory("TraxQADS");
		factory.createEntityManager();
	}
	
	public static void addError(String error) {
		errors = errors.concat(error + System.lineSeparator() + System.lineSeparator());
	}
	
	public static String getError() {
		return errors;
	}
	
	public static void sendEmailRequest(ArrayList <INT7_SND> arrayReq) {
		if(toEmail == null || toEmail.trim().isEmpty()) {
			logger.severe("Email address (toEmail) is not configured. Please check the system properties.");
            return;
		}
		
		try {
			String requests = "";
			for (INT7_SND or: arrayReq) {
				for(OrderSND r: or.getOrder()) {
					String WO = r.getTraxWo();
				requests = requests + " (WO Number: " + WO + ", Location: " + r.getLocationWO() + ", WO Description: " + r.getTcDescription() + "),";
			}
			}
			ArrayList <String> emailsList = new ArrayList <> (Arrays.asList(toEmail.split(",")));
			Email email = new SimpleEmail();
			email.setHostName(host);
            email.setSmtpPort(Integer.parseInt(port));
            email.setAuthentication("apikey", "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");
            email.setFrom(fromEmail);
            email.setSubject("Part Requisition Interface encountered an Error");
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
	
	public static void sendEmailResponse(INT7_TRAX response) {
		if(toEmail == null || toEmail.trim().isEmpty()) {
			logger.severe("Email address (toEmail) is not configured. Please check the system properties.");
			return;
		}
		try {
			StringBuilder responses = new StringBuilder();
				responses.append("WO: ").append(response.getWO()).append(",");
			
			ArrayList < String > emailsList = new ArrayList < String > (Arrays.asList(toEmail.split(",")));
            Email email = new SimpleEmail();
            email.setHostName(host);
            email.setSmtpPort(Integer.valueOf(port));
            email.setAuthentication("apikey", "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");
            email.setFrom(fromEmail);
            email.setSubject("Part Requisition Interface encountered an Error");
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
	
	public static void sendEmailOpsLine(String Operation, INT7_TRAX order, OpsLineEmail opsLineEmails) {
		if (toEmail == null || toEmail.trim().isEmpty()) {
			logger.severe("Emails address (toEmail) is not configured. Please check the system properties.");
			return;
		}
		try {
			String date = new Date().toString();
			ArrayList<String>emailsList = new ArrayList<String>(Arrays.asList(toEmail.split(",")));
			Email email = new SimpleEmail();
			email.setHostName(host);
			email.setSmtpPort(Integer.valueOf(port));
			email.setAuthentication("apikey", "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");
			email.setFrom(fromEmail);
			if(opsLineEmails.getFlag() != null && !opsLineEmails.getFlag().isEmpty() && (opsLineEmails.getFlag().equalsIgnoreCase("Y") || opsLineEmails.getFlag().equalsIgnoreCase("I"))) {
				email.setSubject("Failure to update INT 5 WO: " + order.getWO() + " RFO: " + order.getRfoNO());
				email.setMsg("WO: " + order.getWO() + " RFO: " + order.getRfoNO() + ", Date & Time of Transaction: " + date +  ", Error Code: " + order.getExceptionId() + ", Remarks: " + order.getExceptionDetail());				
			} else {
				email.setSubject("Failure to update WO: " + order.getWO() + " RFO: " + order.getRfoNO());
				email.setMsg("WO: " + order.getWO() + " RFO: " + order.getRfoNO() + ", Date & Time of Transaction: " + date +  ", Error Code: " + order.getExceptionId() + ", Remarks: " + order.getExceptionDetail());				
			}
			for (String emails: emailsList) {
				if(opsLineEmails.getEmail() == null || opsLineEmails.getEmail().isEmpty() || opsLineEmails.getEmail().equalsIgnoreCase("ERROR")) {
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