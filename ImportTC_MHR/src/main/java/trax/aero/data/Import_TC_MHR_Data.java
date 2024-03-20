package trax.aero.data;

import java.io.StringReader;
import java.math.BigDecimal;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.NoResultException;
import javax.persistence.Persistence;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;

import org.eclipse.yasson.internal.Unmarshaller;

import trax.aero.controller.Import_TC_MHR_Controller;
import trax.aero.exception.CustomizeHandledException;
import trax.aero.logger.LogManager;
import trax.aero.model.InterfaceLockMaster;
import trax.aero.pojo.INT6_SND;
import trax.aero.pojo.INT6_TRAX;
import trax.aero.pojo.OperationAudit;
import trax.aero.pojo.OperationSND;
import trax.aero.pojo.OrderTRAX;
import trax.aero.pojo.OperationTRAX;
import trax.aero.pojo.OpsLineEmail;
import trax.aero.pojo.OrderAudit;
import trax.aero.pojo.OrderSND;
import trax.aero.utils.DataSourceClient;
import trax.aero.utils.ErrorType;

public class Import_TC_MHR_Data {
	
	EntityManagerFactory factory;
	  EntityManager em;
	  String executed;
	  private Connection con;

	  final String MaxRecord = System.getProperty("Import_TC_MHR_MaxRecord");
	  Logger logger = LogManager.getLogger("ImportTC_MHR");

	  public Import_TC_MHR_Data(String mark) {
	    try {
	      if (this.con == null || this.con.isClosed()) {
	        this.con = DataSourceClient.getConnection();
	        logger.info(
	          "The connection was stabliched successfully with status: " +
	          String.valueOf(!this.con.isClosed())
	        );
	      }
	    } catch (SQLException e) {
	      logger.severe("An error occured getting the status of the connection");
	      Import_TC_MHR_Controller.addError(e.toString());
	    } catch (CustomizeHandledException e1) {
	      Import_TC_MHR_Controller.addError(e1.toString());
	    } catch (Exception e) {
	      Import_TC_MHR_Controller.addError(e.toString());
	    }
	  }

	  public Import_TC_MHR_Data() {
	    try {
	      if (this.con == null || this.con.isClosed()) {
	        this.con = DataSourceClient.getConnection();
	        logger.info(
	          "The connection was stablished successfully with status: " +
	          String.valueOf(!this.con.isClosed())
	        );
	      }
	    } catch (SQLException e) {
	      logger.severe("An error occured getting the status of the connection");
	      Import_TC_MHR_Controller.addError(e.toString());
	    } catch (CustomizeHandledException e1) {
	      Import_TC_MHR_Controller.addError(e1.toString());
	    } catch (Exception e) {
	      Import_TC_MHR_Controller.addError(e.toString());
	    }

	    factory = Persistence.createEntityManagerFactory("TraxStandaloneDS");
	    em = factory.createEntityManager();
	  }

	  public Connection getCon() {
	    return con;
	  }
	  
