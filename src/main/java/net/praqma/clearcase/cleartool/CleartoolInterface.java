package net.praqma.clearcase.cleartool;

import java.util.ArrayList;
import java.util.HashMap;

import net.praqma.debug.Debug;

public interface CleartoolInterface
{
	//public AbstractCleartoolFactory CFGet();
	public void Update();
	
	/* Baselines */
	public String LoadBaseline( String fqname );
	public ArrayList<String> GetBaselineActivities( String baseline );
	public ArrayList<String> GetBaselineDiffsNmergePrevious( String baseline );
	public void BaselineMakeAttribute( String fqname, String attr );
	public void BaselineRemoveAttribute( String fqname, String attr );
	public void SetPromotionLevel( String fqname, String plevel );
	public String GetPromotionLevel( String fqname );
	public boolean BuildInProgess( String fqname );
	
	/* Components */
	public ArrayList<String> ListBaselines( String component, String stream, String plevel, boolean shortnames );
	
	/* Streams */
	public String GetRecommendedBaseline( String stream );
	public void RecommendBaseline( String stream, String baseline ) throws CleartoolException;
	
	/* Versions */
	public HashMap<String, String> LoadVersion( String version );
	
	/* Activities */
	public String GetChangeset( String activity );
	
	/* Diffs */
	public String diffbl( String nmerge, String fqname );
	
	/* Lists */
	public String[] lsbl_s_comp_stream( String compFqname, String streamFqname );
}