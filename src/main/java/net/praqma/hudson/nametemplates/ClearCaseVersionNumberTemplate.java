package net.praqma.hudson.nametemplates;

import net.praqma.clearcase.ucm.entities.Project;
import net.praqma.hudson.exception.TemplateException;
import net.praqma.hudson.remoting.Util;
import net.praqma.hudson.scm.CCUCMState.State;

public class ClearCaseVersionNumberTemplate extends Template {

	@Override
	public String parse( State state, String args ) throws TemplateException {
		
		try {
			Project project = state.getStream().getProject();
			return Util.getClearCaseVersion( state.getWorkspace(), project );
		} catch (Exception e) {
			return "unknownccversion";
		}
	}

}
