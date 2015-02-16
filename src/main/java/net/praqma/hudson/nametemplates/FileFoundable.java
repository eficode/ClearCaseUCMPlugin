/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package net.praqma.hudson.nametemplates;

import hudson.FilePath;
import hudson.remoting.VirtualChannel;
import java.io.File;
import java.io.IOException;
import java.util.logging.Logger;

/**
 *
 * @author Mads
 */
public class FileFoundable implements FilePath.FileCallable<String> {

    public final String filename;
    	
	private static final Logger logger = Logger.getLogger( FileFoundable.class.getName() );


    public FileFoundable(final String filename) {
        logger.fine("[FileTemplate] FileFoundable created");
        this.filename = filename;
    }

    @Override
    public String invoke(File f, VirtualChannel channel) throws IOException, InterruptedException {
        FilePath path = null;
        logger.fine(String.format("In invoke. Operating on slave with workspace path: %s", f.getAbsolutePath()));
        try {            
            File selectedFile = new File(filename);
            if(selectedFile.isAbsolute()) {
                path = new FilePath(selectedFile);
            } else {
                path = new FilePath(new FilePath(f), filename);                
            }            
            String readFile = path.readToString().trim();
            logger.fine(String.format("[FileTemplate] This file was read on the slave: %s", readFile));
            
            return readFile;
        } catch (IOException ex) {
            logger.fine(String.format("[FileTemplate] Using this file on remote: %s", path.absolutize().getRemote()));
            logger.fine(String.format("[FileTemplate] Invoke caught exception with message %s", ex.getMessage()));
            throw ex;
        }
    }

}
