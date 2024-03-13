package trax.aero.utils;

import java.util.ArrayList;
import java.util.logging.Logger;

import trax.aero.data.Part_Requisition_Data;
import trax.aero.logger.LogManager;
import trax.aero.pojo.INT13_SND;

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
			ArrayReq
		}
	}

}
