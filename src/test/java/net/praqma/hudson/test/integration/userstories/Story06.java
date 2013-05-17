package net.praqma.hudson.test.integration.userstories;

import net.praqma.clearcase.test.annotations.ClearCaseUniqueVobName;
import org.junit.Test;

import net.praqma.clearcase.ucm.entities.Stream;
import net.praqma.util.test.junit.TestDescription;

import java.util.logging.Logger;

public class Story06 extends Story06Base {

	private static Logger logger = Logger.getLogger( Story06.class.getName() );

	@Test
    @ClearCaseUniqueVobName( name = "dip1" )
	@TestDescription( title = "Story 6", text = "New baseline on dev stream. Deliver in progress from another stream, different view", configurations = { "Force deliver = true", "Poll childs" }	)
	public void story06_1() throws Exception {
        Stream dev1 = ccenv.context.streams.get( "one_dev" );
        Stream dev2 = ccenv.context.streams.get( "two_dev" );
        run( dev1, dev2, ccenv.getUniqueName() + "_one_dev", ccenv.getUniqueName() + "_two_dev", null, false );
    }

    @Test
    @ClearCaseUniqueVobName( name = "dip2" )
    @TestDescription( title = "Story 6", text = "New baseline on dev stream. Deliver in progress from same stream, different view", configurations = { "Force deliver = true", "Poll childs" }	)
    public void story06_2() throws Exception {
        Stream dev1 = ccenv.context.streams.get( "one_dev" );
        run( dev1, dev1, ccenv.getUniqueName() + "_one_dev", ccenv.getUniqueName() + "_one_dev", null, false );
    }

    @Test
    @ClearCaseUniqueVobName( name = "dip3" )
    @TestDescription( title = "Story 6", text = "New baseline on dev stream. Deliver in progress from previous build, different view", configurations = { "Force deliver = true", "Poll childs" }	)
    public void story06_3() throws Exception {
        Stream dev1 = ccenv.context.streams.get( "one_dev" );
        run( null, dev1, null, ccenv.getUniqueName() + "_one_dev", null, true );
    }

}
