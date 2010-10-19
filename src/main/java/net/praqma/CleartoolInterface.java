package net.praqma;

interface CleartoolInterface
{
	//public AbstractCleartoolFactory CFGet();
	public void Update();
	
	/* Diffs */
	public String diffbl( String nmerge, String fqname );
}