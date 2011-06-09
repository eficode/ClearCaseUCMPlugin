package net.praqma.hudson.scm;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import net.praqma.clearcase.ucm.UCMException;
import net.praqma.clearcase.ucm.entities.Baseline;
import net.praqma.clearcase.ucm.entities.Project;
import net.praqma.hudson.scm.StoredBaselines.StoredBaseline;

public class StoredBaselines
{
	public class StoredBaseline
	{
		String baseline = "";
		long time = 0;
		Project.Plevel plevel;

		StoredBaseline( String baseline, Project.Plevel plevel )
		{
			this.baseline = baseline;
			this.time = System.currentTimeMillis();
			this.plevel = plevel;
		}

		StoredBaseline( Baseline baseline )
		{
			this.baseline = baseline.getFullyQualifiedName();
			this.time = System.currentTimeMillis();
			try
			{
				this.plevel = baseline.getPromotionLevel( true );
			}
			catch( UCMException e )
			{
				this.plevel = Project.Plevel.REJECTED;
			}
		}

		public String toString()
		{
			return this.baseline + "(" + this.plevel + ", " + StoredBaselines.milliToMinute( System.currentTimeMillis() - this.time ) + ")";
		}
	}

	List<StoredBaseline> baselines = Collections.synchronizedList( new ArrayList<StoredBaseline>() );

	public void addBaseline( String baseline, Project.Plevel plevel )
	{
		baselines.add( new StoredBaseline( baseline, plevel ) );
	}

	public boolean addBaseline( Baseline baseline )
	{
		return baselines.add( new StoredBaseline( baseline ) );
	}

	/**
	 * Prunes the list of baselines and returns the number of baselines removed
	 * @param threshold
	 * @return
	 */
	public int prune( long threshold )
	{
		long now = System.currentTimeMillis();
		int c = 0;

		Iterator<StoredBaseline> it = baselines.iterator();

		while( it.hasNext() )
		{
			StoredBaseline bl = it.next();

			System.out.print( bl.toString() + ": THOLD=" + ( ( bl.time + threshold ) < now ) );
			/* Remove baselines lower than threshold */
			if( ( bl.time + threshold ) < now )
			{
				System.out.println( "[Removed]" );
				it.remove();
				c++;
			}
			else
			{
				System.out.println( "[KEPT]" );
			}
		}

		return c;
	}

	public StoredBaseline getBaseline( String baseline )
	{
		for( StoredBaseline bl : baselines )
		{
			if( bl.baseline.equals( baseline ) )
			{
				return bl;
			}
		}

		return null;
	}

	public static float milliToMinute( long milli )
	{
		return ( (float)milli / 60000 );
	}

	public String toString()
	{
		StringBuffer sb = new StringBuffer();
		Long now = System.currentTimeMillis();

		for( StoredBaseline bl : baselines )
		{
			sb.append( "(" + bl.baseline + ", " + bl.plevel + ", " + milliToMinute( now - bl.time ) + ")\n" );
		}

		return sb.toString();
	}
}