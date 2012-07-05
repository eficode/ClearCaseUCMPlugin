package net.praqma.hudson.exception;

public class UnableToInitializeWorkspaceException extends Exception {

	public UnableToInitializeWorkspaceException( String msg, Exception e ) {
		super( msg, e );
	}
}
