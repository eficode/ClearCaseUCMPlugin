package net.praqma.hudson.exception;

public class ScmException extends Exception {

	public ScmException( String msg, Exception e ) {
		super( msg, e );
	}
}
