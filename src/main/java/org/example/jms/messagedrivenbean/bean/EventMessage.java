package org.example.jms.messagedrivenbean.bean;

import java.io.Serializable;

public class EventMessage implements Serializable {
	
	private static final long serialVersionUID = 7539560235904656962L;
	
	private Type type;
	
	private String value;
	
	public String getValue() {
		return value;
	}
	
	public void setValue(String value) {
		this.value = value;
	}
	
	public Type getType() {
		return type;
	}
	
	public void setType(Type type) {
		this.type = type;
	}
	
}
