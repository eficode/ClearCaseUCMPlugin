package net.praqma;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import hudson.model.User;
import hudson.scm.ChangeLogSet;
import hudson.scm.ChangeLogSet.Entry;

public class ChangeLogEntryImpl extends Entry {
	
	private String dummydata;
	private String filepath;
	protected static Debug logger = Debug.GetLogger();
	private volatile List<String> affectedPaths;
	
	public ChangeLogEntryImpl(String filepath){
		logger.trace_function();
		this.filepath = filepath;
	}

	@Override
	public Collection<String> getAffectedPaths() {
		logger.trace_function();
		if(affectedPaths==null){
			List<String> r = new ArrayList<String>();
			r.add("En filepath");
			affectedPaths = r;
		}
		return affectedPaths;
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
		return "The answer is 42";
	}
	
	public void setDummydata(String dummydata){
		this.dummydata = dummydata;
	}
	
	public String getDummydata(){
		
		return dummydata;
	}

}
