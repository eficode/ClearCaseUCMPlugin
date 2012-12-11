/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package net.praqma.hudson.test.integration.userstories;

import java.io.File;
import java.io.FileOutputStream;
import java.io.ObjectOutputStream;
import net.praqma.clearcase.ucm.utils.BaselineList;
import net.praqma.clearcase.ucm.utils.filters.NoDeliver;
import net.praqma.hudson.test.BaseTestClass;
import org.junit.Test;

/**
 *
 * @author Praqma
 */
public class JENKINS16072 extends BaseTestClass {
    
    /**
     * This is a small test to ensure that the BaseLine list object is serialized proberly. One thing to note is that when you design classes which
     * have associations or belong to another class, remember to ensure that ALL member classes are serializable if the object is used 
     * with Jenkins remoting. (Building on a remote slave).
     * @throws Exception 
     */
    @Test
    public void jenkins_16072() throws Exception {
        File testFile = File.createTempFile("objectSerialization", ".test");
        BaselineList list = new BaselineList().addFilter(new NoDeliver());
        ObjectOutputStream ous = new ObjectOutputStream(new FileOutputStream(testFile));
        ous.writeObject(list);
        ous.close();
        testFile.deleteOnExit();
    }
}
