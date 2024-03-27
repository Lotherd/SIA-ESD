package trax.aero.pojo;



import java.util.ArrayList;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name = "MT_TRAX_RCV_I13_4109_RES", namespace = "http://singaporeair.com/mro/ESDTRAX")
@XmlAccessorType(XmlAccessType.FIELD)
public class INT13_TRAX {
	
	@XmlElement(name = "PR_NUMBER")
	private String PRnumber;
	
	@XmlElement(name = "PR_ITEM")
	private String PRitem;
	
	@XmlElement(name = "RFO_NO")
	private String RFO;
	
	@XmlElement(name = "OPS_NO")
	private String OPS;
	
	@XmlElement(name = "EXCEPTION_ID")
	private String exceptionId;
	
	@XmlElement(name = "EXCEPTION_DETAIL")
	private String exceptionDetail;
	
	@XmlElement(name = "PN")
	private String pn;
	
	@XmlElement(name = "REQUISITION")
	private String requisition;
	
	@XmlElement(name = "REQUISITION_LINE")
	private String requisitionLine;

	public String getPRnumber() {
		return PRnumber;
	}

	public void setPRnumber(String pRnumber) {
		PRnumber = pRnumber;
	}

	public String getPRitem() {
		return PRitem;
	}

	public void setPRitem(String pRitem) {
		PRitem = pRitem;
	}

	public String getRFO() {
		return RFO;
	}

	public void setRFO(String rFO) {
		RFO = rFO;
	}

	public String getOPS() {
		return OPS;
	}

	public void setOPS(String oPS) {
		OPS = oPS;
	}

	public String getExceptionId() {
		return exceptionId;
	}

	public void setExceptionId(String exceptionId) {
		this.exceptionId = exceptionId;
	}

	public String getExceptionDetail() {
		return exceptionDetail;
	}

	public void setExceptionDetail(String exceptionDetail) {
		this.exceptionDetail = exceptionDetail;
	}

	public String getPn() {
		return pn;
	}

	public void setPn(String pn) {
		this.pn = pn;
	}

	public String getRequisition() {
		return requisition;
	}

	public void setRequisition(String requisition) {
		this.requisition = requisition;
	}

	public String getRequisitionLine() {
		return requisitionLine;
	}

	public void setRequisitionLine(String requisitionLine) {
		this.requisitionLine = requisitionLine;
	}

}
