package net.praqma.hudson.test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import hudson.FilePath;
import net.praqma.clearcase.ucm.entities.Activity;
import net.praqma.hudson.Util;

import net.praqma.clearcase.exceptions.ClearCaseException;
import net.praqma.clearcase.ucm.entities.Baseline;
import net.praqma.clearcase.ucm.entities.Tag;
import net.praqma.clearcase.ucm.entities.Project.PromotionLevel;
import net.praqma.clearcase.ucm.entities.Stream;
import net.praqma.hudson.CCUCMBuildAction;
import hudson.model.Result;
import hudson.model.AbstractBuild;
import net.praqma.hudson.exception.ScmException;
import net.praqma.hudson.scm.ChangeLogSetImpl;

import static org.junit.Assert.*;

import static org.hamcrest.CoreMatchers.*;

public class SystemValidator {

    private static Logger logger = Logger.getLogger( SystemValidator.class.getName() );

	private AbstractBuild<?, ?> build;

    /* Validate path elements */
    private boolean checkPathElements = false;
    private Map<FilePath, List<Element>> pathsToCheck = new HashMap<FilePath, List<Element>>();
	
	public SystemValidator( AbstractBuild<?, ?> build ) {
		this.build = build;
	}
	
	public SystemValidator validate() throws ClearCaseException {
		
		System.out.println( "[Validating " + build + "]" );
		
		/* Jenkins build */
		if( checkBuild ) {
			System.out.println( "[Validating build]" );
			checkBuild();
		}

		/* Built baseline not found */
		if( checkBuiltBaselineNotFound ) {
			System.out.println( "[Validating built baseline not found]" );
			checkBuiltBaselineNotFound();
		}
		
		/* Built baseline */
		if( checkBuiltBaseline ) {
			System.out.println( "[Validating built baseline]" );
			checkBuiltBaseline();
		}

        /* Build view */
        if( validateView ) {
            System.out.println( "[Validating build view]" );
            try {
                doValidateBuildView();
            } catch (ScmException ex) {
                throw new ClearCaseException("Failed to create and validate viewtag", ex);
            }
        }
		
		/* Check tagged baseline */
		if( checkTagOnBuiltBaseline ) {
			System.out.println( "[Validating tag on built baseline]" );
			checkBaselineTag();
		}
		
		/* Created baseline */
		if( checkCreatedBaseline ) {
			System.out.println( "[Validating created baseline]" );
			checkCreatedBaseline();
		}

        /* Check the path elements */
        if( checkPathElements ) {
            logger.info( "[Validating path elements]" );
            try {
                doCheckPaths();
            } catch( Exception e ) {
                fail( e.getMessage() );
            }
        }

        if( checkChangeset ) {
            logger.info( "[Validating changeset]" );
            doCheckChangeset();
        }

        if( activities.size() > 0 ) {
            System.out.println( "[Validating changeset]" );
            doCheckActivities();
        }
        
        if(checkBuiltBaselineInFoundation) {
            _validateBuiltBaselineIsInForundation(shouldItBePartOfFoundation);
        }
		
		/**/
		
		return this;
	}

    /* Validate build view */
    private boolean validateView = false;

    public SystemValidator validateBuildView() {
        validateView = true;

        return this;
    }

    private void doValidateBuildView() throws ScmException {
        CCUCMBuildAction action = getBuildAction();
        Stream stream = action.getStream();
        String viewTag = Util.createViewTag( build.getParent().getDisplayName(), stream );
        System.out.println( "[assert] The view tag must be " + viewTag );
        assertThat( action.getViewTag(), is( viewTag ) );
    }
	
	/* Validate build */
	private boolean checkBuild = false;
	private Result buildResult;
	
	public SystemValidator validateBuild( Result buildResult ) {
		this.buildResult = buildResult;
		this.checkBuild = true;
		
		return this;
	}
	
	private void checkBuild() {
		System.out.println( "[assert] " + "Jenkins build must be " + buildResult );
		assertThat( build.getResult(), is( buildResult ) );
	}
	
	/* Validate no built baseline */
	private boolean checkBuiltBaselineNotFound = false;
	
	public SystemValidator validateBuiltBaselineNotFound() {
		 this.checkBuiltBaselineNotFound = true;
		 
		 return this;
	}
	
	public void checkBuiltBaselineNotFound() {
		Baseline baseline = getBuiltBaseline();
		
		System.out.println( "[assert] " + "Built baseline must be null" );
		assertNull( baseline );
	}
	
