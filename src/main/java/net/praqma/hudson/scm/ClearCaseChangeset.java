package net.praqma.hudson.scm;

import java.util.ArrayList;
import java.util.List;

public class ClearCaseChangeset {

	private static final long serialVersionUID = -641415231946264343L;
	
	public class Element {
		private String version;
		private String user;
		
		public Element( String version, String user ) {
			this.version = version;
			this.user = user;
		}

		public String getVersion() {
			return version;
		}

		public String getUser() {
			return user;
		}
	}
	
	private List<Element> changeset = new ArrayList<Element>();
	
	public List<Element> getList() {
		return changeset;
	}
	
	public void addChange( String version, String user ) {
		changeset.add( new Element( version, user ) );
	}
	
}
