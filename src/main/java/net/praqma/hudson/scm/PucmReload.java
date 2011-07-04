/**
 * 
 */
package net.praqma.hudson.scm;

import hudson.model.Run;
import hudson.model.StringParameterValue;
import hudson.model.ParameterValue;
import hudson.model.TaskListener;
import hudson.model.AbstractBuild;
import hudson.model.ParametersAction;
import hudson.model.listeners.RunListener;

import java.util.*;

/**
 * @author jes
 *
 */
public class PucmReload extends RunListener<Run>{ 

	 private ParameterValue getParameterValue(List<ParameterValue> pvs, String key) {
	        for (ParameterValue pv : pvs) {
	            if (pv.getName().equals(key)) {
	                return pv;
	            }
	        }

	        return null;
	    }
	
	public void onCompleted(Run run, TaskListener listener){
		/*if (getParameter(???, pollChild) == true ){
	     *AbstractBuild<?,?> build = (AbstractBuild<?,?>)run;
	     *
	     *PucmAction action = new PucmAction();
	     *build.getActions.add(action);
	     *}
	*/
	}
}
