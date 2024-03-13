package trax.aero.pojo;

import java.util.ArrayList;

import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name = "DT_TRAX_SND_I6_4100_REQ", namespace = "http://singaporeair.com/mro/ESDTRAX")
@XmlAccessorType(XmlAccessType.FIELD)
public class INT6_SND {

	 @XmlElement(name = "Order")
	    private ArrayList<OrderSND> order;
	 
	    public ArrayList<OrderSND> getOrder() {
	        return order;
	    }

	    public void setOrder(ArrayList<OrderSND> order) {
	        this.order = order;
	    }
}