	/* Validate build baseline */
	private boolean checkBuiltBaseline = false;
    private boolean shouldItBePartOfFoundation = false;
    private boolean checkBuiltBaselineInFoundation = false;
	private PromotionLevel builtBaselineLevel;
	private Baseline expectedBuiltBaseline;
	private Boolean builtBaselineIsRecommended;
	
	public SystemValidator validateBuiltBaseline( PromotionLevel level, Baseline expected ) {
		return validateBuiltBaseline( level, expected, null );
	}
	
	public SystemValidator validateBuiltBaseline( PromotionLevel level, Baseline expected, Boolean isRecommended ) {
		this.checkBuiltBaseline = true;
		this.expectedBuiltBaseline = expected;
		this.builtBaselineLevel = level;
		this.builtBaselineIsRecommended = isRecommended;		
		return this;
	}
    
    private void _validateBuiltBaselineIsInForundation(boolean shouldItBeThere) throws ClearCaseException {
        Baseline bl = getBuiltBaseline();
        assertNotNull(bl);
        bl.load();
        /* Check that the built baseline is in the foundation of the stream */
        if(shouldItBeThere) {
            System.out.println( "[assert] " + bl.getNormalizedName() + " should be part of the stream's foundation");
            assertTrue(getStream().getFoundationBaselines().contains(bl));
        } else {
            System.out.println( "[assert] " + bl.getNormalizedName() + " should NOT be part of the stream's foundation");
            assertTrue(!getStream().getFoundationBaselines().contains(bl));            
        }
    }
    
    public SystemValidator validateBuiltBaselineIsInFoundation(boolean shouldItBeThere) {
        this.checkBuiltBaselineInFoundation = true;
        this.shouldItBePartOfFoundation = shouldItBeThere;
        return this;
    }
    
    
	
	private void checkBuiltBaseline() throws ClearCaseException {
		Baseline baseline = getBuiltBaseline();
		assertNotNull( baseline );
		baseline.load();
		
		/* Check level */
		System.out.println( "[assert] " + baseline.getNormalizedName() + " must have the promotion level " + builtBaselineLevel );
		assertEquals( builtBaselineLevel, baseline.getPromotionLevel( true ) );
		
		/* Check expected */
		System.out.println( "[assert] " + baseline.getNormalizedName() + " must be the same as " + expectedBuiltBaseline.getNormalizedName() );
		assertThat( baseline, is( expectedBuiltBaseline ) );
		
		/* Check recommendation */
		if( builtBaselineIsRecommended != null ) {
			System.out.println( "[assert] " + baseline.getNormalizedName() + " must " + (builtBaselineIsRecommended?"":"not ") + "be recommended" );
			Stream stream = getStream().load();
			List<Baseline> rbls = stream.getRecommendedBaselines();
			assertEquals( 1, rbls.size() );
			if( builtBaselineIsRecommended ) {
				assertThat( baseline, is( rbls.get( 0 ) ) );
			} else {
				assertThat( baseline, not( rbls.get( 0 ) ) );
			}
		}
	}
	
	private boolean checkTagOnBuiltBaseline = false;
	private Baseline taggedBaseline;
	private boolean baselineMustBeTagged = false;
	
	public SystemValidator validateBaselineTag( Baseline baseline, boolean mustBeTagged ) {
		checkTagOnBuiltBaseline = true;
		this.taggedBaseline = baseline;
		this.baselineMustBeTagged = mustBeTagged;
		
		return this;
	}
	
	public void checkBaselineTag() {

		Tag tag;
		try {
			System.out.println( "[assert] " + taggedBaseline.getNormalizedName() + " must " + (baselineMustBeTagged?" ":"not ") + "be tagged" );
			tag = Tag.getTag( taggedBaseline, build.getParent().getDisplayName(), build.getNumber()+"", false );
			if( baselineMustBeTagged ) {
				assertNotNull( tag );
				
				/* TODO validate cgi string */
			} else {
				assertNull( tag );
			}
		} catch( Exception e ) {
			fail( "Checking tag failed: " + e.getMessage() );
		}
	}

	
	
	
	/* Validate created baseline */
	private boolean checkCreatedBaseline = false;
	private Boolean createdBaselineExists;
    private Boolean createdBaselineIsRecommended;
	
    public SystemValidator validateCreatedBaseline( boolean exists) {
        return validateCreatedBaseline(exists, null);
    }
    
	public SystemValidator validateCreatedBaseline( boolean exists, Boolean isRecommended ) {
		this.checkCreatedBaseline = true;
		this.createdBaselineExists = exists;
        this.createdBaselineIsRecommended = isRecommended;
		return this;
	}
	
