package net.praqma.hudson.scm;

import java.util.ArrayList;
import java.util.List;

import net.praqma.clearcase.ucm.entities.Baseline;
import net.praqma.clearcase.ucm.entities.Component;
import net.praqma.clearcase.ucm.entities.Project;
import net.praqma.clearcase.ucm.entities.Stream;

public class PucmState
{
	private List<State> states = new ArrayList<State>();
	
	/**
	 * Get a state given job name and job number
	 * @param jobName the hudson job name
	 * @param jobNumber the hudson job number
	 * @return
	 */
	public State getState( String jobName, Integer jobNumber )
	{
		for( State s : states )
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
		for( State s : states )
		{
			if( s.getJobName().equals( jobName ) && s.getJobNumber() == jobNumber )
			{
				states.remove( s );
				return true;
			}
		}
		
		return false;
	}
	
	public State getStateByBaseline( String jobName, String baseline )
	{
		for( State s : states )
		{
			if( s.getJobName().equals( jobName ) && s.getBaseline().GetFQName().equals( baseline ) )
			{
				return s;
			}
		}
		
		return null;		
	}
	
	
	public void addState( State state )
	{
		this.states.add( state );
	}
	
	public boolean stateExists( State state )
	{
		return stateExists( state.jobName, state.jobNumber );
	}
	
	public boolean stateExists( String jobName, Integer jobNumber )
	{
		for( State s : states )
		{
			if( s.getJobName().equals( jobName ) && s.getJobNumber() == jobNumber )
			{
				return true;
			}
		}
		
		return false;
	}
	
	public boolean removeState( State state )
	{
		return states.remove( state );
	}



	public class State
	{
		private Baseline  baseline;
		private Stream    stream;
		private Component component;
		private boolean   doPostBuild = true;
		
		private Project.Plevel plevel;
		
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
		
		public void save()
		{
			PucmState.this.addState( this );
		}
		
		public boolean remove()
		{
			return PucmState.this.removeState( this );
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
		public boolean doPostBuild()
		{
			return doPostBuild;
		}
		public void setPostBuild( boolean doPostBuild )
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
		public void setPlevel( Project.Plevel plevel )
		{
			this.plevel = plevel;
		}
		public Project.Plevel getPlevel()
		{
			return plevel;
		}
		
		public String stringify()
		{
			StringBuffer sb = new StringBuffer();
			
			sb.append( "Job name  : " + this.jobName + "\n" );
			sb.append( "Job number: " + this.jobNumber + "\n" );
			sb.append( "Component : " + this.component + "\n" );
			sb.append( "Stream    : " + this.stream + "\n" );
			sb.append( "Plevel    : " + this.plevel.toString() + "\n" );
			sb.append( "postBuild : " + this.doPostBuild + "\n" );
			
			return sb.toString();
		}
	}
	
	

}
