package trax.aero.utils;

import java.io.StringReader;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import javax.xml.bind.Unmarshaller;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.Marshaller;
import trax.aero.controller.Part_Requisition_Controller;
import trax.aero.data.Part_Requisition_Data;
import trax.aero.logger.LogManager;
import trax.aero.pojo.INT13_SND;
import trax.aero.pojo.INT13_TRAX;
import trax.aero.pojo.OpsLineEmail;

public class Run implements Runnable{
	
	Part_Requisition_Data data = null;
	final String url = System.getProperty("PartREQ_URL");
	final int MAX_ATTEMPTS = 3;
	Logger logger = LogManager.getLogger("Part_REQ");
	
	public Run() {
		data = new Part_Requisition_Data();
	}
	
	private void process() {
		Poster poster = new Poster();
		ArrayList<INT13_SND> ArrayReq = new ArrayList<INT13_SND>();
		String executed = "OK";
		try {
			ArrayReq = data.getRequisiton();
			 String markSendResult;
		      boolean success = false;
			
			if(!ArrayReq.isEmpty()) {
				for (INT13_SND ArrayRequest : ArrayReq) {
					 if (!ArrayRequest.getOrder().isEmpty()) {
	                        logger.info("RUN INFO " + ArrayRequest.getOrder().get(0).getOrderNO());
	                    } else {
	                        logger.info("RUN INFO: Order list is empty");
	                    }
					JAXBContext jc = JAXBContext.newInstance(INT13_SND.class);
					Marshaller marshaller = jc.createMarshaller();
					marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
					
					StringWriter sw = new StringWriter();
			          marshaller.marshal(ArrayRequest, sw);

			          logger.info("Output: " + sw.toString());

			          for (int i = 0; i < MAX_ATTEMPTS; i++) {
			        	  markSendResult = data.markSendData();
			        	  if ("OK".equals(markSendResult)) {
			            success = true;
			            break;
			        	  }
			          }
			         if (!success) {
			        	 logger.severe("Unable to send XML "+ "to URL " + url);
			        	 Part_Requisition_Controller.addError("Unable to send XML " + "to URL " + url + " MAX_ATTEMPTS: " + MAX_ATTEMPTS);
			         } else {
			        	 List<INT13_TRAX> input = null;
			        	 
			        	 try {
			        		 String body = poster.getBody();
				              StringReader sr = new StringReader(body);
				              logger.info("Raw XML data: " + body);
				              jc = JAXBContext.newInstance(INT13_TRAX.class);
				              String xmlDeclaration = body.split("\\s+", 3)[0];
				              logger.info("XML Declaration: " + xmlDeclaration);
				              Unmarshaller unmarshaller = jc.createUnmarshaller();
				              input = (List<INT13_TRAX>) unmarshaller.unmarshal(sr);
				              
				              
				              
				              marshaller = jc.createMarshaller();
				              marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
				              sw = new StringWriter();
				              marshaller.marshal(input, sw);
				              logger.info("Input: " + sw.toString());
				              
				              executed = data.markTransaction(input);
				              if(!executed.equalsIgnoreCase("OK")) {
				            	  executed = "Issue found";
					                throw new Exception("Issue found");
				              }

			        	 }catch (Exception e){
			        		 logger.severe(e.toString());
				              Part_Requisition_Controller.addError(e.toString());
				              if (input != null) {
				            	  for(INT13_TRAX r: input) {
				            		  OpsLineEmail opsLineEmail = data.getOpsLineStaffName(r.getRequisition());
				            		  
				            		  Part_Requisition_Controller.sendEmailOpsLine(r.getOPS(), r, opsLineEmail);
				            	  }
				              } else {
				            	  Part_Requisition_Controller.sendEmailService("NULL");
				              }
			        	 }finally {
			        		 logger.info("finishing");
			        	 }
			        	 logger.info("POST status: " + String.valueOf(success) + " to URL: " + url);
			         }
				}
			}
			if (!Part_Requisition_Controller.getError().isEmpty()) {
				throw new Exception("Issue found");
			}
		}catch(Throwable e){
			logger.severe(e.toString());
		      Part_Requisition_Controller.addError(e.toString());
		      Part_Requisition_Controller.sendEmailRequest(ArrayReq);
		}
	}
	
	public void run() {
	    try {
	      if (data.lockAvailable("I13")) {
	        data.lockTable("I13");
	        process();
	        data.unlockTable("I13");
	      }
	    } catch (Exception e) {
	      e.printStackTrace();
	    }
	  }
	

}
