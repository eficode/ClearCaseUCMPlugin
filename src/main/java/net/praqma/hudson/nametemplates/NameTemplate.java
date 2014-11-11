package net.praqma.hudson.nametemplates;

import hudson.FilePath;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.praqma.hudson.CCUCMBuildAction;
import net.praqma.hudson.exception.TemplateException;

public class NameTemplate {

	private static final Map<String, Template> templates = new HashMap<String, Template>();
	private static final Logger logger = Logger.getLogger( NameTemplate.class.getName() );

	static  {
		templates.put( "date", new DateTemplate() );
		templates.put( "time", new TimeTemplate() );
		templates.put( "stream", new StreamTemplate() );
		templates.put( "component", new ComponentTemplate() );
		templates.put( "baseline", new BaselineTemplate() );
		templates.put( "project", new ProjectTemplate() );
		templates.put( "ccversion", new ClearCaseVersionNumberTemplate() );
		templates.put( "number", new NumberTemplate() );
		templates.put( "user", new UserTemplate() );
		templates.put( "env", new EnvTemplate() );
		templates.put( "file", new FileTemplate() );
	}

	private static Pattern rx_ = Pattern.compile( "(\\[.*?\\])" );
	private static Pattern rx_checkFinal = Pattern.compile( "^[\\w\\._-]*$" );

    /**
     * This is the method we use to validate templates.
     * @param action 
     */
	public static void validateTemplates( CCUCMBuildAction action) {
		logger.finer( "Validating templates for " + action );
         //Only evaluate those that are actually chosen
        
		HashMap<String, String> chosentemplates = getChosenTemplates(action.getNameTemplate());
		for( Entry<String, String> val : chosentemplates.entrySet() ) {			
			try {
				logger.finer( "Validating " + val.getKey() );
				templates.get( val.getKey() ).parse( action, val.getValue(), null );
			} catch (TemplateException e) {
				logger.warning( "Could not validate " + val.getKey() );
			}
		}
	}
    
    public static void validateTemplates( CCUCMBuildAction action, FilePath ws) {
		logger.finer( "Validating templates for " + action );
         //Only evaluate those that are actually chosen
        
		HashMap<String, String> chosentemplates = getChosenTemplates(action.getNameTemplate());
		for( Entry<String, String> val : chosentemplates.entrySet() ) {			
			try {
				logger.finer( "Validating " + val.getKey() );
				templates.get( val.getKey() ).parse( action, val.getValue(), ws );
			} catch (TemplateException e) {
				logger.warning( "Could not validate " + val.getKey() );
			}
		}
	}

	public static String trim( String template ) {
        if( template.matches( "^\".+\"$" ) ) {
        	template = template.substring( 1, template.length()-1 );
        }
        return template;
	}

    /**
     * Method that extracts the names of the chose templates.
     * @param templatestring
     * @return a Set containing the name of the templates chosen.
     */
    public static HashMap<String,String> getChosenTemplates(String templatestring) {
        HashMap<String,String> chosenTemplates = new HashMap<String,String>();
        Matcher m = rx_.matcher( templatestring );

        while( m.find() ) {
                String replace = m.group(1);
                String templateName = replace.toLowerCase().substring( 1, replace.length()-1 );

                String templateValue = null;
                if( templateName.contains( "=" ) ) {
                        String[] s = templateName.split( "=" );
                        templateName = s[0];
                        templateValue = s[1];

                }
                chosenTemplates.put(templateName, templateValue);
        }
        return chosenTemplates;
    }
        
    /**
     * Checks to see if the templates are valid, and that the template names are available.
     * @param template
     * @return
     * @throws TemplateException 
     */
	public static boolean testTemplate( String template ) throws TemplateException {
		Matcher m = rx_.matcher( template );
		String result = template;

		while( m.find() ) {
			String replace = m.group(1);
			String templateName = replace.toLowerCase().substring( 1, replace.length()-1 );

			/*  Pre-process template */
			if( templateName.contains( "=" ) ) {
				String[] s = templateName.split( "=" );
				templateName = s[0];
			}

			if( !templates.containsKey( templateName ) ) {
				throw new TemplateException( "The template " + templateName + " does not exist" );
			} else {
				result = result.replace( replace, "" );
			}
		}

        Matcher f = rx_checkFinal.matcher( result );
		if( !f.find() ) {
			throw new TemplateException( "The template is not correct" );
		}

		return true;
	}
    
    /**
     * At this point. We do not need to filter chosen templates. 
     * @param template
     * @param action
     * @param ws
     * @return
     * @throws TemplateException 
     */
	public static String parseTemplate( String template, CCUCMBuildAction action, FilePath ws) throws TemplateException {
		Matcher m = rx_.matcher( template );
		String result = template;
		
		logger.finer( "PARSING TEMPLATE " + template );

		while( m.find() ) {
			String replace = m.group(1);
			String templateName = replace.toLowerCase().substring( 1, replace.length()-1 );
			String args = null;
			
			logger.finer( template );

			/*  Pre-process template */
			if( templateName.contains( "=" ) ) {
				String[] s = templateName.split( "=" );
				templateName = s[0];
				args = s[1];
			}
                        
			if( !templates.containsKey( templateName ) ) {
				throw new TemplateException( "The template " + templateName + " does not exist" );
			} else {
				String r = templates.get( templateName ).parse( action, args, ws );
				result = result.replace( replace, r );
			}
		}
		
		logger.finer( "Final template is: " + result );

		Matcher f = rx_checkFinal.matcher( result );
		if( !f.find() ) {
			throw new TemplateException( "The template is not correct: " + template );
		}

		return result;
	}    
    
}
