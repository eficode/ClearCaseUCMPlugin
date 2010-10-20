package net.praqma;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

class CleartoolTestFactory extends AbstractCleartoolFactory
{
	public static AbstractCleartoolFactory cfInstance = null;
	
	private static Document testBase = null;
	private static final File testBaseFile = new File( "testbase.xml" );
	
	private static Element root    = null;
	private static Element diffbls = null;
	
	private CleartoolTestFactory()
	{
		logger.trace_function();
		
		/* The search result */
		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		factory.setNamespaceAware( true );

		DocumentBuilder builder;
		try
		{
			builder = factory.newDocumentBuilder();
			testBase = builder.parse( testBaseFile );
		}
		catch ( Exception e )
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		root = testBase.getDocumentElement();
		diffbls = this.GetFirstElement( root, "diffbls" );
		
		logger.debug( "root="+root.getTagName() );
		logger.debug( "diffbls="+diffbls.getTagName() );
	}
	
	public static AbstractCleartoolFactory CFGet()
	{
		logger.trace_function();
		
		if( cfInstance == null )
		{
			cfInstance = new CleartoolTestFactory();
		}
		
		return cfInstance;
	}

	public void Update()
	{
		// TODO Auto-generated method stub
		
	}

	public String diffbl( String nmerge, String fqname )
	{
		logger.trace_function();
		
		//String cmd = "diffbl -pre -act -ver " + nmerge + fqname;
		
		Element dbl = GetDiffblElement( fqname );
		
		StringBuffer sb = new StringBuffer();
		
		return dbl.getTextContent();
	}
	
	
	/* AUXILIARY FUNCTIONS */


	private Element GetDiffblElement( String fqname )
	{
		logger.trace_function();
		logger.debug( "Getting diffbl element: " + fqname );
		
		NodeList list = diffbls.getChildNodes( );
		
		for( int i = 0 ; i < list.getLength( ) ; i++ )
		{
	    	Node node = list.item( i );
	    	
    		if( node.getNodeType( ) == Node.ELEMENT_NODE )
    		{
    			
    			HashMap<String, String> attrs = GetAttributes( (Element)node );
    			
    			if( attrs.get( "fqname" ).equals( fqname ) )
    			{
    				return (Element)node;
    			}
    		}
		}
		
		return null;
	}
	
	private HashMap<String, String> GetAttributes( Element element )
	{
		logger.trace_function();
		
		NamedNodeMap nnm = element.getAttributes( );
		int size = nnm.getLength( );
		HashMap<String, String> list = new HashMap<String, String>( );
		
		for( int i = 0 ; i < size ; i++ )
		{
			Attr at = (Attr)nnm.item( i );
			list.put( at.getName( ), at.getValue( ) );
		}
		
		return list;
	}


	private Element GetFirstElement( Element root, String element )
	{
		logger.trace_function();
		
		NodeList sections = root.getElementsByTagName( element );
		
	    int numSections = sections.getLength();

	    for ( int i = 0 ; i < numSections ; i++ )
	    {
	    	Node node = sections.item( i );
	    	
    		if( node.getNodeType( ) == Node.ELEMENT_NODE )
    		{
    			return (Element)node;
    		}
	    }
	    
	    return null;
	}


}