package net.praqma;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.StringReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.digester.Digester;
import org.xml.sax.SAXException;

import hudson.model.AbstractBuild;
import hudson.scm.ChangeLogParser;
import hudson.scm.ChangeLogSet;
import hudson.scm.ChangeLogSet.Entry;
import hudson.util.Digester2;

public class ChangeLogParserImpl extends ChangeLogParser {
	
	protected static Debug logger = Debug.GetLogger();
	
	@Override
	public ChangeLogSet<? extends Entry> parse(AbstractBuild build, File changelogFile)
			throws IOException, SAXException {
		logger.trace_function();
		logger.log("build: "+build.toString()+" changelogFile: "+changelogFile.toString());
		
		List<ChangeLogEntryImpl> entries = new ArrayList<ChangeLogEntryImpl>();
		
		//Source: http://wiki.hudson-ci.org/display/HUDSON/Change+log
        
		Digester digester = new Digester2();
		digester.push(entries);
		digester.addObjectCreate("*/changeset", ChangeLogEntryImpl.class);
		digester.addSetProperties("*/changeset");
		digester.addBeanPropertySetter("*/changeset/comment");
		digester.addBeanPropertySetter("*/changeset/filepath");
		digester.addSetNext("*/changeset","add");

		StringReader reader = new StringReader("<changelog><changeset version=\"1212\">" +
				"<filepath>this is the 1st filepath</filepath><comment>comment to 1st filepath</comment>" +
				"</changeset><changeset version=\"2424\">" +
				"<filepath>this is the 2nd filepath</filepath><comment>comment to 2nd filepath</comment>" +
				"</changeset></changelog>");
		digester.parse(reader);
		reader.close();
		
		System.out.println("LISTE: "+entries.size());
		
		return new ChangeLogSetImpl(build, entries);
	}

}
