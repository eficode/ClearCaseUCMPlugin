package net.praqma.hudson.scm;

import java.io.Serializable;

public class Polling implements Serializable {
	
	public enum PollingType {
		none,
		self,
		childs,
		siblings,
        siblingshlink
	}
	
	private final PollingType type;
	
	public Polling() {
		type = PollingType.none;
	}
	
	public Polling( PollingType type ) {
		this.type = type;
	}
	
	public Polling( String polling ) {
		if( polling.equals("child") ) {
			this.type = PollingType.childs;
		} else if( polling.equals("self") ) {
			this.type = PollingType.self;			
        } else if( polling.equals("siblinghlink")) {
            this.type = PollingType.siblingshlink;
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
    
    public PollingType getType() {
        return this.type;
    }
	
	public boolean isPollingSiblings() {
		if( this.type.equals(PollingType.siblings) || this.type.equals(PollingType.siblingshlink) ) {
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

    /**
     * Returns true if {@link Polling} other {@link net.praqma.clearcase.ucm.entities.Stream}'s
     */
	public boolean isPollingOther() {
		if( this.type.equals(PollingType.childs) || isPollingSiblings()  ) {
			return true;
		} else {
			return false;
		}
	}
	
	/**
	 * This is actually only important if the polling type is not set. This is typically only if the default constructor is used.
	 * @return whether or not the type is polling
	 */
	public boolean isPolling() {
		if( !this.type.equals( PollingType.none ) ) {
			return true;
		} else {
			return false;
		}
	}
	
    @Override
	public String toString() {
		return type.toString();
	}
}
