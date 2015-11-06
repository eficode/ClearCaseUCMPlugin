package net.praqma.hudson.scm.pollingmode;

/**
 * Marker interface indicating if this mode should use the newest baseline among all candidates always. 
 *
 * @author Mads
 */
public interface NewestFeatureToggle {
    boolean isNewest();
}
