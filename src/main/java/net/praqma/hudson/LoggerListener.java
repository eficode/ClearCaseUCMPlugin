package net.praqma.hudson;

import hudson.Extension;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.model.listeners.RunListener;
import net.praqma.logging.LoggingUtil;

import java.util.logging.Level;

/**
 * @author cwolfgang
 */
//@Extension
public class LoggerListener extends RunListener<Run> {

    public LoggerListener() {
        super( Run.class );
        System.out.println( "Initializing logger for test" );
    }

    @Override
    public void onStarted( Run run, TaskListener listener ) {
        LoggingUtil.setPraqmaticHandler( Level.ALL, "net.praqma" );
    }
}