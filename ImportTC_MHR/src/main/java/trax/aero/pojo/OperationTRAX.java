package trax.aero.pojo;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;

@XmlAccessorType(XmlAccessType.FIELD)
public class OperationTRAX {
	
	@XmlElement(name = "OPS_NO")
    private String opsNo;

    @XmlElement(name = "TASK_CARD")
    private String taskCard;

    @XmlElement(name = "EXCEPTION_ID")
    private String exceptionId;

    @XmlElement(name = "EXCEPTION_DETAIL")
    private String exceptionDetail;

	public String getOpsNo() {
		return opsNo;
	}

	public void setOpsNo(String opsNo) {
		this.opsNo = opsNo;
	}

	public String getTaskCard() {
		return taskCard;
	}

	public void setTaskCard(String taskCard) {
		this.taskCard = taskCard;
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

}
