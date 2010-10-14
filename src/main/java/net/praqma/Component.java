package net.praqma;

class Component extends ClearBase
{
	private String fqobj     = null;
	private String shortname = null;
	private String pvob      = null;
	private String rootdir   = null;
	
	
	public Component( String fqobj, boolean trusted )
	{
		/* Prefix the object with component: */
		if( !fqobj.startsWith( "component:" ) )
		{
			fqobj = "component:" + fqobj;
		}
		
		this.fqobj = fqobj;
		String[] res = TestComponent( fqobj );
		
		this.shortname = res[0];
		this.pvob      = res[1];
	}
	
	

}