package net.praqma.hudson.scm;

import java.util.ArrayList;
import java.util.List;

import net.praqma.clearcase.ucm.entities.Baseline;
import net.praqma.clearcase.ucm.entities.Component;
import net.praqma.clearcase.ucm.entities.Stream;

public class PucmState
{
	private List<State> state = new ArrayList<State>();
	
	public State getState( String jobName, Integer jobNumber )
	{
		for( State s : state )
		{
			if( s.getJobName().equals( jobName ) && s.getJobNumber() == jobNumber )
			{
				return s;
			}
		}
		
		return new State( jobName, jobNumber );
	}
	
	public boolean removeState( String jobName, Integer jobNumber )
	{
		for( State s : state )
		{
			if( s.getJobName().equals( jobName ) && s.getJobNumber() == jobNumber )
			{
				state.remove( s );
				return true;
			}
		}
		
		return false;
	}
	
	public State getStateByBaseline( String jobName, String baseline )
	{
		for( State s : state )
		{
			if( s.getJobName().equals( jobName ) && s.getBaseline().GetFQName().equals( baseline ) )
			{
				return s;
			}
		}
		
		return null;		
	}



	public class State
	{
		private Baseline  baseline;
		private Stream    stream;
		private Component component;
		private boolean   doPostBuild = true;
		
		private String    jobName;
		private Integer   jobNumber;
		
		
		public State(){}
		public State( String jobName, Integer jobNumber )
		{
			this.jobName   = jobName;
			this.jobNumber = jobNumber;
		}
		public State( String jobName, Integer jobNumber, Baseline baseline, Stream stream, Component component, boolean doPostBuild )
		{
			this.jobName     = jobName;
			this.jobNumber   = jobNumber;
			this.baseline    = baseline;
			this.stream      = stream;
			this.component   = component;
			this.doPostBuild = doPostBuild;
		}
		
		public Baseline getBaseline()
		{
			return baseline;
		}
		public void setBaseline( Baseline baseline )
		{
			this.baseline = baseline;
		}
		public Stream getStream()
		{
			return stream;
		}
		public void setStream( Stream stream )
		{
			this.stream = stream;
		}
		public Component getComponent()
		{
			return component;
		}
		public void setComponent( Component component )
		{
			this.component = component;
		}
		public boolean isDoPostBuild()
		{
			return doPostBuild;
		}
		public void setDoPostBuild( boolean doPostBuild )
		{
			this.doPostBuild = doPostBuild;
		}
		public String getJobName()
		{
			return jobName;
		}
		public void setJobName( String jobName )
		{
			this.jobName = jobName;
		}
		public Integer getJobNumber()
		{
			return jobNumber;
		}
		public void setJobNumber( Integer jobNumber )
		{
			this.jobNumber = jobNumber;
		}
	}
	
	

}
