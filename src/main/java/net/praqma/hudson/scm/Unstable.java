package net.praqma.hudson.scm;

import java.io.Serializable;

public class Unstable implements Serializable {

	private static final long serialVersionUID = -133143812099625188L;

	public enum Type {
		successful,
		failed
	}
	
	private Type type;
	
	public Unstable( Type type ) {
		this.type = type;
	}
	
	public Unstable( String type ) {
		if( type.equalsIgnoreCase( "successful" ) ) {
			this.type = Type.successful;
		} else {
			this.type = Type.failed;
		}
	}
	
	public boolean treatFailed() {
		return type.equals( Type.failed );
	}
	
	public boolean treatSuccessful() {
		return type.equals( Type.successful );
	}
	
	public Type getType() {
		return type;
	}
	
    @Override
	public String toString() {
		return type.toString();
	}
}
