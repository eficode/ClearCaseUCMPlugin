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
	
	/* Diffs */
	public String diffbl( String nmerge, String fqname );
	
	/* Lists */
	public String[] lsbl_s_comp_stream( String compFqname, String streamFqname );
}