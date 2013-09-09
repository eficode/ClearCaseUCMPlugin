package net.praqma.hudson.test.integration.userstories;

import hudson.FilePath;
import hudson.model.AbstractBuild;
import hudson.model.FreeStyleProject;
import hudson.model.Project;
import hudson.model.Result;
import net.praqma.clearcase.Rebase;
import net.praqma.clearcase.test.annotations.ClearCaseUniqueVobName;
import net.praqma.clearcase.test.junit.ClearCaseRule;
import net.praqma.clearcase.ucm.entities.Activity;
import net.praqma.clearcase.ucm.entities.Component;
import net.praqma.clearcase.ucm.entities.Stream;
import net.praqma.clearcase.ucm.entities.Version;
import net.praqma.hudson.scm.ChangeLogEntryImpl;
import net.praqma.hudson.scm.ChangeLogSetImpl;
import net.praqma.hudson.test.BaseTestClass;
import net.praqma.hudson.test.CCUCMRule;
import net.praqma.hudson.test.SystemValidator;
import net.praqma.util.test.junit.DescriptionRule;
import net.praqma.util.test.junit.TestDescription;
import org.junit.Rule;
import org.junit.Test;

import java.util.List;
import java.util.logging.Logger;

/**
 * @author cwolfgang
 */
public abstract class JENKINS18278Base extends BaseTestClass {

    private static Logger logger = Logger.getLogger( JENKINS18278Base.class.getName() );

    @Rule
    public static DescriptionRule desc = new DescriptionRule();

    public void printChangeLog( AbstractBuild build ) {
        System.out.println( "LISTING THE CHANGESET FOR " + build );
        ChangeLogSetImpl cls = (ChangeLogSetImpl) build.getChangeSet();
        for( ChangeLogEntryImpl e : cls.getEntries() ) {
            System.out.println( "Author           : " + e.getAuthor() );
            System.out.println( "Activity headline: " + e.getActHeadline() );
            System.out.println( "Affected paths   : " + e.getAffectedPaths() );
            System.out.println( "Message          : " + e.getMsg() );
        }
        System.out.println( "END OF LISTING" );
    }

}
