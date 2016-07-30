package com.qzhu;

import java.io.Serializable;

public class Message implements Serializable{
	
	private static final long serialVersionUID = -7220884435874720185L;
	private final int fromPort;
	public int getFromPort(){
		return this.fromPort;
	}
	private final String fromIp;
	public String getFromIp(){
		return this.fromIp;
	}
	private final String content;
	public String getContent(){
		return this.content;
	}
	public Message(String fromIp,int fromPort,String content){
		this.fromIp=fromIp;
		this.fromPort=fromPort;
		this.content=content;
	}
	
	public String toString() {
		return "["+this.content+"] from "+fromIp+":"+fromPort;
	}
}
