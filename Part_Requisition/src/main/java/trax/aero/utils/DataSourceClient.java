package trax.aero.utils;

import java.sql.Connection;
import java.util.logging.Logger;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.sql.DataSource;

import trax.aero.logger.LogManager;

public class DataSourceClient {
	
	static Logger logger = LogManager.getLogger("Part_REQ");
	
	public static Connection getConnection() throws Exception{
		Connection connection;
		Context ctx;
		try {
			ctx = new InitialContext();
			
			DataSource ds = null;
			
			if(System.getProperty("jboss.serve.config.dir") != null) ds = (DataSource) ctx.lookup("java:/TraxQADS");
			else ds = (DataSource) ctx.lookup("TraxQADS");
			connection = ds.getConnection();
		} catch (Exception e) {
			logger.severe("An error ocurred trying connect to the DataSource: TraxQADS");
			throw new Exception(
					"\nGetting error trying to connect to the datasource. " +
					"\n error: " +
					ErrorType.INTERNAL_SERVER_ERROR);
		}
		return connection;
	}

}
