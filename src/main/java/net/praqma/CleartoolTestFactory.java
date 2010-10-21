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
	
	private static Element root      = null;
	private static Element baselines = null;
	
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
		
		root      = testBase.getDocumentElement();
		baselines = this.GetFirstElement( root, "baselines" );
		
		logger.debug( "root=" + root.getTagName() );
		logger.debug( "baselines=" + baselines.getTagName() );
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
	
	/**
	 * Loads a baseline from XML Document
	 */
	public String LoadBaseline( String fqname )
	{
		Element ble = GetElementWithFqname( baselines, fqname );
		
		/* ($shortname, $component, $stream, $plevel, $user) */
		String baseline = GetElement( ble, "shortname" ).getTextContent() + "::" + 
		 				  GetElement( ble, "component" ).getTextContent() + "::" +
		 				  GetElement( ble, "stream" ).getTextContent() + "::" +
		 				  GetElement( ble, "plevel" ).getTextContent() + "::" +
		 				  GetElement( ble, "user" ).getTextContent() + "::";
		
		return baseline;
	}

	/**
	 * 
	 */
	public String diffbl( String nmerge, String fqname )
	{
		logger.trace_function();
		
		//String cmd = "diffbl -pre -act -ver " + nmerge + fqname;
		
		Element ble = GetElementWithFqname( baselines, fqname );
		
		return GetElement( ble, "diffbl" ).getTextContent();
	}
	
	/**
	 * Maybe this should dynamically create the element buildinprogress?!?
	 */
	public void BaselineMakeAttribute( String fqname, String attr )
	{
		logger.trace_function();
		
		logger.debug( fqname + " = " + attr );
		Element bip = GetElement( GetElement( GetElementWithFqname( baselines, fqname ), "attributes" ), attr );
//		Element e1 = GetElementBtFqname( baselines, fqname );
//		Element e2 = GetElement( e1, "attributes" );
//		Element bip = GetElement( e2, attr );
		bip.setTextContent( "true" );
	}
	
	public void SetPromotionLevel( String fqname, String plevel )
	{
		logger.trace_function();
		
		logger.debug( "setting plevel " + plevel );
		
		Element pl = GetElement( GetElementWithFqname( baselines, fqname ), "plevel" );
		pl.setTextContent( plevel );
		//logger.debug( "---->" + pl.getTextContent() + " + " + plevel );
	}
	
	public String GetPromotionLevel( String fqname )
	{
		logger.trace_function();
		
		String plevel = GetElement( GetElementWithFqname( baselines, fqname ), "plevel" ).getTextContent( );
		logger.debug( "Getting plevel " + plevel );
		return plevel;
	}
	
	/**
	 * Maybe this should dynamically create the element buildinprogress?!?
	 */
	public void BaselineRemoveAttribute( String fqname, String attr )
	{
		Element bip = GetElement( GetElement( GetElementWithFqname( baselines, fqname ), "attributes" ), attr );
		bip.setTextContent( "false" );
	}
	
	/**
	 * UNTESTED lsbl_s_comp_stream
	 */
	public String[] lsbl_s_comp_stream( String component, String stream )
	{
		logger.trace_function();
		
		//String cmd = "lsbl -s -component "  + this.GetFQName() + " -stream " + stream.GetFQName();
		
		Element lsbl = GetLsblElementCompStream( component, stream );
		
		return lsbl.getTextContent().split( "\n" );
	}
	
	
	/* AUXILIARY FUNCTIONS */
	
	private Element GetElementWithFqname( Element e, String fqname )
	{
		logger.trace_function();
		logger.debug( "Getting " + e.getNodeName() + " element with fqname: " + fqname );
		
		NodeList list = e.getChildNodes( );
		
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
	
	private Element GetElement( Element e, String tag )
	{
		logger.trace_function();
		logger.debug( "Getting "+e.getNodeName()+" element: " + tag );
		
		NodeList list = e.getChildNodes( );
		
		for( int i = 0 ; i < list.getLength( ) ; i++ )
		{
	    	Node node = list.item( i );
	    	
    		if( node.getNodeType( ) == Node.ELEMENT_NODE )
    		{
    			if( node.getNodeName().equals( tag ) )
    			{
    				return (Element)node;
    			}
    		}
		}
		
		return null;
	}


	private Element GetDiffblElement( String fqname )
	{
		logger.trace_function();
		logger.debug( "Getting diffbl element: " + fqname );
		
		NodeList list = baselines.getChildNodes( );
		
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
	
	private Element GetLsblElementCompStream( String component, String stream )
	{
		logger.trace_function();
		logger.debug( "Getting lsbl element for component: " + component );
		
		NodeList list = baselines.getChildNodes( );
		
		for( int i = 0 ; i < list.getLength( ) ; i++ )
		{
	    	Node node = list.item( i );
	    	
    		if( node.getNodeType( ) == Node.ELEMENT_NODE )
    		{
    			
    			HashMap<String, String> attrs = GetAttributes( (Element)node );
    			
    			if( attrs.get( "component" ).equals( component ) && attrs.get( "stream" ).equals( stream ) )
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