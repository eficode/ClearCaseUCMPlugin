package net.praqma;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.xml.sax.SAXException;

import hudson.model.AbstractBuild;
import hudson.scm.ChangeLogParser;
import hudson.scm.ChangeLogSet;
import hudson.scm.ChangeLogSet.Entry;

public class ChangeLogParserImpl extends ChangeLogParser {
	protected static Debug logger = Debug.GetLogger();
	@Override
	public ChangeLogSet<? extends Entry> parse(AbstractBuild build, File changelogFile)
			throws IOException, SAXException {
		// TODO Auto-generated method stub
		logger.trace_function();
		List<ChangeLogEntryImpl> entries = new ArrayList<ChangeLogEntryImpl>();
		
        BufferedReader in = new BufferedReader(new FileReader(changelogFile));
        StringBuilder message = new StringBuilder();
        String s;
        
        ChangeLogEntryImpl entry = null;
        
        //TODO: Make a loop that reads the changeset from file
        
        
        entries.add(new ChangeLogEntryImpl("Msg 1"));
        entries.add(new ChangeLogEntryImpl("Msg 2"));
        entries.add(new ChangeLogEntryImpl("Msg 3"));
        
        
		return new ChangeLogSetImpl(build, entries);
	}

}
