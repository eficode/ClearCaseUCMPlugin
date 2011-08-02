package net.praqma.hudson.scm;

public class Polling {
	
	enum PollingType {
		childs,
		siblings
	}
	
	private PollingType type;
	
	public Polling( String polling ) {
		if( polling.equals("child") ) {
			this.type = PollingType.childs;
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
	
	public String toString() {
		return type.toString();
	}
}
