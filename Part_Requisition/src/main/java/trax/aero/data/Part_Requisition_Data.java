package trax.aero.data;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;

import trax.aero.controller.Part_Requisition_Controller;
import trax.aero.exception.CustomizeHandledException;
import trax.aero.logger.LogManager;
import trax.aero.pojo.INT13_SND;
import trax.aero.pojo.OrderSND;
import trax.aero.pojo.OrderComponentSND;
import trax.aero.pojo.INT13_TRAX;
import trax.aero.utils.DataSourceClient;

public class Part_Requisition_Data {
	
	EntityManagerFactory factory;
	EntityManager em;
	String executed;
	private Connection con;
	
	final String MaxRecord = System.getProperty("Part_REQ_MaxRecord");
	Logger logger = LogManager.getLogger("Part_REQ");
	
	public Part_Requisition_Data(String mark) {
		try {
			if(this.con == null || this.con.isClosed()) {
				this.con = DataSourceClient.getConnection();
				logger.info("The connection was stablished successfully with status: " +String.valueOf(!this.con.isClosed()));
			}
		} catch(SQLException e) {
			logger.severe("An error ocurred getting the status of the connection");
			Part_Requisition_Controller.addError(e.toString());
		} catch (CustomizeHandledException e1) {
			Part_Requisition_Controller.addError(e1.toString());
		} catch (Exception e) {
			Part_Requisition_Controller.addError(e.toString());
		}
	}
	
	public Part_Requisition_Data(){
		try {
			if(this.con == null || this.con.isClosed()) {
				this.con = DataSourceClient.getConnection();
				logger.info("The connection was stablished successfully with status: " + String.valueOf(!this.con.isClosed()));
			}
		} catch (SQLException e) {
		      logger.severe("An error occured getting the status of the connection");
		      Part_Requisition_Controller.addError(e.toString());
		    } catch (CustomizeHandledException e1) {
		      Part_Requisition_Controller.addError(e1.toString());
		    } catch (Exception e) {
		     Part_Requisition_Controller.addError(e.toString());
		    }
		factory = Persistence.createEntityManagerFactory("TraxStandalonaDS");
		em = factory.createEntityManager();
	}
	
	public Connection getCon() {
		return con;
	}
	
