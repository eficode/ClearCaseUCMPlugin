package net.praqma.hudson.scm;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.logging.Logger;

import hudson.model.User;
import hudson.scm.ChangeLogSet.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A change set is a collection of changed entries. This classes represents one
 * entry, which is a user, a comment and a list of versions
 * 
 * @author Troels Selch
 * @author Margit Bennetzen
 * 
 */
public class ChangeLogEntryImpl extends Entry {

    private static final transient Pattern splitChangeSet = Pattern.compile("^([^\\(]+)\\(([^\\)]+)(.*?)(\\S+)(\\S+)(.*)");
    private ChangeLogSetImpl parent;
    private String actName;
    private String actHeadline;
    private String msg;
    private String author;
    private String date;

	protected static final Logger logger = Logger.getLogger( ChangeLogEntryImpl.class.getName()  );
	private volatile List<String> affectedPaths = new ArrayList<>();

	public ChangeLogEntryImpl() {
	}

	/**
	 * Hudson calls this to show changes on the changes-page
     * @return A string of path names for changes
	 */
	@Override
	public Collection<String> getAffectedPaths() {
		return affectedPaths;
	}
    
    public String getOnlyChangedFile(String fulltext) {
        Matcher m = splitChangeSet.matcher(fulltext);
        try {
            while(m.find()) {
                return m.group(1);
            }
        } catch (Exception ex) {
            //ignore;
        }
        return null;
    }
    
    public String getOnlyClearCaseChangedFile(String fulltext) {
        Matcher m = splitChangeSet.matcher(fulltext);
        try {
            while(m.find()) {
                return m.group(2);
            } 
        } catch (Exception ex) {
            //ignore
        }
        return null;
    }
    
    public String getUserClearCaseNumber(String fulltext) {
        Matcher m = splitChangeSet.matcher(fulltext);
        try {
            while(m.find()) {
                return m.group(6);
            } 
        } catch (Exception ex) {
            //ignore
        }
        return null;
    }

	public void setNextFilepath( String filepath ) {
		affectedPaths.add( filepath );
	}

	@Override
	public User getAuthor() {
		if( author == null ) {
			return User.getUnknown();
		}
		return User.get( author );
	}

	public void setMyAuthor( String author ) {
		this.author = author;
	}

	/**
	 * This is to tell the Entry which Change set it belongs to
	 * @param parent Ties entry to this parent
	 */
	public void setParent( ChangeLogSetImpl parent ) {
		this.parent = parent;
	}

	/**
	 * Used in digest.jelly to get the message attached to the entry
     * @return A message string
	 */
	@Override
	public String getMsg() {
		return actName;
	}

	public void setActName( String actName ) {
		this.actName = actName;
	}
	
	public void setActHeadline( String actHeadline ) {
		this.actHeadline = actHeadline;
	}
	
	public String getActHeadline() {
		return actHeadline;
	}
        
}
