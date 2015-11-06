package net.praqma.hudson.scm;

import java.io.Serializable;

public class Polling implements Serializable {
	
	public enum PollingType {
		none,
		self,
		childs,
		siblings,
        siblingshlink,
        subscribe,
        rebase
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
        } else if (polling.equals("rebase")) {
            this.type = PollingType.rebase;
        } else if (polling.equals("subscribe")) {
            this.type = PollingType.subscribe;
		} else {
			this.type = PollingType.siblings;
		}
	}
	
	public boolean isPollingChilds() {
		return this.type.equals(PollingType.childs);
	}
    
    public boolean isPollingSubscribe() {
        return this.type.equals(PollingType.subscribe);
    }
    
    public PollingType getType() {
        return this.type;
    }
	
	public boolean isPollingSiblings() {
        return this.type.equals(PollingType.siblings) || this.type.equals(PollingType.siblingshlink);			
	}
	
	public boolean isPollingSelf() {
		return this.type.equals(PollingType.self);			
	}

    /**
     * @return true if {@link Polling} other {@link net.praqma.clearcase.ucm.entities.Stream}
     */
	public boolean isPollingOther() {
		return this.type.equals(PollingType.childs) || isPollingSiblings();			
	}
    
    public boolean isPollingRebase() {
        return this.type.equals(PollingType.rebase);
    }
	
	/**
	 * This is actually only important if the polling type is not set. This is typically only if the default constructor is used.
	 * @return whether or not the type is polling
	 */
	public boolean isPolling() {
		return !this.type.equals( PollingType.none );			
	}
	
    @Override
	public String toString() {
		return type.toString();
	}
}