	public String markTransaction(List<INT13_TRAX> request) {
		executed = "OK";
		
		String sqlDate = "UPDATE REQUISITION_HEADER SET INTERFACE_TRANSFERRED_DATE_ESD = sysdate WHERE INTERFACE_TRANSFERRED_DATE_ESD IS NULL AND REQUISITION = ?";
		String sqlDate2 = "UPDATE REQUISITION_DETAIL SET INTERFACE_TRANSFERRED_DATE_ESD = sysdate WHERE INTERFACE_TRANSFERRED_DATE_ESD IS NULL AND REQUISITION = ? AND REQUISITION_LINE = ?";
		String sqlPR = "UPDATE REQUISITION_DETAIL SET PR_NO = ? AND PR_ITEM = ? WHERE REQUISITION = ? AND REQUISITION_LINE = ?";
		String sqlUpdateREQD = "UPDATE REQUISITION_DETAIL SET STATUS = 'CLOSED' WHERE REQUISITION = ? AND REQUISITON_LINE = ? AND PR_NO IS NOT NULL AND PR_ITEM IS NOT NULL";
		String sqlUpdateREQH = "UPDATE REQUISITION_HEADER RH SET RH.STATUS = 'CLOSED' WHERE RH.REQUISITION IN (SELECT RD.REQUISITION FROM RD.REQUISITION WHERE RD.PR_NO IS NOT NULL AND RD.PR_ITEM IS NOT NULL GROUP BY RD.REQUISITION HAVING COUNT(*) = COUNT(CASE WHEN RD.STATUS = 'CLOSED' THEN 1 END))";
		String sqlCheckWOStatus = "SELECT W.STATUS FROM WO W, REQUISITION_HEADER R WHERE R.REQUISITION = ? and W.WO = R.WO";
		String sqlUpdateReqStatus = "UPDTE REQUISITION_HEADER SET STATUS = 'CLOSED' WHERE REQUISITION = ?";
		
		try(PreparedStatement pstmt1 = con.prepareStatement(sqlDate);
			PreparedStatement pstmt2 = con.prepareStatement(sqlDate2);
			PreparedStatement pstmt3 = con.prepareStatement(sqlPR);
			PreparedStatement pstmt4 = con.prepareStatement(sqlUpdateREQD);
		    PreparedStatement pstmt5 = con.prepareStatement(sqlUpdateREQH);
			PreparedStatement pstmt6 = con.prepareStatement(sqlCheckWOStatus);
			PreparedStatement pstmt7 = con.prepareStatement(sqlUpdateReqStatus)){
			
			for(INT13_TRAX r: request) {
				if(r != null) {
					pstmt1.setString(1, r.getRequisition());
					pstmt1.executeUpdate();
					
					pstmt2.setString(1, r.getRequisition());
					pstmt2.setString(2, r.getRequisitionLine());
					pstmt2.executeUpdate();
					
					if (r.getPRnumber() != null && !r.getPRnumber().isEmpty() && r.getPRitem() != null && !r.getPRitem().isEmpty()) {
						pstmt3.setString(1, r.getPRnumber());
						pstmt3.setString(2, r.getPRitem());
						pstmt3.setString(3, r.getRequisition());
						pstmt3.setString(4, r.getRequisitionLine());
						pstmt3.executeUpdate();
					}
					
					if (r.getPRnumber() != null && !r.getPRnumber().isEmpty() && r.getPRitem() != null && !r.getPRitem().isEmpty()) {
		                pstmt4.setString(1, r.getRequisition());
		                pstmt4.setString(2, r.getRequisitionLine());
		                pstmt4.executeUpdate();
		            }
					
					pstmt5.executeUpdate();
					
					pstmt6.setString(1, r.getRequisition());
					ResultSet rs = pstmt6.executeQuery();
					if(rs.next() && "CLOSED".equalsIgnoreCase(rs.getString(1))) {
						pstmt7.setString(1, r.getRequisition());
						pstmt7.executeUpdate();
					}
					
					if (!r.getExceptionId().equalsIgnoreCase("53")) {
						executed = "Request PR number: " + r.getPRnumber() + ", Error Code: " + r.getPRitem() + ", Error Code: " + r.getExceptionId() + ", Remarks: " + r.getExceptionDetail();
						Part_Requisition_Controller.addError(executed);
					}
					
				}
			}
			
		} catch (SQLException e) {
	        executed = e.toString();
	        Part_Requisition_Controller.addError(executed);
	        logger.severe(executed);
	    } 
		
		return executed;
	}
	
	
	public ArrayList<INT13_SND> getRequisiton() throws Exception{
		executed = "OK";
		
		ArrayList<INT13_SND> list = new ArrayList<INT13_SND>();
		ArrayList<OrderSND> orlist = new ArrayList<OrderSND>();
		ArrayList<OrderComponentSND> oclist = new ArrayList<OrderComponentSND>();
		
		String sqlRequisition ="SELECT DISTINCT RD.REQUISITION, RD.REQUISITION_LINE, RD.PN, WT.PN_SN, RD.QTY_REQUIRE, R.WO. R.TASK_CARD, W.LOCATION, \r\n" +
							   "W.RFO_NO, WTI.OPS_NO, RD.PR_NO, RD.PR_ITEM FROM REQUISITION_DETAIL RD INNER JOIN REQUISITION_HEADER R ON R.REQUISITION = RD.REQUISITION \r\n" +
							   "INNER JOIN WO W ON W.WO = R.WO INNER JOIN WO_TASK_CARD WT ON WT.WO = R.WO AND WT.TASK_CARD = R.TASK_CARD \r\n" +
							   "INNER JOIN WO_TASK_CARD_ITEM WTI ON WTI.WO = R.WO AND WT.TASK_CARD = R.TASK_CARD WHERE WT.PN = RD.PN AND W.RFO_NO IS NOT NULL";
		
		if (MaxRecord != null && !MaxRecord.isEmpty()) {
			sqlRequisition = "SELECT * FROM (" + sqlRequisition;
		}
		
		if (MaxRecord != null && !MaxRecord.isEmpty()) {
			sqlRequisition = sqlRequisition + " ) WHERE ROWNUM <= ?";
		}
		
		 PreparedStatement pstmt1 = null;
		 ResultSet rs1 = null;
		
		try {
			pstmt1 = con.prepareStatement(sqlRequisition);
			
			if (MaxRecord != null && !MaxRecord.isEmpty()) {
		        pstmt1.setString(1, MaxRecord);
		      }

		      rs1 = pstmt1.executeQuery();
		      
		      if(rs1 != null) {
		    	  while(rs1.next()) {
		    		  logger.info("Processing Requisition: " + rs1.getString(1) + ", Requisition Line: " + rs1.getString(2));
		    		  INT13_SND req = new INT13_SND();
		    		  orlist = new ArrayList<OrderSND>();
		    		  req.setOrder(orlist);
		    		  OrderSND Inbound = new OrderSND();
		    		  oclist = new ArrayList<OrderComponentSND>();
		    		  Inbound.setComponents(oclist);
		    		  OrderComponentSND InboundC = new OrderComponentSND();
		    		  
		    		  if (rs1.getString(1) != null && !rs1.getNString(1).isEmpty()) {
		    			  InboundC.setRequisition(rs1.getString(1));
		    		  } else {
		    			  InboundC.setRequisition("");
		    		  }
		    		  
		    		  if(rs1.getString(2) != null && !rs1.getNString(2).isEmpty()) {
		    			  InboundC.setRequisitionLine(rs1.getString(2));
		    		  } else {
		    			  InboundC.setRequisitionLine("");
		    		  }
		    		 
		    		  if (rs1.getString(6) != null && rs1.getNString(6).isEmpty() && rs1.getString(8) != null && rs1.getNString(8).isEmpty()) {
		    			  logger.info("Getting Task card: " + rs1.getString(6) + ", ESD RFO: " + rs1.getString(8) + ", Operation Number: " + rs1.getString(9));
		    			  
		    			  InboundC.setTC_number(rs1.getString(6));
		    			  Inbound.setOrderNO(rs1.getString(8));
		    			  InboundC.setACT(rs1.getString(9));
		    			  InboundC.setWO_location(rs1.getString(7));
		    		  }
		    		  
		    		  logger.info("Checking PN: " + rs1.getString(3) + ", SN: " + rs1.getString(4) + ", QTY: " + rs1.getString(5) + ", PR_Number: " + rs1.getString(10) + ", PR_Item: " + rs1.getString(11));
		    		  
		    		  InboundC.setMaterialPartNumber(rs1.getString(3));
		    		  InboundC.setWoSN(rs1.getString(4));
		    		  InboundC.setQuantity(rs1.getString(5));
		    		  InboundC.setPRnumber(rs1.getString(10));
		    		  InboundC.setPRitem(rs1.getString(11));	
		    		  
		    		  list.add(req);
		    	  }
		      }
     
		} catch (Exception e) {
		      e.printStackTrace();
		      executed = e.toString();
		      Part_Requisition_Controller.addError(e.toString());

		      logger.severe(executed);
		      throw new Exception("Issue found");
		}finally {
		      if (rs1 != null && !rs1.isClosed()) rs1.close();
		      if (pstmt1 != null && !pstmt1.isClosed()) pstmt1.close();
		}
		
		logger.info("DONE " + list.size());
	    return list;
		
	}

}
