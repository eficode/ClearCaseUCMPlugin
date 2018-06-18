package net.praqma.hudson;

import edu.umd.cs.findbugs.annotations.*;
import hudson.FilePath;
import hudson.model.AbstractBuild;
import hudson.model.BuildListener;

import java.io.*;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import net.praqma.clearcase.exceptions.ClearCaseException;
import net.praqma.clearcase.exceptions.ViewException;
import net.praqma.clearcase.ucm.entities.Activity;
import net.praqma.clearcase.ucm.entities.Baseline;
import net.praqma.clearcase.ucm.entities.Project;
import net.praqma.clearcase.ucm.entities.Stream;
import net.praqma.clearcase.ucm.entities.Version;
import net.praqma.clearcase.ucm.view.SnapshotView;
import net.praqma.clearcase.ucm.view.UCMView;
import net.praqma.clearcase.ucm.view.SnapshotView.Components;
import net.praqma.clearcase.ucm.view.SnapshotView.LoadRules2;
import net.praqma.clearcase.ucm.view.UpdateView;
import net.praqma.hudson.exception.ScmException;
import net.praqma.hudson.remoting.CreateChangeSetRemote;
import org.apache.commons.lang.SystemUtils;

@SuppressFBWarnings("")
public abstract class Util {

	private static final Logger logger = Logger.getLogger( Util.class.getName() );

	public static Project.PromotionLevel getLevel( String level ) {
		if( level.equalsIgnoreCase( "any" ) ) {
			return null;
		} else {
			return Project.getPlevelFromString( level );
		}
	}
    
    public static String createChangelog(AbstractBuild<?, ?> build, List<Activity> activities, Baseline bl, boolean trimmed, File viewRoot, List<String> readonly, boolean ignoreReadOnly) throws IOException, InterruptedException {
        return Util.createChangelog(build, activities, bl, trimmed, viewRoot, readonly, ignoreReadOnly, true);
    }

    public static String createChangelog(AbstractBuild<?, ?> build, List<Activity> activities, Baseline bl, boolean trimmed, File viewRoot, List<String> readonly, boolean ignoreReadOnly, boolean useSlaves ) throws IOException, InterruptedException {
        if(useSlaves) {
            return build.getWorkspace().act(new CreateChangeSetRemote(activities, bl, trimmed, viewRoot, readonly, ignoreReadOnly));
        } else {
          CreateChangeSetRemote set = new CreateChangeSetRemote(activities, bl, trimmed, viewRoot, readonly, ignoreReadOnly);
          return set.invoke(null, null);            
        }
    }

    public static class ChangeSetGenerator {
        private StringBuilder buffer = new StringBuilder(  );

        public ChangeSetGenerator createHeader( String header ) {
            buffer.append( "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" );
            buffer.append( "<changelog>" );
            buffer.append( "<changeset>" );
            buffer.append( "<entry>" );
            buffer.append( "<blName>" + header + "</blName>" );
            return this;
        }

        public ChangeSetGenerator addAcitivity( String name, String header, String username, List<Version> versions ) {
            if(versions.size() > 0) {
                buffer.append( "<activity>" );
                buffer.append("<actName>").append(name).append("</actName>");
                buffer.append("<actHeadline>").append(header).append("</actHeadline>");
                buffer.append("<author>").append(username).append("</author>");
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

	public static SnapshotView makeView( Stream stream, File workspace, BuildListener listener, String loadModule, File viewroot, String viewtag, boolean update ) throws ScmException {

		PrintStream hudsonOut = listener.getLogger();
		SnapshotView snapview = null;

		hudsonOut.println( "[" + Config.nameShort + "] View root: " + viewroot.getAbsolutePath() );
		hudsonOut.println( "[" + Config.nameShort + "] View tag : " + viewtag );

		boolean pathExists = false;

		try {
			if( viewroot.exists() ) {
				pathExists = true;
				hudsonOut.println( "[" + Config.nameShort + "] Reusing view root" );
			} else {
				if( !viewroot.mkdir() ) {
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
					makeView( stream, workspace, listener, loadModule, viewroot, viewtag, true );
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
                UpdateView uw = new UpdateView(snapview).swipe().generate().overwrite().setLoadRules(new LoadRules2(Components.valueOf( loadModule.toUpperCase() ) ));
                uw.update();				
			} catch( ClearCaseException e ) {
				e.print( hudsonOut );
                if(e instanceof ViewException && ((ViewException)e).getType().equals( ViewException.Type.REBASING ) ) {
                    hudsonOut.println( "The view is currently being used to rebase another stream" );
                }
				throw new ScmException( "Could not update snapshot view", e );
			} catch (IOException ioex) {
                throw new ScmException("Could not update snapshot view, failed with IOException", ioex);
            }
		}

		return snapview;
	}

    public static void println( PrintStream out, Object msg ) {
        out.println( "[" + Config.nameShort + "] " + msg.toString()  );
    }

    private static String sanitize( String str ) {
        return str.replaceAll( "\\s", "_" );
    }
    
    /**
     * A method that fixes JENKINS-20748
     * @param str The base view tag name
     * @return A view tag with CCUCM prefixed, and the computer name appended.
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
