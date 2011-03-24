package net.praqma.hudson;

import java.io.File;
import java.io.IOException;

import net.praqma.util.io.BuildNumberStamper;
import net.praqma.util.io.Pom;

public class GetVersion
{
	public static void main( String[] args ) throws IOException
	{
		System.out.println( "Trying to stamp version number into Version.java" );
		Pom pom = new Pom( new File( "pom.xml" ) );	
		
		BuildNumberStamper stamp = new BuildNumberStamper( new File( "src/main/java/net/praqma/hudson/Version.java" ) );
		
		String version = pom.getVersion();
		String[] vs = version.split( "\\." );
		
		switch( vs.length )
		{
		case 1:
			stamp.stampIntoCode( "0", "0", vs[0], "" );
			break;
			
		case 2:
			stamp.stampIntoCode( "0", vs[0], vs[1], "" );
			break;
			
		case 3:
			stamp.stampIntoCode( vs[0], vs[1], vs[2], "" );
			break;
			
		default:
			System.err.println( "Unknown format: " + vs.length );
			System.exit( 1 );
		}
		
		System.out.println( "Stamped " + version + " into Version.java" );
	}
}
