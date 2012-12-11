/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package net.praqma.hudson.test.integration.userstories;

import java.io.File;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
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
