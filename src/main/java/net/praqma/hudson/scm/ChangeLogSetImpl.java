package net.praqma.hudson.scm;

import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import net.praqma.util.debug.Logger;

import hudson.model.AbstractBuild;
import hudson.scm.ChangeLogSet;

/**
 * This class represents a ChangeLogSet. This is the accumulation of all the
 * entries on a baseline
 * 
 * @author Troels Selch
 * @author Margit Bennetzen
 * 
 */

public class ChangeLogSetImpl extends ChangeLogSet<ChangeLogEntryImpl> {

	protected static Logger logger = Logger.getLogger();
	private List<ChangeLogEntryImpl> entries = null;
	private String baselineName;

	protected ChangeLogSetImpl( AbstractBuild<?, ?> build, List<ChangeLogEntryImpl> entries ) {
		super( build );

		this.entries = Collections.unmodifiableList( entries );
		for( ChangeLogEntryImpl entry : entries ) {
			entry.setParent( this );
		}
	}

	public Iterator<ChangeLogEntryImpl> iterator() {
		return entries.iterator();
	}

	@Override
	public boolean isEmptySet() {
		return entries.isEmpty();
	}

	/**
	 * Used by index.jelly to display list of entries
	 * 
	 * @return
	 */
	public List<ChangeLogEntryImpl> getEntries() {
		return entries;
	}

	public void setBaselineName( String baselineName ) {
		this.baselineName = baselineName;
	}

	public String getBaselineName() {
		return baselineName;
	}
}
