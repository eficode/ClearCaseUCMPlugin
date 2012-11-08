package net.praqma.hudson.exception;

public class CCUCMException extends Exception {
    public static final long serialVersionUID = 1L;

    public CCUCMException( String msg, Exception e ) {
        super( msg, e );
    }

    public CCUCMException( String m ) {
        super( m );
    }

    public CCUCMException( Exception e ) {
        super( e );
    }
}
