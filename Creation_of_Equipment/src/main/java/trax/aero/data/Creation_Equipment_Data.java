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
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.NoResultException;
import javax.persistence.Persistence;
import javax.xml.bind.JAXBException;

import trax.aero.controller.Creation_Equipment_Controller;
import trax.aero.exception.CustomizeHandledException;
import trax.aero.logger.LogManager;
import trax.aero.model.InterfaceLockMaster;
import trax.aero.pojo.INT5_SND;
import trax.aero.pojo.INT5_TRAX;
import trax.aero.pojo.OpsLineEmail;
import trax.aero.pojo.OrderSND;
import trax.aero.utils.DataSourceClient;
import trax.aero.utils.ErrorType;

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
		
		String sqlUpdateWO = "UPDATE WO SET MOD_NO = ?, INTERFACE_SAP_TRANSFERRED_DATE = SYSDATE, INTERFACE_SAP_TRANSFERRED_FLAG = 'Y' WHERE INTERFACE_SAP_TRANSFERRED_FLAG IS NULL and WO = ?";
		
		try {
			PreparedStatement pstmt1 = con.prepareStatement(sqlUpdateWO);
			
			if (request != null) {
				
				if(request.getModNO() != null && !request.getModNO().isEmpty()) {
				pstmt1.setString(2, request.getWO());
				pstmt1.setString(1, request.getModNO());
				pstmt1.executeUpdate();
				}
				
				if (request.getExceptionId() != null && !request.getExceptionId().equalsIgnoreCase("53")) {
					executed = "WO: " + request.getWO() + ", WBS: " + request.getModNO() + ", Error Code: " + request.getExceptionId() + ", Remarks: " + request.getExceptionDetail();
				}
			}
			
			
		}catch (SQLException e) {
	        executed = e.toString();
	        Creation_Equipment_Controller.addError(executed);
	        logger.severe(executed);
	    } 
		
		return executed;
	}
	
	public ArrayList<INT5_SND> getWorkOrder() throws Exception{
		executed = "OK";
		
		if (this.con == null || this.con.isClosed()) {
	        try {
	            this.con = DataSourceClient.getConnection(); 
	            if (this.con == null || this.con.isClosed()) {
	                throw new IllegalStateException("Issues connecting to the database");
	            }
	            logger.info("Established connection to the database.");
	        } catch (SQLException e) {
	            throw new IllegalStateException("Error trying to re-connect to the database.", e);
	        }
	    }
		
		ArrayList<INT5_SND> list = new ArrayList<INT5_SND>();
		ArrayList<OrderSND> orlist = new ArrayList<OrderSND>();
		
		String sqlWorkOrder = "SELECT W.WO, W.LOCATION, W.WO_DESCRIPTION, WS.PN, WS.PN_SN, W.SCHEDULE_START_DATE, \r\n" +
				"W.SCHEDULE_COMPLETION_DATE, P.ENGINE, W.CREATED_BY, W.THIRD_PARTY_WO, S.PARTY, CASE \r\n" +
				"WHEN S.PARTY = '1P' AND (W.THIRD_PARTY_WO = 'N' OR W.THIRD_PARTY_WO IS NULL) THEN 'N' \r\n" +
				"WHEN S.PARTY = '3P' AND W.THIRD_PARTY_WO = 'Y' THEN 'Y' WHEN S.PARTY = '1P' THEN 'N' \r\n" +
				"WHEN S.PARTY = '3P' THEN 'Y' ELSE NULL END AS THIRD_PARTY_FLAG FROM WO W \r\n" +
				"JOIN SYSTEM_TRAN_CODE S ON W.SOURCE_TYPE = S.SYSTEM_CODE \r\n" +
				"JOIN WO_SHOP_DETAIL WS ON W.WO = WS.WO \r\n" +
				"JOIN PN_MASTER P ON WS.PN = P.PN WHERE W.SOURCE_TYPE IS NOT NULL \r\n" +
				"AND S.SYSTEM_TRANSACTION = 'SOURCETYPE' AND W.INTERFACE_SAP_TRANSFERRED_FLAG IS NULL AND W.INTERFACE_SAP_TRANSFERRED_DATE IS NULL AND W.STATUS = 'CONF SLOT'";
		
		String sqlMark = "UPDATE WO SET INTERFACE_SAP_TRANSFERRED_DATE = SYSDATE WHERE WO = ?";
		
		if (MaxRecord != null && !MaxRecord.isEmpty()) {
			sqlWorkOrder = "SELECT * FROM (" + sqlWorkOrder;
		}
		if (MaxRecord != null && !MaxRecord.isEmpty()) {
			sqlWorkOrder = sqlWorkOrder + " ) WHERE ROWNUM <= ?";
		}
		
		 PreparedStatement pstmt1 = null;
		 ResultSet rs1 = null;
		 PreparedStatement pstmt2 = null;
		 ResultSet rs2 = null;
		 
		 try {
			pstmt1 = con.prepareStatement(sqlWorkOrder);
			pstmt2 = con.prepareStatement(sqlMark);
			
			if (MaxRecord != null && !MaxRecord.isEmpty()) {
		        pstmt1.setString(1, MaxRecord);
		      }
			
			rs1 = pstmt1.executeQuery();
			
			if(rs1 != null) {
				while(rs1.next()) {
					logger.info("Processiong WO: " + rs1.getString(1) + ", WO Description: " + rs1.getString(3) + ", Location: " + rs1.getString(2));
					INT5_SND req = new INT5_SND();
					orlist = new ArrayList<OrderSND>();
		    		 req.setOrder(orlist);
		    		 OrderSND Inbound = new OrderSND();
		    		 
					if (rs1.getString(1) != null && !rs1.getNString(1).isEmpty()) {
						Inbound.setTraxWo(rs1.getString(1));
						Inbound.setTcDescription(rs1.getString(3));
						Inbound.setLocationWO(rs1.getString(2));
					} 
					
					logger.info("WO Scheduled Start Date: " + rs1.getString(6) + ", WO Scheduled Completion Date: " + rs1.getString(7) + ", Employee: " + rs1.getString(9));
					
					Inbound.setStartDate(rs1.getString(6));
					Inbound.setEndDate(rs1.getString(7));
					Inbound.setCustomerID(rs1.getString(9));
					
					if (rs1.getString(8) != null && !rs1.getNString(8).isEmpty()) {
						Inbound.setTechControl(rs1.getString(8));
					}
					
					Inbound.setPflag(rs1.getString(12));
					
					logger.info("PN: " + rs1.getString(4) + ", SN: " + rs1.getString(5));
					
					if (rs1.getString(4) != null && !rs1.getNString(4).isEmpty()) {
						Inbound.setPn(rs1.getString(4));
						Inbound.setPnSn(rs1.getString(5));
					}
					
					req.getOrder().add(Inbound);
					list.add(req);
					
					pstmt2.setString(1, Inbound.getTraxWo());
					pstmt2.executeQuery();
				}
			}
			if (rs1 != null && !rs1.isClosed()) rs1.close();
			
		 } catch (Exception e) {
		      e.printStackTrace();
		      executed = e.toString();
		      Creation_Equipment_Controller.addError(e.toString());

		      logger.severe(executed);
		      throw new Exception("Issue found");
		}finally {
		      if (rs1 != null && !rs1.isClosed()) rs1.close();
		      if (pstmt1 != null && !pstmt1.isClosed()) pstmt1.close();
		}
		
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
