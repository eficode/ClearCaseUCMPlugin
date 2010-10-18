package net.praqma;

import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import hudson.model.AbstractBuild;
import hudson.scm.ChangeLogSet;

public class ChangeLogSetImpl extends ChangeLogSet<ChangeLogEntryImpl> {
	
	protected static Debug logger = Debug.GetLogger();
	
	private List<ChangeLogEntryImpl> history = null;

	protected ChangeLogSetImpl(AbstractBuild<?, ?> build, List<ChangeLogEntryImpl> logs) {
		super(build);
		logger.trace_function();
		logger.print_trace();
		this.history = Collections.unmodifiableList(logs);
        for (ChangeLogEntryImpl log : logs) {
            log.setParent(this);
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

	
}
