package net.praqma;

import java.util.Collection;

import hudson.model.User;
import hudson.scm.ChangeLogSet;
import hudson.scm.ChangeLogSet.Entry;

public class ChangeLogEntryImpl extends Entry {
	
	private String filepath;
	protected static Debug logger = Debug.GetLogger();
	
	public ChangeLogEntryImpl(String filepath){
		logger.trace_function();
		this.filepath = filepath;
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
		User u = User.getUnknown();
		logger.log(" Unknown user: "+u.toString());
		return u;
	}

	public String getFilepath() {
		// TODO Auto-generated method stub
		logger.trace_function();
		return filepath;
	}
	
	public void setParent(ChangeLogSet parent){
		logger.trace_function();
		super.setParent(parent);
	}

	@Override
	public String getMsg() {
		// TODO Auto-generated method stub
		return "The answer is 42";
	}

}
