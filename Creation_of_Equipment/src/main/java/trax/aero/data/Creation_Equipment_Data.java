package trax.aero.data;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;
import javax.xml.bind.JAXBException;

import trax.aero.controller.Creation_Equipment_Controller;
import trax.aero.exception.CustomizeHandledException;
import trax.aero.logger.LogManager;
import trax.aero.pojo.INT5_SND;
import trax.aero.pojo.INT5_TRAX;
import trax.aero.utils.DataSourceClient;

public class Creation_Equipment_Data {
	
	EntityManagerFactory factory;
	EntityManager em;
	String executed;
	private Connection con;
	
	final String MaxRecord = System.getProperty("Creation_EQ_MaxRecord");
	Logger logger = LogManager.getLogger("CreationEquipment");
	
	public Creation_Equipment_Data(String mark) {
		try {
			if(this.con == null || this.con.isClosed()) {
				this.con = DataSourceClient.getConnection();
				logger.info("The connection was stablished successfully with status: " +String.valueOf(!this.con.isClosed()));
			}
		} catch(SQLException e) {
			logger.severe("An error ocurred getting the status of the connection");
			Creation_Equipment_Controller.addError(e.toString());
		} catch(CustomizeHandledException e1) {
			Creation_Equipment_Controller.addError(e1.toString());
		} catch(Exception e) {
			Creation_Equipment_Controller.addError(e.toString());
		}
		
	}
	
	public Creation_Equipment_Data() {
		try {
			if(this.con == null || this.con.isClosed()) {
				this.con = DataSourceClient.getConnection();
				logger.info("The connection was stablished successfully with status: " + String.valueOf(!this.con.isClosed()));
			}
		} catch (SQLException e) {
		      logger.severe("An error occured getting the status of the connection");
		      Creation_Equipment_Controller.addError(e.toString());
		    } catch (CustomizeHandledException e1) {
		      Creation_Equipment_Controller.addError(e1.toString());
		    } catch (Exception e) {
		      Creation_Equipment_Controller.addError(e.toString());
		    }
		factory = Persistence.createEntityManagerFactory("TraxQADS");
		em = factory.createEntityManager();
	}
	
	public Connection getCon() {
		return con;
	}
	
	public String markSendData() throws JAXBException {
		INT5_TRAX request = new INT5_TRAX();
		  try {
		        markTransaction(request);
		        logger.info("markTransaction completed successfully.");
		        return "OK";
		    } catch (Exception e) {
		    	logger.log(Level.SEVERE, "Error executing markTransaction", e);
		    	e.printStackTrace();
		        return null; 
		    }
	}
	
	public String markTransaction(INT5_TRAX request) {
		executed = "OK";
		
		return executed;
	}
	
	public INT5_SND getRequisition() throws Exception{
		executed = "OK";
		
		if (this.con == null || this.con.isClosed()) {
	        try {
	            this.con = DataSourceClient.getConnection(); 
	            if (this.con == null || this.con.isClosed()) {
	                throw new IllegalStateException("Issues connecting to the database");
	            }
	            logger.info("Established connection to the database");
	        } catch (SQLException e) {
	            throw new IllegalStateException("Error trying to re-connect to the database.", e);
	        }
	    }
		
		INT5_SND list = new INT5_SND();
		
		return list;
	}
	

}
