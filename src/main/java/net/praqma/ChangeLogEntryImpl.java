package net.praqma;

import java.util.Collection;

import hudson.model.User;
import hudson.scm.ChangeLogSet;
import hudson.scm.ChangeLogSet.Entry;

public class ChangeLogEntryImpl extends Entry {
	
	private String msg;
	protected static Debug logger = Debug.GetLogger();
	
	public ChangeLogEntryImpl(String msg){
		logger.trace_function();
		this.msg = msg;
	}

	@Override
	public Collection<String> getAffectedPaths() {
		// TODO Auto-generated method stub
		logger.trace_function();
		return null;
	}

	@Override
	public User getAuthor() {
		// TODO Auto-generated method stub
		logger.trace_function();
		return null;
	}

	@Override
	public String getMsg() {
		// TODO Auto-generated method stub
		logger.trace_function();
		return msg + "Svaret er 42";
	}
	
	public void setParent(ChangeLogSet parent){
		logger.trace_function();
		super.setParent(parent);
	}

}
