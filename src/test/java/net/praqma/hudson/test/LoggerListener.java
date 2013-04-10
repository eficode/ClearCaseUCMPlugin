package net.praqma.hudson.test;

import hudson.Extension;
import hudson.model.AbstractBuild;
import hudson.model.TaskListener;
import hudson.model.listeners.RunListener;
import net.praqma.logging.LoggingUtil;
import net.praqma.logging.PraqmaticLogHandler;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author cwolfgang
 */
@Extension
public class LoggerListener extends RunListener<AbstractBuild> {

    public LoggerListener() {
        super( AbstractBuild.class );
    }

    @Override
    public void onStarted( AbstractBuild run, TaskListener listener ) {
        EnableLoggerAction action = run.getAction( EnableLoggerAction.class );

        if( action != null ) {
            File output = new File( action.getOutputDir(), CCUCMRule.getSafeName( run.getProject().getDisplayName() ) + "." + run.getNumber() + ".log" );
            List<String> loggers = new ArrayList<String>(2);
            loggers.add( "net.praqma" );
            try {
                LoggingUtil.setPraqmaticHandler( Level.ALL, loggers, output );
            } catch( FileNotFoundException e ) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void onFinalized( AbstractBuild abstractBuild ) {
        EnableLoggerAction action = abstractBuild.getAction( EnableLoggerAction.class );

        if( action != null ) {
            int threadId = (int) Thread.currentThread().getId();
            Logger logger = Logger.getLogger( "net.praqma" );
            for( Handler handler : logger.getHandlers() ) {
                //System.out.println( "[[]] Checking " + handler );
                if( handler instanceof PraqmaticLogHandler ) {
                    PraqmaticLogHandler h = (PraqmaticLogHandler) handler;
                    //System.out.println( "[[PH]] THREAD " + h.getThreadId() );
                    if( h.getThreadId() == threadId ) {
                        //System.out.println( "[[PH]] REMOVING THREAD " + h.getThreadId() );
                        logger.removeHandler( handler );
                        handler.close();
                    }
                }
            }
        }
    }
}