	private void checkCreatedBaseline() throws ClearCaseException {
		Baseline baseline = getCreatedBaseline();
		
		System.out.println( "Validating created baseline" );
		
		/* Validate null check */
		if( createdBaselineExists != null ) {
            
			System.out.println( "[assert] Created baseline must " + (createdBaselineExists?"not ": "") + "be null" );
			if( createdBaselineExists ) {
				assertNotNull( baseline );
			} else {
				assertNull( baseline );
			}
            
            System.out.println( String.format( "[assert] Created baseline %s must be recommended", baseline ) );
            if(createdBaselineIsRecommended != null) {
                if( createdBaselineIsRecommended ) {
                    List<Baseline> bls = getStream().load().getRecommendedBaselines();                    
                    assertThat( baseline, is(bls.get(0)) );
                } else {
                    List<Baseline> bls = getStream().load().getRecommendedBaselines();
                    assertThat( bls.size(), is(1));
                    assertThat( baseline, not(bls.get(0)));
                }
            }
		}
		
	}


    /* Path checks */

    public static class Element {
        private boolean mustExist;
        private String element;

        public Element( String element, boolean mustExist ) {
            this.element = element;
            this.mustExist = mustExist;
        }

        @Override
        public String toString() {
            return element;
        }
    }

    public SystemValidator addElementToPathCheck( FilePath path, Element element ) {
        this.checkPathElements = true;

        if( pathsToCheck.containsKey( path ) ) {
            pathsToCheck.get( path ).add( element );
        } else {
            List<Element> e = new ArrayList<Element>();
            e.add( element );
            pathsToCheck.put( path, e );
        }

        return this;
    }

    private void doCheckPaths() throws IOException, InterruptedException {
        for( FilePath path : pathsToCheck.keySet() ) {
            List<Element> elements = pathsToCheck.get( path );
            logger.fine( "Checking " + path );

            for( Element element : elements ) {
                if( element.mustExist ) {
                    logger.fine( "Path must have " + element );
                    assertTrue( "The path " + path + " does not have " + element, new FilePath( path, element.element ).exists() );
                } else {
                    logger.fine( "Path must NOT have " + element );
                    assertFalse( "The path " + path + " does have " + element, new FilePath( path, element.element ).exists() );
                }
            }
        }
    }

    private List<Activity> activities = new ArrayList<Activity>(  );

    public SystemValidator addActivityToCheck( Activity activity ) {
        activities.add( activity );
        return this;
    }

    public SystemValidator addActivitiesToCheck( List<Activity> activities ) {
        this.activities = activities;
        return this;
    }

    private void doCheckActivities() {
        CCUCMBuildAction action = getBuildAction();

        logger.fine( "The number of activities is " + action.getActivities().size() + ", must be " + activities.size() );
        assertThat( action.getActivities().size(), is( activities.size() ) );

        for( Activity a : activities ) {
            logger.fine( "The changeset must have " + a.getNormalizedName() );
            assertTrue( action.getActivities().contains( a ) );
        }
    }

    private boolean checkChangeset = false;
    private int numberOfChanges = 0;

    public SystemValidator checkChangeset( int numberOfChanges ) {
        this.checkChangeset = true;
        this.numberOfChanges = numberOfChanges;

        return this;
    }

    private void doCheckChangeset() {
        System.out.println( "[assert] The number of changesets must be " + numberOfChanges );

        try {
            System.out.println( "CHANGE LOG: " + build.getChangeSet() );
            System.out.println( "CHANGE LOG: " + build.getChangeSet().getClass() );
            ChangeLogSetImpl cls = (ChangeLogSetImpl) build.getChangeSet();

            assertThat( cls.getEntries().size(), is( numberOfChanges ) );
        } catch( ClassCastException e ) {
            if( numberOfChanges > 0 ) {
                fail( "The number of changes is zero!" );
            }
        }
    }
	
	/* Helpers */
	private CCUCMBuildAction action;
	
	private CCUCMBuildAction getBuildAction() {
		if( action == null ) {
			action = build.getAction( CCUCMBuildAction.class );
			assertNotNull( action );
		}
		
		return action;
	}
	
	private Baseline getBuiltBaseline() {
		return getBuildAction().getBaseline();
	}
	
	private Stream getStream() {
		return getBuildAction().getStream();
	}
	
	private Baseline getCreatedBaseline() {
		return getBuildAction().getCreatedBaseline();
	}
	
	
}
