package net.praqma.hudson.test.integration.userstories;

import hudson.model.AbstractBuild;
import net.praqma.hudson.scm.ChangeLogEntryImpl;
import net.praqma.hudson.scm.ChangeLogSetImpl;
import net.praqma.hudson.test.BaseTestClass;
import net.praqma.util.test.junit.DescriptionRule;
import org.junit.Rule;

/**
 * @author cwolfgang
 */
public abstract class JENKINS18278Base extends BaseTestClass {

    @Rule
    public DescriptionRule desc = new DescriptionRule();

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