	  public String markSendData() throws JAXBException
		{
		  INT6_TRAX request = new INT6_TRAX();
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
	  
	  
	  public String markTransaction(INT6_TRAX request) {
		    executed = "OK";

		    String sqlDate = "UPDATE WO_TASK_CARD SET INTERFACE_TRANSFERRED_DATE = sysdate, INTERFACE_FLAG = null WHERE INTERFACE_TRANSFERRED_DATE IS NULL AND TASK_CARD = ? AND WO = ?";
		    String sqlOPS = "UPDATE WO_TASK_CARD_ITEM SET OPS_NO = ? WHERE TASK_CARD = ? AND WO = ?";

		    try (PreparedStatement pstmt2 = con.prepareStatement(sqlDate);
		         PreparedStatement pstmt3 = con.prepareStatement(sqlOPS)) {

		        for (OrderTRAX r : request.getOrder()) {
		            for (OperationTRAX o : r.getOperations()) {
		                if (o != null) {
		                    pstmt2.setString(1, o.getTaskCard());
		                    pstmt2.setString(2, r.getWo());
		                    pstmt2.executeUpdate();

		                    if (o.getOpsNo() != null && !o.getOpsNo().isEmpty()) {
		                        pstmt3.setString(1, o.getOpsNo());
		                        pstmt3.setString(2, o.getTaskCard());
		                        pstmt3.setString(3, r.getWo());
		                        pstmt3.executeUpdate();
		                    }

		                    if (!r.getExceptionId().equalsIgnoreCase("53")) {
		                        executed = "Request SAP Order Number: " + r.getRfoNo() + ", Error Code: " + r.getExceptionId() + ", Remarks: " + r.getExceptionDetail() + ", Operation Number: " + o.getOpsNo();
		                        Import_TC_MHR_Controller.addError(executed);
		                    }
		                }
		            }
		        }
		    } catch (SQLException e) {
		        executed = e.toString();
		        Import_TC_MHR_Controller.addError(executed);
		        logger.severe(executed);
		    }
		    return executed;
		}

	  public ArrayList<INT6_SND> getTaskCards() throws Exception{
	    executed = "OK";

	    ArrayList<INT6_SND> list = new ArrayList<INT6_SND>();
	    ArrayList<OperationSND> oplist = new ArrayList<OperationSND>();
	    ArrayList<OrderSND> orlist = new ArrayList<OrderSND>();

	    String sqlTaskCard =
	      "SELECT REFERENCE_TASK_CARD,TASK_CARD_DESCRIPTION,PRIORITY,WO,TASK_CARD,STATUS,\r\n" +
	      "(SELECT W.RFO_NO FROM WO W WHERE W.WO = WO_TASK_CARD.WO AND W.MODULE = 'SHOP' AND WO_TASK_CARD.INTERFACE_FLAG is not null \r\n" +
	      "AND (WO_TASK_CARD.non_routine = 'N' OR WO_TASK_CARD.non_routine = 'Y' OR WO_TASK_CARD.non_routine IS NULL) AND w.rfo_no IS NOT NULL) as ESD_RFO \r\n" +
	      "FROM WO_TASK_CARD WHERE INTERFACE_TRANSFERRED_DATE IS NULL AND (1=(SELECT count(*) FROM WO W \r\n" +
	      "WHERE W.WO = WO_TASK_CARD.WO AND W.MODULE = 'SHOP' AND WO_TASK_CARD.INTERFACE_FLAG is not null AND W.RFO_NO is not null \r\n" +
	      "AND (WO_TASK_CARD.non_routine = 'N' OR WO_TASK_CARD.non_routine = 'Y' OR WO_TASK_CARD.non_routine IS NULL)))  \r\n" +
	      "AND (non_routine = 'N' OR non_routine = 'Y' OR non_routine IS NULL) AND EXISTS (SELECT 1 FROM wo w WHERE w.wo = wo_task_card.wo AND w.module = 'SHOP' AND w.rfo_no IS NOT NULL) \r\n" ;

	    if (MaxRecord != null && !MaxRecord.isEmpty()) {
	      sqlTaskCard = "SELECT *	FROM (" + sqlTaskCard;
	    }

	    if (MaxRecord != null && !MaxRecord.isEmpty()) {
	      sqlTaskCard = sqlTaskCard + "  )WHERE ROWNUM <= ?";
	    }

	    String sqlItem =
	      "SELECT WTI.OPS_NO, WT.MODIFIED_DATE FROM WO_TASK_CARD_ITEM WTI, WO_TASK_CARD WT WHERE WTI.WO = ? AND WTI.TASK_CARD = ? AND WT.WO = WTI.WO AND WT.TASK_CARD = WTI.TASK_CARD";

	    String sqlWork =
	     "SELECT COALESCE(SUM(NVL(man_hours, 0)), 0) AS total_man_hours, COALESCE(SUM(NVL(inspector_man_hours, 0)), 0) AS total_inspector_man_hours, COALESCE(SUM(NVL(man_hours, 0) * NVL(man_require, 0)), 0) + COALESCE(SUM(NVL(inspector_man_hours, 0) * NVL(inspector_man_require, 0)), 0) AS Total_Hours\r\n"
	     + "FROM WO_task_card_item WHERE TASK_CARD = ? AND WO = ?";
	    
	    String sqlStatus = 
	    	" SELECT STATUS FROM WO_TASK_CARD WHERE WO =? and TASK_CARD = ?";
	    
	    String sqlCategory = 
		    	" SELECT WO_CATEGORY FROM WO WHERE WO =?";
	    
	    String sqlMark = "UPDATE WO_TASK_CARD SET INTERFACE_FLAG = null WHERE INTERFACE_TRANSFERRED_DATE IS NULL AND TASK_CARD = ? AND WO = ?";

	    PreparedStatement pstmt1 = null;
	    ResultSet rs1 = null;

	    PreparedStatement pstmt2 = null;
	    ResultSet rs2 = null;

	    PreparedStatement pstmt3 = null;
	    ResultSet rs3 = null;
	    
	    PreparedStatement pstmt4 = null;
	    ResultSet rs4 = null;
	    
	    PreparedStatement pstmt5 = null;
	    ResultSet rs5 = null;
	    
	    PreparedStatement pstmt6 = null;
	    ResultSet rs6 = null;


	    try {
	      pstmt1 = con.prepareStatement(sqlTaskCard);
	      pstmt2 = con.prepareStatement(sqlItem);
	      pstmt3 = con.prepareStatement(sqlWork);
	      pstmt4 = con.prepareStatement(sqlStatus);
	      pstmt5 = con.prepareStatement(sqlCategory);
	      pstmt6 = con.prepareStatement(sqlMark);

	      if (MaxRecord != null && !MaxRecord.isEmpty()) {
	        pstmt1.setString(1, MaxRecord);
	      }

	      rs1 = pstmt1.executeQuery();

	      if (rs1 != null) {
	        //loop per line
	        while (rs1.next()) {
	          logger.info("Processing WO Task Card: " + rs1.getString(5) + ", WO: " +rs1.getString(4));
	          INT6_SND req = new INT6_SND();
	          orlist = new ArrayList<OrderSND>();
	          req.setOrder(orlist);
	          OrderSND Inbound = new OrderSND();
	          oplist = new ArrayList<OperationSND>();
	          Inbound.setOperations(oplist);
	          OperationSND InboundItem = new OperationSND();

	          if (rs1.getString(1) != null && !rs1.getNString(1).isEmpty()) {
	            Inbound.setSapOrderNumber(rs1.getString(1));
	          } else {
	            Inbound.setSapOrderNumber("");
	          }

	          if (rs1.getString(2) != null && !rs1.getNString(2).isEmpty()) {
	            InboundItem.setTcDescription(rs1.getString(2));
	          } else {
	            InboundItem.setTcDescription("");
	          }

	          Inbound.setTraxWO(rs1.getString(4));
	          InboundItem.setTcNumber(rs1.getString(5));

	          if (rs1.getString(7) != null && !rs1.getNString(5).isEmpty()) {
	            logger.info(
	              "Using ESD_RFO: " +
	              rs1.getString(7) +
	              ", WO Task Card: " +
	              rs1.getString(5) +
	              ", WO: " +
	              rs1.getString(4)
	            );

	            Inbound.setSapOrderNumber(rs1.getString(7));
	          }
	          
	          pstmt5.setString(1, Inbound.getTraxWO());
	          rs5 = pstmt5.executeQuery();
	          
	          
	          if (rs5 != null && rs5.next()) {
	        	    logger.info("Category of the WO: " + rs5.getString(1));
	        	    
	        	    InboundItem.setTcCategory(rs5.getString(1));
	  	          Inbound.getOperations().add(InboundItem);
	        	}else {
	          if (rs5 != null && !rs5.isClosed()) {
	        	    rs5.close();
	        	}
	        	}
	          
	          
	          pstmt4.setString(1, Inbound.getTraxWO());
	          pstmt4.setString(2, InboundItem.getTcNumber());
	          rs4 = pstmt4.executeQuery();
	          
	          String deletionIndicator = "N";
	          
	          if (rs4 != null && rs4.next()) {
	        	    logger.info("Status of the WO: " + rs4.getString(1));

	        	    if (!"OPEN".equals(rs4.getString(1))) {
	        	        deletionIndicator = "Y";
	        	    }
	        	}
	          if (rs4 != null && !rs4.isClosed()) {
	        	    rs4.close();
	        	}
	          InboundItem.setDeletionIndicator(deletionIndicator);
	          Inbound.getOperations().add(InboundItem);


	          pstmt2.setString(1, Inbound.getTraxWO());
	          pstmt2.setString(2, InboundItem.getTcNumber());
	          
	          req.getOrder().add(Inbound);
	          Inbound.getOperations().add(InboundItem);
	          list.add(req);

	          rs2 = pstmt2.executeQuery();
	          
	          DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.S");

	          LocalDateTime storedDateTime = null;
	      
	          if (rs2 != null) {
	        	    //loop per line
	        	  while (rs2.next()) {
	        	      logger.info("Processing WO Task Card Item Operation Number: " + rs2.getString(1));
	        	      
	        	       LocalDateTime currentDateTime = LocalDateTime.parse(rs2.getString(2), formatter);


	        	      if (storedDateTime == null || currentDateTime.isAfter(storedDateTime)) {
	        	            if (rs2.getString(1) != null && !rs2.getString(1).isEmpty()) {
	        	                InboundItem.setOperationNumber(rs2.getString(1));
	        	            }
	        	            storedDateTime = currentDateTime;
	        	        } else {
	        	            InboundItem.setOperationNumber("");
	        	        }

	        	      //InboundItem.setDeletionIndicator("");

	        	      Integer hours = 0;

	        	      pstmt3.setString(1, InboundItem.getTcNumber());
	        	      pstmt3.setString(2, Inbound.getTraxWO());

	        	      rs3 = pstmt3.executeQuery();

	        	      if (rs3 != null) {
	        	        // LOOP EACH LINE
	        	        if (rs3.next()) {
	        	          logger.info("MECH HRS: " + rs3.getString(1) + " INSP HRS: " + rs3.getString(2) + " TOTAL HRS: " + rs3.getString(3));
	        	          if (rs3.getString(1) != null && !rs3.getString(1).isEmpty()) {
	        	            hours = hours + new BigDecimal(rs3.getString(3)).intValue();
	        	          } // hrs check
	        	        }
	        	      }

	        	      if (rs3 != null && !rs3.isClosed()) rs3.close();

	        	      //MAN HOURS
	        	      String manHours = "hh";
	        	      String hour = "00";

	        	      if (hours.intValue() != 0) {
	        	        hour = hours.toString();
	        	      }
	        	      manHours = manHours.replaceAll("hh", hour);

	        	      InboundItem.setStandardManHours(manHours);

	        	      Inbound.getOperations().add(InboundItem);
	        	    }
	        	  }
	          
	          if (rs2 != null && !rs2.isClosed()) 
	        	  rs2.close();
	          //list.add(req);
	          
	          pstmt6.setString(1, InboundItem.getTcNumber());
	          pstmt6.setString(2, Inbound.getTraxWO());
	          
	          pstmt6.executeQuery();
	         
	          
	        }
	       
	      }
	    } catch (Exception e) {
	      e.printStackTrace();
	      executed = e.toString();
	      Import_TC_MHR_Controller.addError(e.toString());

	      logger.severe(executed);
	      throw new Exception("Issue found");
	    } finally {
	      if (rs1 != null && !rs1.isClosed()) rs1.close();
	      if (pstmt1 != null && !pstmt1.isClosed()) pstmt1.close();
	      if (pstmt2 != null && !pstmt2.isClosed()) pstmt2.close();
	      if (pstmt3 != null && !pstmt3.isClosed()) pstmt3.close();
	      if (pstmt4 != null && !pstmt4.isClosed()) pstmt4.close();
		  if (pstmt5 != null && !pstmt5.isClosed()) pstmt5.close();
	    }
	    logger.info("DONE " + list.size());
	    return list;
	  }

	  public String setOpsLine(String opsLine, String email) throws Exception {
	    String Executed = "OK";

	    String query =
	      "INSERT INTO OPS_LINE_EMAIL_MASTER (OPS_LINE, \"EMAIL\") VALUES (?, ?)";

	    PreparedStatement ps = null;

	    try {
	      if (con == null || con.isClosed()) {
	        con = DataSourceClient.getConnection();
	        logger.severe(
	          "The connection was stablished successfully with status: " +
	          String.valueOf(!con.isClosed())
	        );
	      }

	      ps = con.prepareStatement(query);

	      ps.setString(1, opsLine);
	      ps.setString(2, email);

	      ps.executeUpdate();
	    } catch (SQLException sqle) {
	      logger.severe(
	        "A SQLException" +
	        " occurred executing the query to get the location site capacity. " +
	        "\n error: " +
	        ErrorType.BAD_REQUEST +
	        "\nmessage: " +
	        sqle.getMessage()
	      );
	      throw new Exception(
	        "A SQLException" +
	        " occurred executing the query to get the location site capacity. " +
	        "\n error: " +
	        ErrorType.BAD_REQUEST +
	        "\nmessage: " +
	        sqle.getMessage()
	      );
	    } catch (NullPointerException npe) {
	      logger.severe(
	        "A NullPointerException occurred executing the query to get the location site capacity. " +
	        "\n error: " +
	        ErrorType.BAD_REQUEST +
	        "\nmessage: " +
	        npe.getMessage()
	      );
	      throw new Exception(
	        "A NullPointerException occurred executing the query to get the location site capacity. " +
	        "\n error: " +
	        ErrorType.BAD_REQUEST +
	        "\nmessage: " +
	        npe.getMessage()
	      );
	    } catch (Exception e) {
	      logger.severe(
	        "An Exception occurred executing the query to get the location site capacity. " +
	        "\n error: " +
	        ErrorType.INTERNAL_SERVER_ERROR +
	        "\nmessage: " +
	        e.getMessage()
	      );
	      throw new Exception(
	        "An Exception occurred executing the query to get the location site capacity. " +
	        "\n error: " +
	        ErrorType.INTERNAL_SERVER_ERROR +
	        "\nmessage: " +
	        e.getMessage()
	      );
	    } finally {
	      try {
	        if (ps != null && !ps.isClosed()) ps.close();
	      } catch (SQLException e) {
	        logger.severe("An error ocurrer trying to close the statement");
	      }
	    }

	    return Executed;
	  }

	  public String deleteOpsLine(String opsline) throws Exception {
	    String Executed = "OK";

	    String query = "DELETE OPS_LINE_EMAIL_MASTER where \"OPS_LINE\" = ?";

	    PreparedStatement ps = null;

	    try {
	      if (con == null || con.isClosed()) {
	        con = DataSourceClient.getConnection();
	        logger.info(
	          "The connection was stablished successfully with status: " +
	          String.valueOf(!con.isClosed())
	        );
	      }

	      ps = con.prepareStatement(query);
	      ps.setString(1, opsline);
	      ps.executeUpdate();
	    } catch (SQLException sqle) {
	      logger.severe(
	        "A SQLException" +
	        " occurred executing the query to get the location site capacity. " +
	        "\n error: " +
	        ErrorType.BAD_REQUEST +
	        "\nmessage: " +
	        sqle.getMessage()
	      );
	      throw new Exception(
	        "A SQLException" +
	        " occurred executing the query to get the location site capacity. " +
	        "\n error: " +
	        ErrorType.BAD_REQUEST +
	        "\nmessage: " +
	        sqle.getMessage()
	      );
	    } catch (NullPointerException npe) {
	      logger.severe(
	        "A NullPointerException occurred executing the query to get the location site capacity. " +
	        "\n error: " +
	        ErrorType.BAD_REQUEST +
	        "\nmessage: " +
	        npe.getMessage()
	      );
	      throw new Exception(
	        "A NullPointerException occurred executing the query to get the location site capacity. " +
	        "\n error: " +
	        ErrorType.BAD_REQUEST +
	        "\nmessage: " +
	        npe.getMessage()
	      );
	    } catch (Exception e) {
	      logger.severe(
	        "An Exception occurred executing the query to get the location site capacity. " +
	        "\n error: " +
	        ErrorType.INTERNAL_SERVER_ERROR +
	        "\nmessage: " +
	        e.getMessage()
	      );
	      throw new Exception(
	        "An Exception occurred executing the query to get the location site capacity. " +
	        "\n error: " +
	        ErrorType.INTERNAL_SERVER_ERROR +
	        "\nmessage: " +
	        e.getMessage()
	      );
	    } finally {
	      try {
	        if (ps != null && !ps.isClosed()) ps.close();
	      } catch (SQLException e) {
	        logger.severe("An error ocurrer trying to close the statement");
	      }
	    }

	    return Executed;
	  }

	  public String getemailByOpsLine(String opsLine) throws Exception {
	    ArrayList<String> groups = new ArrayList<String>();

	    String query = "", group = "";
	    if (opsLine != null && !opsLine.isEmpty()) {
	      query =
	        "Select \\\"EMAIL\\\", OPS_LINE FROM OPS_LINE_EMAIL_MASTER where OPS_LINE = ?";
	    } else {
	      query = " Select \"EMAIL\", OPS_LINE FROM OPS_LINE_EMAIL_MASTER";
	    }
	    PreparedStatement ps = null;

	    try {
	      if (con == null || con.isClosed()) {
	        con = DataSourceClient.getConnection();
	        logger.info(
	          "The connection was stablished successfully with status: " +
	          String.valueOf(!con.isClosed())
	        );
	      }

	      ps = con.prepareStatement(query);
	      if (opsLine != null && !opsLine.isEmpty()) {
	        ps.setString(1, opsLine);
	      }

	      ResultSet rs = ps.executeQuery();

	      if (rs != null) {
	        while (rs.next()) {
	          groups.add(
	            "OPS_LINE: " + rs.getString(2) + " EMAIL: " + rs.getString(1)
	          );
	        }
	      }
	      rs.close();
	    } catch (SQLException sqle) {
	      logger.severe(
	        "A SQLException" +
	        " occurred executing the query to get the location site capacity. " +
	        "\n error: " +
	        ErrorType.BAD_REQUEST +
	        "\nmessage: " +
	        sqle.getMessage()
	      );
	      throw new Exception(
	        "A SQLException" +
	        " occurred executing the query to get the location site capacity. " +
	        "\n error: " +
	        ErrorType.BAD_REQUEST +
	        "\nmessage: " +
	        sqle.getMessage()
	      );
	    } catch (NullPointerException npe) {
	      logger.severe(
	        "A NullPointerException occurred executing the query to get the location site capacity. " +
	        "\n error: " +
	        ErrorType.BAD_REQUEST +
	        "\nmessage: " +
	        npe.getMessage()
	      );
	      throw new Exception(
	        "A NullPointerException occurred executing the query to get the location site capacity. " +
	        "\n error: " +
	        ErrorType.BAD_REQUEST +
	        "\nmessage: " +
	        npe.getMessage()
	      );
	    } catch (Exception e) {
	      logger.severe(
	        "An Exception occurred executing the query to get the location site capacity. " +
	        "\n error: " +
	        ErrorType.INTERNAL_SERVER_ERROR +
	        "\nmessage: " +
	        e.getMessage()
	      );
	      throw new Exception(
	        "An Exception occurred executing the query to get the location site capacity. " +
	        "\n error: " +
	        ErrorType.INTERNAL_SERVER_ERROR +
	        "\nmessage: " +
	        e.getMessage()
	      );
	    } finally {
	      try {
	        if (ps != null && !ps.isClosed()) ps.close();
	      } catch (SQLException e) {
	        logger.severe("An error ocurrer trying to close the statement");
	      }
	    }

	    for (String g : groups) {
	      group = group + g + "\n";
	    }

	    return group;
	  }

	  public String getemailByOnlyOpsLine(String opsLine) {
	    String email = "ERROR";

	    String query =
	      " Select \"EMAIL\", cost_centre FROM OPS_LINE_EMAIL_MASTER where OPS_LINE = ?";

	    PreparedStatement ps = null;

	    try {
	      if (con == null || con.isClosed()) {
	        con = DataSourceClient.getConnection();
	        logger.info(
	          "The connection was stablished successfully with status: " +
	          String.valueOf(!con.isClosed())
	        );
	      }

	      ps = con.prepareStatement(query);
	      if (opsLine != null && !opsLine.isEmpty()) {
	        ps.setString(1, opsLine);
	      }

	      ResultSet rs = ps.executeQuery();

	      if (rs != null) {
	        while (rs.next()) {
	          email = rs.getString(1);
	        }
	      }
	      rs.close();
	    } catch (Exception e) {
	      email = "ERROR";
	    } finally {
	      try {
	        if (ps != null && !ps.isClosed()) ps.close();
	      } catch (SQLException e) {
	        logger.severe("An error ocurrer trying to close the statement");
	      }
	    }

	    return email;
	  }
	  public OpsLineEmail getOpsLineStaffName(String wo, String taskCard) {
			String query = "";
			OpsLineEmail OpsLineEmail = new OpsLineEmail();

			query =
			  "SELECT rm.RELATION_CODE,rm.NAME,w.ops_line,wtc.INTERFACE_FLAG\r\n" +
			  "FROM WO w, WO_TASK_CARD wtc, relation_master rm \r\n" +
			  "WHERE w.wo = ? AND wtc.task_card = ? AND w.wo = wtc.wo AND w.created_by = rm.relation_code";

			PreparedStatement ps = null;

			try {
			  if (con == null || con.isClosed()) {
				con = DataSourceClient.getConnection();
				logger.info(
				  "The connection was stablished successfully with status: " +
				  String.valueOf(!con.isClosed())
				);
			  }

			  ps = con.prepareStatement(query);

			  ps.setString(1, wo);
			  ps.setString(2, taskCard);

			  ResultSet rs = ps.executeQuery();

			  if (rs != null) {
				while (rs.next()) {
				  if (rs.getString(1) != null && !rs.getString(1).isEmpty()) {
					OpsLineEmail.setRelationCode(rs.getString(1));
				  } else {
					OpsLineEmail.setRelationCode("");
				  }

				  if (rs.getString(2) != null && !rs.getString(2).isEmpty()) {
					OpsLineEmail.setName(rs.getString(2));
				  } else {
					OpsLineEmail.setName("");
				  }

				  if (rs.getString(3) != null && !rs.getString(3).isEmpty()) {
					OpsLineEmail.setOpsLine(rs.getString(3));
				  } else {
					OpsLineEmail.setOpsLine("");
				  }

				  OpsLineEmail.setEmail(
					getemailByOnlyOpsLine(OpsLineEmail.getOpsLine())
				  );

				  if (rs.getString(4) != null && !rs.getString(4).isEmpty()) {
					OpsLineEmail.setFlag(rs.getString(4));
				  } else {
					OpsLineEmail.setFlag("");
				  }
				}
			  }
			  rs.close();
			} catch (Exception e) {
			  logger.severe(e.toString());
			  OpsLineEmail.setOpsLine("");
			  OpsLineEmail.setName("");
			  OpsLineEmail.setRelationCode("");
			  OpsLineEmail.setFlag("");
			} finally {
			  try {
				if (ps != null && !ps.isClosed()) ps.close();
			  } catch (SQLException e) {
				logger.severe("An error ocurrer trying to close the statement");
			  }
			}

			return OpsLineEmail;
		  }
		  
		 public boolean lockAvailable(String notificationType) {
			InterfaceLockMaster lock;
			try {
				lock = em
						.createQuery("SELECT i FROM InterfaceLockMaster i WHERE i.interfaceType = :type", InterfaceLockMaster.class)
						.setParameter("type", notificationType)
						.getSingleResult();
				em.refresh(lock);
			} catch (NoResultException e) {
				lock = new InterfaceLockMaster();
				lock.setInterfaceType(notificationType);
				lock.setLocked(new BigDecimal(0)); 
				insertData(lock);
				return true;
			}

			if (lock.getLocked().intValue() == 1) {
				LocalDateTime now = LocalDateTime.now();
				LocalDateTime lockTime = LocalDateTime.ofInstant(lock.getLockedDate().toInstant(), ZoneId.systemDefault());
				Duration duration = Duration.between(lockTime, now);
				if (duration.getSeconds() >= lock.getMaxLock().longValue()) {
					lock.setLocked(new BigDecimal(0)); 
					insertData(lock);
					return true;
				}
				return false; 
			} else {
				lock.setLocked(new BigDecimal(1)); 
				insertData(lock);
				return true;
			}
		  }

		  private <T> void insertData(T data) {
			try {
			  if (!em.getTransaction().isActive()) em.getTransaction().begin();
			  em.merge(data);
			  em.getTransaction().commit();
			} catch (Exception e) {
			  logger.severe(e.toString());
			}
		  }

		  public void lockTable(String notificationType) {
			em.getTransaction().begin();
			InterfaceLockMaster lock = em.createQuery("SELECT i FROM InterfaceLockMaster i where i.interfaceType = :type",InterfaceLockMaster.class)
			  .setParameter("type", notificationType)
			  .getSingleResult();
			lock.setLocked(new BigDecimal(1));    lock.setLockedDate(
			  Date.from(LocalDateTime.now().atZone(ZoneId.systemDefault()).toInstant())
			);
			InetAddress address = null;

			try {
			  address = InetAddress.getLocalHost();
			} catch (UnknownHostException e) {
			  logger.info(e.getMessage());
			}

			lock.setCurrentServer(address.getHostName());
			em.merge(lock);
			em.getTransaction().commit();
		  }

		  public void unlockTable(String notificationType) {
			em.getTransaction().begin();
			InterfaceLockMaster lock = em
			  .createQuery(
				"SELECT i FROM InterfaceLockMaster i where i.interfaceType = :type",
				InterfaceLockMaster.class
			  )
			  .setParameter("type", notificationType)
			  .getSingleResult();
			lock.setLocked(new BigDecimal(0));
			lock.setUnlockedDate(
			  Date.from(LocalDateTime.now().atZone(ZoneId.systemDefault()).toInstant())
			);

			em.merge(lock);
			em.getTransaction().commit();
		  }
}
