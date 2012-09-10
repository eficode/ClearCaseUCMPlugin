package net.praqma.hudson.scm;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import hudson.model.User;
import hudson.scm.ChangeLogSet.Entry;

import net.praqma.util.debug.Logger;

/**
 * A change set is a collection of changed entries. This classes represents one
 * entry, which is a user, a comment and a list of versions
 * 
 * @author Troels Selch
 * @author Margit Bennetzen
 * 
 */
public class ChangeLogEntryImpl extends Entry {

	private ChangeLogSetImpl parent;
	private String actName;
	private String actHeadline;
	private String msg;
	private String author;
	private String date;
	protected static Logger logger = Logger.getLogger();
	private volatile List<String> affectedPaths = new ArrayList<String>();

	public ChangeLogEntryImpl() {
	}

	/**
	 * Hudson calls this to show changes on the changes-page
	 */
	@Override
	public Collection<String> getAffectedPaths() {
		// a baseline can be set without any files changed - but then we wont
		// build
		return affectedPaths;
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

	// Digester in ChangeLogParserImpl cannot call setAuthor successfully, but
	// setMyAuthor works.
	public void setMyAuthor( String author ) {
		this.author = author;
	}

	/**
	 * This is to tell the Entry which Changeset it belongs to
	 * 
	 * @param parent
	 */
	public void setParent( ChangeLogSetImpl parent ) {
		this.parent = parent;
	}

	/**
	 * Used in digest.jelly to get the message attached to the entry
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
