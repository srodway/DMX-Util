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
package com.ibm.curam.xml.dmx.impl;

import java.io.File;
import java.io.IOException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Node;

public class DMXFile {
	
	private DMXFile() {		
	}
	
	
	/**
	 * 
	 * @param fileName
	 * @return
	 * @throws Exception
	 */
	public static Node getNode(String fileName) throws IOException {
		// Parse the two input files
		final DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
		dbFactory.setIgnoringElementContentWhitespace(true);
		dbFactory.setNamespaceAware(true);
		Node doc = null;

		try {
			final DocumentBuilder docBuilder = dbFactory.newDocumentBuilder();
			doc = docBuilder.parse(new File(fileName));
		} catch (java.io.FileNotFoundException fne) {
			doc = null;
		} catch (Exception e) {
			e.printStackTrace();
		}
		return doc;

	}

}
