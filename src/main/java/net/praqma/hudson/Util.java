package net.praqma.hudson;

import hudson.FilePath;
import hudson.model.BuildListener;

import java.io.*;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import net.praqma.clearcase.Cool;
import net.praqma.clearcase.PVob;
import net.praqma.clearcase.exceptions.ClearCaseException;
import net.praqma.clearcase.exceptions.ViewException;
import net.praqma.clearcase.ucm.entities.Activity;
import net.praqma.clearcase.ucm.entities.Baseline;
import net.praqma.clearcase.ucm.entities.Project;
import net.praqma.clearcase.ucm.entities.Stream;
import net.praqma.clearcase.ucm.entities.Version;
import net.praqma.clearcase.ucm.utils.ReadOnlyVersionFilter;
import net.praqma.clearcase.ucm.utils.VersionList;
import net.praqma.clearcase.ucm.view.SnapshotView;
import net.praqma.clearcase.ucm.view.UCMView;
import net.praqma.clearcase.ucm.view.SnapshotView.Components;
import net.praqma.clearcase.ucm.view.SnapshotView.LoadRules2;
import net.praqma.hudson.exception.ScmException;
import org.apache.commons.lang.SystemUtils;

public abstract class Util {

	private static final Logger logger = Logger.getLogger( Util.class.getName() );

	public static Project.PromotionLevel getLevel( String level ) {
		if( level.equalsIgnoreCase( "any" ) ) {
			return null;
		} else {
			return Project.getPlevelFromString( level );
		}
	}

	public Stream getDeveloperStream( String streamname, PVob pvob, Stream buildIntegrationStream, Baseline foundationBaseline ) throws ScmException {
		Stream devstream = null;

		try {
			if( Stream.streamExists( streamname + pvob ) ) {
				devstream = Stream.get( streamname, pvob );
			} else {
				devstream = Stream.create( buildIntegrationStream, streamname + pvob, true, foundationBaseline );
			}
			
			devstream.load();
		} catch( Exception e ) {
			throw new ScmException( "Could not get stream: " + streamname, e );
		}

		return devstream;
	}

    public static String createChangelog( List<Activity> activities, Baseline bl, boolean trimmed, boolean discard, File viewRoot, List<String> readonly ) {
        logger.fine( "Generating change set, " + trimmed );
        ChangeSetGenerator csg = new ChangeSetGenerator().createHeader( bl.getShortname() );

        if( trimmed ) {
            VersionList vl = new VersionList().addActivities( activities ).setBranchName( "^.*" + Cool.qfs + bl.getStream().getShortname() + ".*$" );
            if(discard) {
                logger.fine("Discard enabled...enabling read-only filter");
                vl = vl.addFilter(new ReadOnlyVersionFilter(viewRoot, readonly)).apply();
            }
            
            Map<Activity, List<Version>> changeSet = vl.getLatestForActivities();
            for( Activity activity : changeSet.keySet() ) {
                csg.addAcitivity( activity.getShortname(), activity.getHeadline(), activity.getUser(), changeSet.get( activity ) );
            }
        } else {
            for( Activity activity : activities ) {
                VersionList versions = new VersionList( activity.changeset.versions ).getLatest();
                if(discard) {
                    versions = versions.addFilter(new ReadOnlyVersionFilter(viewRoot, readonly)).apply();
                }
                csg.addAcitivity( activity.getShortname(), activity.getHeadline(), activity.getUser(), versions );
            }
        }

        return csg.close().get();
    }

    public static class ChangeSetGenerator {
        private StringBuilder buffer = new StringBuilder(  );

        public ChangeSetGenerator createHeader( String header ) {
            buffer.append( "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" );
            buffer.append( "<changelog>" );
            buffer.append( "<changeset>" );
            buffer.append( "<entry>" );
            buffer.append( ( "<blName>" + header + "</blName>" ) );

            return this;
        }

        public ChangeSetGenerator addAcitivity( String name, String header, String username, List<Version> versions ) {
            if(versions.size() > 0) {
                buffer.append( "<activity>" );
                buffer.append( "<actName>" + name + "</actName>" );
                buffer.append( "<actHeadline>" + header + "</actHeadline>" );
                buffer.append( "<author>" + username + "</author>" );
                String temp = null;
                for( Version v : versions ) {
                    try {
                        temp = "<file>" + v.getSFile() + " (" + v.getVersion() + ") user: " + v.blame() + "</file>";
                    } catch( ClearCaseException e ) {
                        logger.warning( "Could not generate log" );
                    }
                    buffer.append( temp );
                }
                buffer.append( "</activity>" );
            }
            return this;
        }

        public ChangeSetGenerator close() {
            buffer.append( "</entry>" );
            buffer.append( "</changeset>" );
            buffer.append( "</changelog>" );

            return this;
        }

        public String get() {
            return buffer.toString();
        }
    }

	public static SnapshotView makeView( Stream stream, File workspace, BuildListener listener, String loadModule, File viewroot, String viewtag ) throws ScmException {
		return makeView( stream, workspace, listener, loadModule, viewroot, viewtag, true );
	}

