package trax.aero.pojo;

import java.util.ArrayList;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name = "MT_TRAX_SND_I5_4112", namespace = "http://singaporeair.com/mro/ESDTRAX")
@XmlAccessorType(XmlAccessType.FIELD)
public class INT5_SND {
    
	@XmlElement(name = "Order")
	private ArrayList<OrderSND> order;

	public ArrayList<OrderSND> getOrder() {
	return order;
}

	public void setOrder(ArrayList<OrderSND> order) {
	this.order = order;
}
	
    
    
}