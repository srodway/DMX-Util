/*
 * IBM Confidential
 *
 * OCO Source Materials
 *
 * Copyright IBM Corporation 2016
 *
 * The source code for this program is not published or otherwise divested
 * of its trade secrets, irrespective of what has been deposited with the US
 * Copyright Office
 */
package com.ibm.curam.utils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

import org.apache.commons.io.FileUtils;

public class FileUtil {

	public final static String PATH_MARKER="/";
	
	public static void copyFiles(List <String> sourceFiles, String destDir) {
		
		try {
			Files.createDirectories(Paths.get(destDir));
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		for(String file : sourceFiles) {
			copyFile(file, destFile(file, destDir));
		}
	}
	
	public static void copyFile(String sourceFile, String destFile) {
		File source = new File(sourceFile);
		File dest = new File(destFile);
		
		try {
		    FileUtils.copyFile(source, dest);
		} catch (IOException e) {
		    e.printStackTrace();
		}
	}

	public static String destFile(String sourceFile, String destDir) {
		final String fileRef = sourceFile;
		final String fileName = fileRef.substring((fileRef.lastIndexOf(PATH_MARKER) + PATH_MARKER.length()));
		final String file = destDir + fileName;
		
		return file;
	}	
}
