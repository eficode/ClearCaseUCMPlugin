package net.praqma.hudson.scm;

import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Logger;

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

	protected static final Logger logger = Logger.getLogger( ChangeLogSetImpl.class.getName()  );
	private List<ChangeLogEntryImpl> entries = null;
	private String baselineName;

	protected ChangeLogSetImpl( AbstractBuild<?, ?> build, List<ChangeLogEntryImpl> entries ) {
		super( build );

		this.entries = Collections.unmodifiableList( entries );
		for( ChangeLogEntryImpl entry : entries ) {
			entry.setParent( this );
		}
	}

    @Override
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
	 * @return A list of {@link ChangeLogEntryImpl}
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
