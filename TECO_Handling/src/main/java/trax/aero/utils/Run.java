package trax.aero.utils;

import java.io.StringWriter;
import java.util.ArrayList;
import java.util.logging.Logger;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.Marshaller;
import trax.aero.controller.TECO_Handling_Controller;
import trax.aero.data.TECO_Handling_Data;
import trax.aero.logger.LogManager;
import trax.aero.pojo.INT15_SND;
import trax.aero.pojo.INT15_TRAX;

public class Run implements Runnable{
	
	TECO_Handling_Data data = null;
	final String url = System.getProperty("TECO_url");
	final int MAX_ATTEMPTS = 3;
	Logger logger = LogManager.getLogger("TECO_Handling");
	
	public Run() {
		data = new TECO_Handling_Data();
	}
	
	public void process() {
		Poster poster = new Poster();
		ArrayList<INT15_SND> ArrayReq = new ArrayList<INT15_SND>();
		String executed = "OK";
		
		try {
			ArrayReq = data.getSVO();
			String markSendResult;
			boolean success = false;
			
			if(!ArrayReq.isEmpty()) {
				for (INT15_SND ArrayRequest : ArrayReq) {
					if (!ArrayRequest.getSAP_number().isEmpty()) {
						logger.info("RUN INFO " +ArrayRequest.getSAP_number() );
					} else {
						logger.info("RUN INFO: List is empty");
					}
					JAXBContext jc = JAXBContext.newInstance(INT15_SND.class);
					Marshaller marshaller = jc.createMarshaller();
					marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
					
					StringWriter sw = new StringWriter();
					marshaller.marshal(ArrayRequest, sw);
					
					
					logger.info("Output: " + sw.toString());
					
					for (int i = 0; i < MAX_ATTEMPTS; i++) {
			        	  success = poster.post(ArrayRequest, url);
			        	  markSendResult = data.markSendData();
			        	  if ("OK".equals(markSendResult)) {
			            success = true;
			            break;
			        	  }
			          }
			          if (!success) {
				        	 logger.severe("Unable to send XML "+ "to URL " + url);
				        	 TECO_Handling_Controller.addError("Unable to send XML " + "to URL " + url + " MAX_ATTEMPTS: " + MAX_ATTEMPTS);
				         } else {
				        	 INT15_TRAX input = null;
				        	 
				        	 logger.info("finishing");
				        	 
					        logger.info("POST status: " + String.valueOf(success) + " to URL: " + url);
				         }
					
				}
			}
			if (!TECO_Handling_Controller.getError().isEmpty()) {
				throw new Exception("Issue found");
			}
			
		}catch(Throwable e){
			logger.severe(e.toString());
			TECO_Handling_Controller.addError(e.toString());
			TECO_Handling_Controller.sendEmailRequest(ArrayReq);
		}
	}
	
	
	public void run() {
	    try {
	      if (data.lockAvailable("I15")) {
	        data.lockTable("I15");
	        process();
	        data.unlockTable("I15");
	      }
	    } catch (Exception e) {
	      e.printStackTrace();
	    }
	  }

}
