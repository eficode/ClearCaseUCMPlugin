package net.praqma.scm;

import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import hudson.model.AbstractBuild;
import hudson.scm.ChangeLogSet;

import net.praqma.debug.Debug;

/**
 *This class represents a ChangeLogSet. This is the accumulation of all the entries on a baseline
 * 
 * @author Troels Selch Sørensen
 * @author Margit Bennetzen
 *
 */

public class ChangeLogSetImpl extends ChangeLogSet<ChangeLogEntryImpl> {
	
	protected static Debug logger = Debug.GetLogger();	
	private List<ChangeLogEntryImpl> history = null;

	protected ChangeLogSetImpl(AbstractBuild<?, ?> build, List<ChangeLogEntryImpl> entries) {
		super(build);
		logger.trace_function();
		logger.print_trace();
		this.history = Collections.unmodifiableList(entries);
        for (ChangeLogEntryImpl entry : entries) {
            entry.setParent(this);
        }
	}
	
	public Iterator<ChangeLogEntryImpl> iterator() {
		logger.trace_function();
		return history.iterator();
	}

	@Override
	public boolean isEmptySet() {
		logger.trace_function();
		return history.isEmpty();
	}
	
	public List<ChangeLogEntryImpl> getEntries(){
		logger.trace_function();
		return history;
	}
}
