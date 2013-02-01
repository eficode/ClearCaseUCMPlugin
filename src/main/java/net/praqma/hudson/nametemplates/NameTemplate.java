package net.praqma.hudson.nametemplates;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.praqma.hudson.CCUCMBuildAction;
import net.praqma.hudson.exception.TemplateException;

public class NameTemplate {

	private static Map<String, Template> templates = new HashMap<String, Template>();
	private static Logger logger = Logger.getLogger( NameTemplate.class.getName() );

	static {
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

	public static void validateTemplates( CCUCMBuildAction action ) {
		logger.finer( "Validating templates for " + action );
		Set<String> keys = templates.keySet();
		for( String key : keys ) {
			String r;
			try {
				logger.finer( "Validating " + key );
				r = templates.get( key ).parse( action, "" );
			} catch (TemplateException e) {
				logger.warning( "Could not validate " + key );
			}
		}
	}

	public static String trim( String template ) {
        if( template.matches( "^\".+\"$" ) ) {
        	template = template.substring( 1, template.length()-1 );
        }

        return template;
	}

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

	public static String parseTemplate( String template, CCUCMBuildAction action ) throws TemplateException {
		logger.finer( "Parsing template for " + action );
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
			
			logger.finer( "--->" + templateName + ": " + args );

			if( !templates.containsKey( templateName ) ) {
				throw new TemplateException( "The template " + templateName + " does not exist" );
			} else {
				String r = templates.get( templateName ).parse( action, args );
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
