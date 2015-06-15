package net.praqma.hudson.test;

import net.praqma.util.debug.Logger;
import net.praqma.util.debug.Logger.LogLevel;
import net.praqma.util.debug.appenders.Appender;
import net.praqma.util.debug.appenders.ConsoleAppender;

import org.junit.rules.ExternalResource;

public class LoggerRule extends ExternalResource {

	private Appender appender;
	
	@Override
	protected void before() {
		System.out.println( "[LoggerRule] Setting up logger" );
		appender = new ConsoleAppender();
		Logger.addAppender( appender );
		Logger.setMinLogLevel( LogLevel.DEBUG );
	}
	
	@Override
	protected void after() {
		System.out.println( "[LoggerRule] Removing " + appender );
		Logger.removeAppender( appender );
	}
}
