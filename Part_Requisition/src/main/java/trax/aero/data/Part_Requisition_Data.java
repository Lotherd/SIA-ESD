package trax.aero.data;

import java.math.BigDecimal;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.NoResultException;
import javax.persistence.Persistence;
import javax.xml.bind.JAXBException;

import trax.aero.controller.Part_Requisition_Controller;
import trax.aero.exception.CustomizeHandledException;
import trax.aero.logger.LogManager;
import trax.aero.model.InterfaceLockMaster;
import trax.aero.pojo.INT13_SND;
import trax.aero.pojo.OrderSND;
import trax.aero.pojo.OrderComponentSND;
import trax.aero.pojo.INT13_TRAX;
import trax.aero.pojo.OpsLineEmail;
import trax.aero.utils.DataSourceClient;
import trax.aero.utils.ErrorType;

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
		factory = Persistence.createEntityManagerFactory("TraxQADS");
		em = factory.createEntityManager();
	}
	
	public Connection getCon() {
		return con;
	}
	
	public String markSendData() throws JAXBException
	{
	  INT13_TRAX request = new INT13_TRAX();
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
	
	public String markTransaction(INT13_TRAX request) {
		executed = "OK";
		
		String sqlDate = "UPDATE REQUISITION_HEADER SET INTERFACE_TRANSFERRED_DATE_ESD = sysdate WHERE INTERFACE_TRANSFERRED_DATE_ESD IS NULL AND REQUISITION = ?";
		String sqlDate2 = "UPDATE REQUISITION_DETAIL SET INTERFACE_TRANSFERRED_DATE_ESD = sysdate WHERE INTERFACE_TRANSFERRED_DATE_ESD IS NULL AND REQUISITION = ? AND REQUISITION_LINE = ?";
		String sqlPR = "UPDATE REQUISITION_DETAIL SET PR_NO = ?, PR_ITEM = ? WHERE REQUISITION = ? AND REQUISITION_LINE = ?";
		String sqlUpdateREQD = "UPDATE REQUISITION_DETAIL SET STATUS = 'CLOSED' WHERE REQUISITION = ? AND REQUISITION_LINE = ? AND PR_NO IS NOT NULL AND PR_ITEM IS NOT NULL";
		String sqlUpdateREQH = "UPDATE REQUISITION_HEADER RH SET RH.STATUS = 'CLOSED' WHERE RH.REQUISITION IN (SELECT RD.REQUISITION FROM REQUISITION_DETAIL RD WHERE RD.PR_NO IS NOT NULL AND RD.PR_ITEM IS NOT NULL GROUP BY RD.REQUISITION HAVING COUNT(*) = COUNT(CASE WHEN RD.STATUS = 'CLOSED' THEN 1 END))";
		String sqlCheckWOStatus = "SELECT W.STATUS FROM WO W, REQUISITION_HEADER R WHERE R.REQUISITION = ? and W.WO = R.WO";
		String sqlUpdateReqStatus = "UPDATE REQUISITION_HEADER SET STATUS = 'CLOSED' WHERE REQUISITION = ?";
		
		try(PreparedStatement pstmt1 = con.prepareStatement(sqlDate);
			PreparedStatement pstmt2 = con.prepareStatement(sqlDate2);
			PreparedStatement pstmt3 = con.prepareStatement(sqlPR);
			PreparedStatement pstmt4 = con.prepareStatement(sqlUpdateREQD);
		    PreparedStatement pstmt5 = con.prepareStatement(sqlUpdateREQH);
			PreparedStatement pstmt6 = con.prepareStatement(sqlCheckWOStatus);
			PreparedStatement pstmt7 = con.prepareStatement(sqlUpdateReqStatus)){
			
				if(request != null) {
					pstmt1.setString(1, request.getRequisition());
					pstmt1.executeUpdate();
					
					pstmt2.setString(1, request.getRequisition());
					pstmt2.setString(2, request.getRequisitionLine());
					pstmt2.executeUpdate();
					
					if (request.getPRnumber() != null && !request.getPRnumber().isEmpty() && request.getPRitem() != null && !request.getPRitem().isEmpty()) {
						pstmt3.setString(1, request.getPRnumber());
						pstmt3.setString(2, request.getPRitem());
						pstmt3.setString(3, request.getRequisition());
						pstmt3.setString(4, request.getRequisitionLine());
						pstmt3.executeUpdate();
					}
					
					if (request.getPRnumber() != null && !request.getPRnumber().isEmpty() && request.getPRitem() != null && !request.getPRitem().isEmpty()) {
		                pstmt4.setString(1, request.getRequisition());
		                pstmt4.setString(2, request.getRequisitionLine());
		                pstmt4.executeUpdate();
		            }
					
					pstmt5.executeUpdate();
					
					pstmt6.setString(1, request.getRequisition());
					ResultSet rs = pstmt6.executeQuery();
					if(rs.next() && "CLOSED".equalsIgnoreCase(rs.getString(1))) {
						pstmt7.setString(1, request.getRequisition());
						pstmt7.executeUpdate();
					}
					
					if (!request.getExceptionId().equalsIgnoreCase("53")) {
						executed = "Request PR number: " + request.getPRnumber() + ", Error Code: " + request.getPRitem() + ", Error Code: " + request.getExceptionId() + ", Remarks: " + request.getExceptionDetail();
						Part_Requisition_Controller.addError(executed);
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
		
		if (this.con == null || this.con.isClosed()) {
	        try {
	            this.con = DataSourceClient.getConnection(); 
	            if (this.con == null || this.con.isClosed()) {
	                throw new IllegalStateException("No se pudo restablecer la conexión a la base de datos.");
	            }
	            logger.info("Conexión a la base de datos restablecida exitosamente.");
	        } catch (SQLException e) {
	            throw new IllegalStateException("Error al restablecer la conexión a la base de datos.", e);
	        }
	    }
		
		ArrayList<INT13_SND> list = new ArrayList<INT13_SND>();
		ArrayList<OrderSND> orlist = new ArrayList<OrderSND>();
		ArrayList<OrderComponentSND> oclist = new ArrayList<OrderComponentSND>();
		
		String sqlRequisition ="SELECT DISTINCT RD.REQUISITION, RD.REQUISITION_LINE, RD.PN, WS.PN_SN, RD.QTY_REQUIRE, R.WO, R.TASK_CARD, W.LOCATION, \r\n" +
							   "W.RFO_NO, WTI.OPS_NO, RD.PR_NO, RD.PR_ITEM, R.CREATED_BY FROM REQUISITION_DETAIL RD INNER JOIN REQUISITION_HEADER R ON R.REQUISITION = RD.REQUISITION \r\n" +
							   "INNER JOIN WO W ON W.WO = R.WO INNER JOIN WO_TASK_CARD WT ON WT.WO = R.WO AND WT.TASK_CARD = R.TASK_CARD \r\n" +
							   "INNER JOIN WO_TASK_CARD_ITEM WTI ON WTI.WO = R.WO AND WT.TASK_CARD = R.TASK_CARD INNER JOIN WO_SHOP_DETAIL WS ON WS.WO = W.WO \r\n" + 
							   "WHERE RD.STATUS = 'OPEN' AND R.INTERFACE_TRANSFERRED_DATE_ESD IS NULL AND W.RFO_NO IS NOT NULL AND RAISE_PR ='Y'";
		
		String sqlMark = "UPDATE REQUISITION_HEADER SET INTERFACE_TRANSFERRED_DATE_ESD = SYSDATE WHERE REQUISITION = ?";
		
		if (MaxRecord != null && !MaxRecord.isEmpty()) {
			sqlRequisition = "SELECT * FROM (" + sqlRequisition;
		}
		
		if (MaxRecord != null && !MaxRecord.isEmpty()) {
			sqlRequisition = sqlRequisition + " ) WHERE ROWNUM <= ?";
		}
		
		 PreparedStatement pstmt1 = null;
		 ResultSet rs1 = null;
		 PreparedStatement pstmt2 = null;
		 ResultSet rs2 = null;
		
		try {
			pstmt1 = con.prepareStatement(sqlRequisition);
			pstmt2 = con.prepareStatement(sqlMark);
			
			if (MaxRecord != null && !MaxRecord.isEmpty()) {
		        pstmt1.setString(1, MaxRecord);
		      }

		      rs1 = pstmt1.executeQuery();
		      
		      if(rs1 != null) {
		    	  while(rs1.next()) {
		    		  logger.info("Processing Requisition: " + rs1.getString(1) + ", Requisition Line: " + rs1.getString(2) + ", RFO_NO: " + rs1.getString(9));
		    		  INT13_SND req = new INT13_SND();
		    		  orlist = new ArrayList<OrderSND>();
		    		  req.setOrder(orlist);
		    		  OrderSND Inbound = new OrderSND();
		    		  oclist = new ArrayList<OrderComponentSND>();
		    		  Inbound.setComponents(oclist);
		    		  OrderComponentSND InboundC = new OrderComponentSND();
		    		  
		    		  if (rs1.getString(9) != null && !rs1.getNString(9).isEmpty()) {
		    		  Inbound.setOrderNO(rs1.getString(9));
		    		  } else {
		    			  Inbound.setOrderNO("");
		    		  }
		    		  
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
		    		  
		    		  logger.info("OPS_NO: " + rs1.getString(10) + ", Created By " + rs1.getString(13) + ", Location: " + rs1.getString(8));
		    		  
		    		  
		    		  InboundC.setGoodsRecipient(rs1.getString(13));
		    		 
		    		  if (rs1.getString(6) != null && rs1.getNString(6).isEmpty() && rs1.getString(9) != null && rs1.getNString(9).isEmpty()) {
		    			  logger.info("Getting Task card: " + rs1.getString(7) + ", ESD RFO: " + rs1.getString(9) + ", Operation Number: " + rs1.getString(10));
		    			  
		    			  InboundC.setTC_number(rs1.getString(7));
		    			  InboundC.setACT(rs1.getString(10));
		    			  InboundC.setWO_location(rs1.getString(8));
		    		  }
		    		  
		    		  logger.info("Checking PN: " + rs1.getString(3) + ", SN: " + rs1.getString(4) + ", QTY: " + rs1.getString(5) + ", PR_Number: " + rs1.getString(10) + ", PR_Item: " + rs1.getString(11));
		    		  
		    		  InboundC.setMaterialPartNumber(rs1.getString(3));
		    		  if(rs1.getString(4) != null && !rs1.getNString(4).isEmpty()) {
		    			  InboundC.setWoSN(rs1.getString(4));
		    		  } else {
		    			  InboundC.setWoSN("");
		    		  }
		    		  InboundC.setQuantity(rs1.getString(5));
		    		  InboundC.setPRnumber(rs1.getString(11));
		    		  InboundC.setPRitem(rs1.getString(12));	

		    		  
		    		  req.getOrder().add(Inbound);
		    		  Inbound.getComponents().add(InboundC);
		    		  list.add(req);
		    		  
		    		  pstmt2.setString(1, InboundC.getRequisition());
		    		  pstmt2.executeQuery();
		    	  }
		    	 
		      }
		      if (rs1 != null && !rs1.isClosed()) rs1.close();
		      
		      
     
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
	
	public String deleteOpsLine(String opsline) throws Exception{
		String Executed = "OK";
		
		String query = "DELETE OPS_LINE_EMAIL_MASTER where \"OPS_LINE\" = ?";

	    PreparedStatement ps = null;
	    
	    try {
	    	if(con == null || con.isClosed()) {
	    		con = DataSourceClient.getConnection();
	    		logger.info("The connection was stablished successfully with status: " + String.valueOf(!con.isClosed()));
	    	}
	    	
	    	ps = con.prepareStatement(query);
	    	
		    ps.setString(1, opsline);
		    
		    ps.executeUpdate();
	    } catch(SQLException sqle) {
	    	logger.severe("A SQLException" + " occurred executing the query to get the location site capacity. " + "\n error: " + ErrorType.BAD_REQUEST + "\n message: " + sqle.getMessage());
	    	throw new Exception("A SQLException" + " occurred executing the query to get the location site capacity. " + "\n error: " + ErrorType.BAD_REQUEST + "\n message: " + sqle.getMessage());
	    } catch(NullPointerException npe) {
	    	logger.severe("A NullPointerException occurred executing the query to get the location site capacity. " + "\n error: " + ErrorType.BAD_REQUEST + "\n message: " + npe.getMessage());
	    	throw new Exception("A NullPointerException occurred executing the query to get the location site capacity. " + "\n error: " + ErrorType.BAD_REQUEST + "\n message: " + npe.getMessage());
	    }catch(Exception e) {
	    	logger.severe("An Exception occurred executing the query to get the location site capacity. " + "\n error: " + ErrorType.INTERNAL_SERVER_ERROR + "\nmessage: " + e.getMessage());
	    	throw new Exception("An Exception occurred executing the query to get the location site capacity. " + "\n error: " + ErrorType.INTERNAL_SERVER_ERROR + "\nmessage: " + e.getMessage());
	    } finally {
	    	try {
	    		if(ps != null && !ps.isClosed()) ps.close();
	    	} catch(SQLException e) {
	    		logger.severe("Error trying to close the statement");
	    	}
	    }
		
		return Executed;
	}
	
	public String getemailByOpsLine(String opsline) throws Exception{
		ArrayList<String> groups = new ArrayList<String>();
		 String query = "", group = "";
		 
		 if (opsline != null && !opsline.isEmpty()) {
		      query = "Select \\\"EMAIL\\\", OPS_LINE FROM OPS_LINE_EMAIL_MASTER where OPS_LINE = ?";
		    } else {
		      query = " Select \"EMAIL\", OPS_LINE FROM OPS_LINE_EMAIL_MASTER";
		    }
		 
		 PreparedStatement ps = null;
		 
		 try {
			 if(con == null || con.isClosed()) {
				 con = DataSourceClient.getConnection();
				 logger.info("The connection was stablished successfully with status: " + String.valueOf(!con.isClosed()));
			 }
			 ps = con.prepareStatement(query);
			 if (opsline != null && !opsline.isEmpty()) {
				 ps.setString(1, opsline);
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
		 } catch(SQLException sqle) {
			 logger.severe("A SQLException" + " occurred executing the query to get the location site capacity. " + "\n error: " + ErrorType.BAD_REQUEST + "\n message: " + sqle.getMessage());
			 throw new Exception("A SQLException" + " occurred executing the query to get the location site capacity. " + "\n error: " + ErrorType.BAD_REQUEST + "\n message: " + sqle.getMessage());
		 } catch(NullPointerException npe) {
			 logger.severe("A NullPointerException occurred executiong the query to get the location site capacity. " + "\n error:" + ErrorType.BAD_REQUEST + "\n message: " + npe.getMessage());
			 throw new Exception("A NullPointerException occurred executiong the query to get the location site capacity. " + "\n error:" + ErrorType.BAD_REQUEST + "\n message: " + npe.getMessage());
		 } catch(Exception e) {
		    	logger.severe("An Exception occurred executing the query to get the location site capacity. " + "\n error: " + ErrorType.INTERNAL_SERVER_ERROR + "\nmessage: " + e.getMessage());
		    	throw new Exception("An Exception occurred executing the query to get the location site capacity. " + "\n error: " + ErrorType.INTERNAL_SERVER_ERROR + "\nmessage: " + e.getMessage());
		 } finally {
			 try {
				 if(ps != null && !ps.isClosed()) ps.close();
			 } catch(SQLException e) {
				 logger.severe("Error trying to close the statement");
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
		        logger.info("The connection was stablished successfully with status: " + String.valueOf(!con.isClosed()));
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
		        logger.severe("Error trying to close the statement");
		      }
		    }
	    
	    return email;
	}
	
	  public OpsLineEmail getOpsLineStaffName(String requisition) {
			String query = "";
			OpsLineEmail OpsLineEmail = new OpsLineEmail();

			query =
			  "SELECT rm.RELATION_CODE, rm.NAME,w.ops_line, r.INTERFACE_TRANSFERRED_DATE_ESD \r\n" +
			  "FROM WO w, REQUISITION_HEADER r, relation_master rm \r\n" +
			  "WHERE r.requisition = ? AND w.wo = r.wo AND r.created_by = rm.relation_code";

			PreparedStatement ps = null;

			try {
			  if (con == null || con.isClosed()) {
				con = DataSourceClient.getConnection();
				logger.info("The connection was stablished successfully with status: " + String.valueOf(!con.isClosed()));
			  }

			  ps = con.prepareStatement(query);

			  ps.setString(1, requisition);

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
				logger.severe("Error trying to close the statement");
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
