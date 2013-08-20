package net.praqma.hudson.scm;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class ClearCaseChangeset implements Serializable {

	public class Element implements Serializable {
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
