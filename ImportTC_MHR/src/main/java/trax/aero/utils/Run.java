package trax.aero.utils;

import java.io.StringReader;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.logging.Logger;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;

import trax.aero.controller.Import_TC_MHR_Controller;
import trax.aero.data.Import_TC_MHR_Data;
import trax.aero.logger.LogManager;
import trax.aero.pojo.INT6_SND;
import trax.aero.pojo.INT6_TRAX;
import trax.aero.pojo.OperationTRAX;
import trax.aero.pojo.OpsLineEmail;
import trax.aero.pojo.OrderTRAX;

public class Run implements Runnable{

	// Variables
	  Import_TC_MHR_Data data = null;
	  final String url = System.getProperty("ImportTcMhr_URL");
	  final int MAX_ATTEMPTS = 3;
	  Logger logger = LogManager.getLogger("ImportTC_MHR");

	  public Run() {
	    data = new Import_TC_MHR_Data();
	  }

	  private void process() {
	    Poster poster = new Poster();
	    ArrayList<INT6_SND> ArrayReq = new ArrayList<INT6_SND>();
	    String executed = "OK";
	    try {
	      //loop
	      ArrayReq = data.getTaskCards();
	      boolean success = false;

	      if (!ArrayReq.isEmpty()) {
	        for (INT6_SND ArrayRequest : ArrayReq) {
	        	logger.info("RUN INFO " + ArrayRequest.getOrder().get(0).getTraxWO());
	          JAXBContext jc = JAXBContext.newInstance(INT6_SND.class);
	          Marshaller marshaller = jc.createMarshaller();
	          marshaller.setProperty(
	            Marshaller.JAXB_FORMATTED_OUTPUT,
	            Boolean.TRUE
	          );
	          StringWriter sw = new StringWriter();
	          marshaller.marshal(ArrayRequest, sw);

	          logger.info("Output: " + sw.toString());

	          for (int i = 0; i < MAX_ATTEMPTS; i++) {
	            success = poster.post(ArrayRequest, url);
	            if (success) {
	              break;
	            }
	          }
	          if (!success) {
	            logger.severe("Unable to send XML " + " to URL " + url);
	            Import_TC_MHR_Controller.addError(
	              "Unable to send XML " +
	              " to URL " +
	              url +
	              " MAX_ATTEMPTS: " +
	              MAX_ATTEMPTS
	            );
	          } else {
	            INT6_TRAX input = null;

	            try {
	              String body = poster.getBody();
	              
	              if(body == null || body.trim().isEmpty()) {
                      logger.severe("Received empty XML response");
                      throw new Exception("Empty XML response");
                  }
	              
	              StringReader sr = new StringReader(body);
	              logger.info("Raw XML data: " + body);
	              jc = JAXBContext.newInstance(INT6_TRAX.class);
	              String xmlDeclaration = body.split("\\s+", 3)[0];
	              logger.info("XML Declaration: " + xmlDeclaration);
	              Unmarshaller unmarshaller = jc.createUnmarshaller();
	              
	              input = (INT6_TRAX) unmarshaller.unmarshal(sr);

	              marshaller = jc.createMarshaller();
	              marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
	              sw = new StringWriter();
	              marshaller.marshal(input, sw);
	              logger.info("Input: " + sw.toString());

	              executed = data.markTransaction(input);
	              if (!executed.equalsIgnoreCase("OK")) {
	                executed = "Issue found";
	                throw new Exception("Issue found");
	              }
	            } catch (Exception e) {
	              logger.severe(e.toString());
	              Import_TC_MHR_Controller.addError(e.toString());
	              if (input != null) {
	                for (OrderTRAX o : input.getOrder()) {
	                  if (
	                    o.getOperations() != null && !o.getOperations().isEmpty()
	                  ) {
	                    for (OperationTRAX op : o.getOperations()) {
	                      OpsLineEmail opsLineEmail = data.getOpsLineStaffName(
	                        o.getWo(),
	                        op.getTaskCard()
	                      );

	                      Import_TC_MHR_Controller.sendEmailOpsLine(
	                        op.getOpsNo(),
	                        o,
	                        op,
	                        opsLineEmail
	                      );
	                    }
	                  } else {
	                    for (OperationTRAX op : o.getOperations()) {
	                      OpsLineEmail opsLineEmail = data.getOpsLineStaffName(
	                        o.getWo(),
	                        op.getTaskCard()
	                      );

	                      Import_TC_MHR_Controller.sendEmailOpsLine(
	                        "",
	                        o,
	                        op,
	                        opsLineEmail
	                      );
	                    }
	                  }
	                }
	              } else {
	                Import_TC_MHR_Controller.sendEmailService("NULL");
	              }
	            } finally {
	              logger.info("finishing");
	            }
	            logger.info(
	              "POST status: " + String.valueOf(success) + " to URL: " + url
	            );
	          }
	        }
	      }
	      if (!Import_TC_MHR_Controller.getError().isEmpty()) {
	        throw new Exception("Issue found");
	      }
	    } catch (Throwable e) {
	      logger.severe(e.toString());
	      Import_TC_MHR_Controller.addError(e.toString());
	      Import_TC_MHR_Controller.sendEMailRequest(ArrayReq);
	    }
	  }

	  public void run() {
	    try {
	      if (data.lockAvailable("I06")) {
	        data.lockTable("I06");
	        process();
	        data.unlockTable("I06");
	      }
	    } catch (Exception e) {
	      e.printStackTrace();
	    }
	  }
	
}
