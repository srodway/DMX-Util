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
import java.io.StringWriter;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.Node;

public class DMXFile {
	
	private static final Logger logger = Logger.getLogger("dmxtool");
	
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
	
	
	/**
	 * Test method use to provide a string representation of the Node.
	 * 
	 * @param doc
	 * @return
	 */
	public static final String getNodeAsText(Node doc) {
		final StringWriter sw = new StringWriter();
		
		try {
			final TransformerFactory transfac = TransformerFactory.newInstance();
			
			final Transformer trans = transfac.newTransformer();
		
			trans.setOutputProperty(OutputKeys.METHOD, "xml");
			trans.setOutputProperty(OutputKeys.INDENT, "yes");
			trans.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", Integer.toString(2));
	
			final StreamResult result = new StreamResult(sw);
			final DOMSource source = new DOMSource(doc);
			
			trans.transform(source, result);
			
			logger.log(Level.FINEST, () -> "getNodeAsText\n\n" + sw.toString());
		} catch (TransformerException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return sw.toString();
	}

}
