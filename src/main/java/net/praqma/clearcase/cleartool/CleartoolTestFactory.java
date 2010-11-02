package net.praqma.clearcase.cleartool;

import net.praqma.debug.Debug;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Attr;
import org.w3c.dom.DOMError;
import org.w3c.dom.DOMException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class CleartoolTestFactory extends AbstractCleartoolFactory
{
	public static AbstractCleartoolFactory cfInstance = null;
	
	private static Document testBase = null;
	//private static final File testBaseFile = new File( "testbase.xml" );
	private static final String testBaseFile = "testbase.xml";
	//private static final File testBaseFile = new File( "c:\\temp\\testbase.xml" );
	
	private static Element root       = null;
	private static Element baselines  = null;
	private static Element streams    = null;
	private static Element versions   = null;
	private static Element activities = null;
	
	protected static final String filesep = System.getProperty( "file.separator" );
	
	private CleartoolTestFactory( boolean hudson )
	{
		logger.trace_function();
		
		/* The search result */
		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		factory.setNamespaceAware( true );
		
		DocumentBuilder builder;
		try
		{
			builder = factory.newDocumentBuilder();
			if( hudson )
			{
				logger.log( "Getting XML as stream" );
				testBase = builder.parse( this.getClass().getClassLoader().getResourceAsStream( testBaseFile ) );
			}
			else
			{
				logger.log( "Getting XML as file" );
				testBase = builder.parse( "src" + filesep + "main" + filesep + "resources" + filesep + testBaseFile );
			}
		}
		catch ( Exception e )
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		root       = testBase.getDocumentElement();
		baselines  = this.GetFirstElement( root, "baselines" );
		streams    = this.GetFirstElement( root, "streams" );
		versions   = this.GetFirstElement( root, "versions" );
		activities = this.GetFirstElement( root, "activities" );
		
		logger.debug( "root=" + root.getTagName() );
		logger.debug( "baselines=" + baselines.getTagName() );
		logger.debug( "streams=" + streams.getTagName() );
		logger.debug( "activities=" + activities.getTagName() );
	}
	
	public static AbstractCleartoolFactory CFGet( boolean hudson )
	{
		logger.trace_function();
		
		if( cfInstance == null )
		{
			cfInstance = new CleartoolTestFactory( hudson );
		}
		
		return cfInstance;
	}
	

	public void Update()
	{
		// TODO Auto-generated method stub
		
	}
	
	/*
	 * 
	 * Version functionality
	 * 
	 */
	
	public HashMap<String, String> LoadVersion( String version )
	{
		logger.trace_function();
		logger.debug( version );
		
		Element ve = GetElementWithFqname( versions, version );
		
		HashMap<String, String> result = new HashMap<String, String>();
		result.put( "date", GetElement( ve, "date" ).getTextContent() );
		result.put( "user", GetElement( ve, "user" ).getTextContent() );
		result.put( "machine", GetElement( ve, "machine" ).getTextContent() );
		result.put( "comment", GetElement( ve, "comment" ).getTextContent() );
		result.put( "checkedout", GetElement( ve, "checkedout" ).getTextContent() );
		result.put( "kind", GetElement( ve, "kind" ).getTextContent() );
		result.put( "branch", GetElement( ve, "branch" ).getTextContent() );

		return result;
	}
	
	
	
	public String LoadVersion_old( String version )
	{
		logger.trace_function();
		logger.debug( version );
		
		Element ve = GetElementWithFqname( versions, version );
		String res = GetElement( ve, "date" ).getTextContent() + "::" + 
		             GetElement( ve, "user" ).getTextContent() + "::" +
		             GetElement( ve, "machine" ).getTextContent() + "::" +
		             GetElement( ve, "comment" ).getTextContent() + "::" +
		             GetElement( ve, "checkedout" ).getTextContent() + "::" +
		             GetElement( ve, "kind" ).getTextContent() + "::" +
		             GetElement( ve, "branch" ).getTextContent() + "::"
		             ;
		
		return res;
	}
	
	/*
	 * 
	 * Changeset functionality
	 * 
	 */
	

	
	/*
	 * 
	 * ACTIVITY/CHANGESET FUNCTIONALITY
	 * 
	 */
	
	public String GetChangeset( String activity )
	{
		logger.trace_function();
		logger.debug( activity );
		
		/* Get the Changeset from the activity */
		Element ae = GetElementWithFqname( activities, activity );
		
		Element ce = GetElement( ae, "changeset" );
		
		NodeList list = ce.getElementsByTagName( "version" );
		StringBuffer sb = new StringBuffer();
		
		for( int i = 0 ; i < list.getLength( ) ; i++ )
		{
	    	Node node = list.item( i );
	    	
    		if( node.getNodeType( ) == Node.ELEMENT_NODE )
    		{
    			sb.append( node.getTextContent() + ", " );
    		}
		}
		
		return sb.toString();
	}
	
	/*
	 *  STREAM FUNCTIONALITY
	 *  
	 */
	
	public String GetRecommendedBaseline( String stream ) throws CleartoolException
	{
		logger.trace_function();
		logger.debug( "TestFactory: GetRecommendedBaseline "+ stream );
		
		Element e = GetElement( GetElementWithFqname( streams, stream ), "recommended_baseline" );
		
		if( e != null )
		{
			return e.getTextContent();
		}
		else
		{
			throw new CleartoolException( "Recommended baseline not found." );
		}
	}
	
	/**
	 * 
	 */
	public void RecommendBaseline( String stream, String baseline ) throws CleartoolException
	{
		logger.trace_function();
		logger.debug( "TestFactory: RecommendBaseline" );
		
		Element e = GetElement( GetElementWithFqname( streams, stream ), "recommended_baseline" );
		
		if( e == null )
		{
			throw new CleartoolException( "Could not set recommended baseline" );
		}
		
		e.setTextContent( baseline );
	}
	
	/*
	 *  BASELINE FUNCTIONALITY
	 *  
	 */
	
	public ArrayList<String> GetBaselineActivities( String baseline )
	{
		logger.trace_function();
		logger.debug( baseline );
		
		Element ble = GetElementWithFqname( baselines, baseline );
		Element act = GetElement( ble, "activities" );
		
		NodeList list = act.getChildNodes( );
		ArrayList<String> result = new ArrayList<String>();
		
		for( int i = 0 ; i < list.getLength( ) ; i++ )
		{
	    	Node node = list.item( i );
	    	
    		if( node.getNodeType( ) == Node.ELEMENT_NODE )
    		{
    			if( node.getNodeName().equalsIgnoreCase( "activity" ) )
    			{
    				result.add( node.getTextContent() );
    			}
    		}
		}
		
		return result;
	}
	
	public ArrayList<String> GetBaselineDiffsNmergePrevious( String baseline )
	{
		logger.trace_function();
		logger.debug( baseline );
		
		ArrayList<String> acts = GetBaselineActivities( baseline );
		
		ArrayList<String> result = new ArrayList<String>();
		
		for( String act : acts )
		{
			logger.debug( "GETTING ACTIVITY = " + act );
			/* Get the changeset from an activity */
			Element ce = GetElement( GetElementWithFqname( activities, act ), "changeset" );
			
			NodeList list = ce.getChildNodes( );
			
			for( int i = 0 ; i < list.getLength( ) ; i++ )
			{
		    	Node node = list.item( i );
		    	
	    		if( node.getNodeType( ) == Node.ELEMENT_NODE )
	    		{
	    			if( node.getNodeName().equalsIgnoreCase( "version" ) )
	    			{
	    				result.add( node.getTextContent() );
	    			}
	    		}
			}
		}
		
		return result;
	}
	
	/**
	 * Loads a baseline from XML Document
	 */
	public String LoadBaseline( String fqname ) throws CleartoolException
	{
		logger.trace_function();
		logger.debug( "TestFactory: LoadBaseline" );
		
		Element ble = GetElementWithFqname( baselines, fqname );
		
		if( ble == null )
		{
			throw new CleartoolException( "No baselines with name " + fqname );
		}
		
		String baseline = "";
		
		try
		{
			/* ($shortname, $component, $stream, $plevel, $user) */
			baseline = GetElement( ble, "shortname" ).getTextContent() + "::" + 
	 				   GetElement( ble, "component" ).getTextContent() + "::" +
	 				   GetElement( ble, "stream" ).getTextContent() + "::" +
	 				   GetElement( ble, "plevel" ).getTextContent() + "::" +
	 				   GetElement( ble, "user" ).getTextContent() + "::";
		}
		catch( DOMException e )
		{
			throw new CleartoolException( "For baseline " + fqname + ": " + e.getMessage() );
		}
		
		return baseline;
	}
	
	public ArrayList<String> ListBaselines( String component, String stream, String plevel, boolean shortnames )
	{
		logger.trace_function();
		logger.debug( "TestFactory: ListBaselines: " + component + ", " + stream + ", " + plevel );
		
		NodeList list = baselines.getChildNodes( );
		ArrayList<String> result = new ArrayList<String>();
		
		for( int i = 0 ; i < list.getLength( ) ; i++ )
		{
	    	Node node = list.item( i );
	    	
    		if( node.getNodeType( ) == Node.ELEMENT_NODE )
    		{
    			HashMap<String, String> attrs = GetAttributes( (Element)node );
    			
    			String c = GetElement( (Element)node, "component" ).getTextContent();
    			String s = GetElement( (Element)node, "stream" ).getTextContent();
    			String p = GetElement( (Element)node, "plevel" ).getTextContent();
    			if( c.equals( component ) && s.equals( stream ) && p.equals( plevel ) )
    			{
    				//sb.append( attrs.get( "fqname" ) );
    				if( shortnames )
    				{
    					result.add( GetElement( (Element)node, "shortname" ).getTextContent() );
    				}
    				else
    				{
    					result.add( attrs.get( "fqname" ) );
    				}
    			}
    		}
		}
		
		return result;
	}
	
	/**
	 * 
	 */
	public String diffbl( String nmerge, String fqname )
	{
		logger.trace_function();
		logger.debug( "TestFactory: diffbl" );
		
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
		
		logger.debug( "TestFactory: BaselineMakeAttribute" );
		
		logger.debug( fqname + " = " + attr );
		Element bip = GetElement( GetElement( GetElementWithFqname( baselines, fqname ), "attributes" ), attr );
//		Element e1 = GetElementBtFqname( baselines, fqname );
//		Element e2 = GetElement( e1, "attributes" );
//		Element bip = GetElement( e2, attr );
		bip.setTextContent( "true" );
	}
	
	public boolean BuildInProgess( String fqname )
	{
		logger.trace_function();
		logger.debug( "TestFactory: BuildInProgess" );
		return GetElement( GetElement( GetElementWithFqname( baselines, fqname ), "attributes" ), "BuildInProgress" ).getTextContent().equals( "true" );
	}
	
	public void SetPromotionLevel( String fqname, String plevel )
	{
		logger.trace_function();
		logger.debug( "TestFactory: SetPromotionLevel" );
		
		logger.debug( "setting plevel " + plevel );
		
		Element pl = GetElement( GetElementWithFqname( baselines, fqname ), "plevel" );
		pl.setTextContent( plevel );
		//logger.debug( "---->" + pl.getTextContent() + " + " + plevel );
	}
	
	public String GetPromotionLevel( String fqname )
	{
		logger.trace_function();
		logger.debug( "TestFactory: GetPromotionLevel" );
		
		String plevel = GetElement( GetElementWithFqname( baselines, fqname ), "plevel" ).getTextContent( );
		logger.debug( "Getting plevel " + plevel );
		return plevel;
	}
	
	/**
	 * Maybe this should dynamically create the element buildinprogress?!?
	 */
	public void BaselineRemoveAttribute( String fqname, String attr )
	{
		logger.trace_function();
		logger.debug( "TestFactory: BaselineRemoveAttribute" );
		Element bip = GetElement( GetElement( GetElementWithFqname( baselines, fqname ), "attributes" ), attr );
		bip.setTextContent( "false" );
	}
	
	/**
	 * UNTESTED lsbl_s_comp_stream
	 */
	public String[] lsbl_s_comp_stream( String component, String stream )
	{
		logger.trace_function();
		logger.debug( "TestFactory: lsbl_s_comp_stream" );
		
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
		//NodeList list = e.getElementsByTagName( "stream" );
		
		logger.debug( "MY SIZE="+list.getLength( ) );
		
		for( int i = 0 ; i < list.getLength( ) ; i++ )
		{
	    	Node node = list.item( i );
	    	
	    	if( node.getNodeType( ) == Node.ELEMENT_NODE )
    		{
    			HashMap<String, String> attrs = GetAttributes( (Element)node );
    			
//    			for( Map.Entry<String, String> pairs : attrs.entrySet() )
//    			{
//    		        System.out.println(pairs.getKey() + " = " + pairs.getValue());
//
//    			}
    			
    			if( attrs.get( "fqname" ) != null && attrs.get( "fqname" ).equals( fqname ) )
    			{
    				return (Element)node;
    			}
    		}
		}
		
		return null;
	}
	
	private Element GetElement( Element e, String tag ) throws DOMException
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
		
		throw new DOMException( DOMError.SEVERITY_WARNING, "Could not GetElement " + tag );
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
			logger.debug( "ATTR="+at.getNodeName() );
		}
		
		return list;
	}


	private Element GetFirstElement( Element root, String element )
	{
		logger.trace_function();
		
		//NodeList sections = root.getElementsByTagName( element );
		NodeList sections = root.getChildNodes();
		
	    int numSections = sections.getLength();

	    for ( int i = 0 ; i < numSections ; i++ )
	    {
	    	Node node = sections.item( i );
	    	
    		if( node.getNodeType( ) == Node.ELEMENT_NODE && node.getNodeName().equals( element ) )
    		{
    			return (Element)node;
    		}
	    }
	    
	    return null;
	}


}