	public static SnapshotView makeView( Stream stream, File workspace, BuildListener listener, String loadModule, File viewroot, String viewtag, boolean update ) throws ScmException {

		PrintStream hudsonOut = listener.getLogger();
		SnapshotView snapview = null;

		hudsonOut.println( "[" + Config.nameShort + "] View root: " + viewroot.getAbsolutePath() );
		hudsonOut.println( "[" + Config.nameShort + "] View tag : " + viewtag );

		boolean pathExists = false;

		/*
		 * Determine if there is a view path if not, create it
		 */
		try {
			if( viewroot.exists() ) {
				pathExists = true;
				hudsonOut.println( "[" + Config.nameShort + "] Reusing view root" );
			} else {
				if( viewroot.mkdir() ) {
				} else {
					throw new ScmException( "Could not create folder for view root:  " + viewroot.toString(), null );
				}
			}
		} catch( Exception e ) {
			throw new ScmException( "Could not make workspace (for viewroot " + viewroot.toString() + "). Cause: " + e.getMessage(), e );

		}

		hudsonOut.println( "[" + Config.nameShort + "] Determine if view tag exists" );
		if( UCMView.viewExists( viewtag ) ) {
			hudsonOut.println( "[" + Config.nameShort + "] Reusing view tag" );
			try {
				String vt = SnapshotView.viewrootIsValid( viewroot );
				hudsonOut.println( "[" + Config.nameShort + "] UUID resulted in " + vt );
				/* Not the correct view tag given the view */
				if( !vt.equals( viewtag ) && pathExists ) {
					hudsonOut.println( "[" + Config.nameShort + "] View tag is not the same as " + vt );
					/* Delete view */
					FilePath path = new FilePath( viewroot );
					hudsonOut.println( "[" + Config.nameShort + "] Trying to delete " + path );
					try {
						path.deleteRecursive();
					} catch( Exception e ) {
						throw new ScmException( "Unable to recursively prepare view root", e );
					}
					makeView( stream, workspace, listener, loadModule, viewroot, viewtag );
				}
			} catch( ClearCaseException ucmE ) {
				try {
					hudsonOut.println( "[" + Config.nameShort + "] Regenerating invalid view root" );
					UCMView.end( viewtag );
					SnapshotView.regenerateViewDotDat( viewroot, viewtag );
				} catch( ClearCaseException ucmEx ) {
					ucmEx.print( hudsonOut );
					throw new ScmException( "Could not make workspace - could not regenerate view", ucmEx );
				} catch( IOException e ) {
					throw new ScmException( "Could not make workspace - could not regenerate view", e );
				}
			} catch( Exception e ) {
				hudsonOut.println( "[" + Config.nameShort + "] Failed making workspace: " + e.getMessage() );
				throw new ScmException( "Failed making workspace", e );
			}

			hudsonOut.println( "[" + Config.nameShort + "] Getting snapshotview" );
			try {
				snapview = SnapshotView.get( viewroot );
			} catch( ClearCaseException e ) {
				e.print( hudsonOut );
				throw new ScmException( "Could not get view for workspace", e );
			} catch( IOException e ) {
				throw new ScmException( "Could not get view for workspace", e );
			}
		} else {
			try {
				hudsonOut.println( "[" + Config.nameShort + "] Creating new view" );
				snapview = SnapshotView.create( stream, viewroot, viewtag );

				hudsonOut.println( "[" + Config.nameShort + "] Created new view in local workspace: " + viewroot.getAbsolutePath() );
			} catch( ClearCaseException e ) {
				e.print( hudsonOut );
				throw new ScmException( "View not found in this region, but views with viewtag '" + viewtag + "' might exist in the other regions. Try changing the region Hudson or the slave runs in.", e );
			} catch( IOException e ) {
				throw new ScmException( "Unable to create view: " + e.getMessage(), e );
			}
		}

		if( update ) {
			try {
				hudsonOut.println( "[" + Config.nameShort + "] Updating view using " + loadModule.toLowerCase() + " modules." );
				snapview.Update( true, true, true, false, new LoadRules2( snapview, Components.valueOf( loadModule.toUpperCase() ) ) );
			} catch( ClearCaseException e ) {
				e.print( hudsonOut );
				if( e instanceof ViewException ) {
					if( ((ViewException)e).getType().equals( ViewException.Type.REBASING ) ) {
						hudsonOut.println( "The view is currently being used to rebase another stream" );
					}
				}
				throw new ScmException( "Could not update snapshot view", e );
			}
		}

		return snapview;
	}

    public static void println( PrintStream out, Object msg ) {
        out.println( "[" + Config.nameShort + "] " + msg.toString()  );
    }

    public static void storeException( File file, Throwable throwable ) throws IOException {
        file.delete();
        PrintWriter out = new PrintWriter( file );
        throwable.printStackTrace( out );
        out.close();
    }

    private static String sanitize( String str ) {
        return str.replaceAll( "\\s", "_" );
    }
    
    /**
     * This method generalizes some functionality. The logic to create the viewtag is now also used used in
     * the {@link CheckoutTask} class and the {@link RemoteDeliver} class. This is an attempt to fix JENKINS-20748
     * @param str
     * @return 
     */
    public static String createAndSanitizeCCUCMViewTag(String str) {
        String viewtag = null;        
        if(SystemUtils.IS_OS_WINDOWS) {
            viewtag = "CCUCM_" + sanitize( str ) + "_" + System.getenv( "COMPUTERNAME" );
        } else {
            try {
                String inetAdr = InetAddress.getLocalHost().getHostName();
                viewtag = "CCUCM_" + sanitize( str ) + "_" + inetAdr;
            } catch (UnknownHostException ex) {
                logger.log(Level.WARNING, "Failed to get hostname of localhost", ex);
        }
             
        }        
        return viewtag;
    }

    public static String createViewTag( String str, Stream stream ) throws ScmException {
        
        String viewtag = createAndSanitizeCCUCMViewTag(str);
        
        if(viewtag == null) {
            throw new ScmException("Failed to create view tag", new Exception());
        }
        
        viewtag += "_"+stream.getShortname();
        
        return viewtag;
    }
    
}
