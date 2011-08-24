package net.praqma.hudson.scm;

public class Polling {
	
	enum PollingType {
		self,
		childs,
		siblings
	}
	
	private PollingType type;
	
	public Polling( String polling ) {
		if( polling.equals("child") ) {
			this.type = PollingType.childs;
		} else if( polling.equals("self") ) {
			this.type = PollingType.self;			
		} else {
			this.type = PollingType.siblings;
		}
	}
	
	public boolean isPollingChilds() {
		if( this.type.equals(PollingType.childs) ) {
			return true;
		} else {
			return false;
		}
	}
	
	public boolean isPollingSiblings() {
		if( this.type.equals(PollingType.siblings) ) {
			return true;
		} else {
			return false;
		}
	}
	
	public boolean isPollingSelf() {
		if( this.type.equals(PollingType.self) ) {
			return true;
		} else {
			return false;
		}
	}
	
	public String toString() {
		return type.toString();
	}
}
