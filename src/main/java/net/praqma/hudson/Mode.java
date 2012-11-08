package net.praqma.hudson;

/**
 * User: cwolfgang
 * Date: 08-11-12
 * Time: 21:23
 */
public abstract class Mode {

    private CCUCMBuildAction action;


    public abstract void doit();

    public static Mode getMode( CCUCMBuildAction action ) {
        return null;
    }
}
