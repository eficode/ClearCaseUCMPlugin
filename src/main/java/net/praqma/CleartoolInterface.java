package net.praqma;

interface CleartoolInterface
{
	//public AbstractCleartoolFactory CFGet();
	public void Update();
	
	/* Baselines */
	public String LoadBaseline( String fqname );
	public void BaselineMakeAttribute( String fqname, String attr );
	public void BaselineRemoveAttribute( String fqname, String attr );
	public void SetPromotionLevel( String fqname, String plevel );
	public String GetPromotionLevel( String fqname );
	public boolean BuildInProgess( String fqname );
	
	/* Components */
	public String[] ListBaselines( String component, String stream, String plevel );
	
	/* Streams */
	public String GetRecommendedBaseline( String stream );
	public void RecommendBaseline( String stream, String baseline ) throws CleartoolException;
	
	/* Versions */
	public String LoadVersion( String version );
	
	/* Changesets */
	public String LoadChangeset( String changeset );
	
	/* Activities */
	public String GetChangeset( String activity );
	
	/* Diffs */
	public String diffbl( String nmerge, String fqname );
	
	/* Lists */
	public String[] lsbl_s_comp_stream( String compFqname, String streamFqname );
}