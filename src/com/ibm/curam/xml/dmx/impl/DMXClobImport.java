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

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.apache.commons.io.IOUtils;

/**
 * This purpose of this class is to correct a minor issue that exist with
 * extracted DMX files, due to various database version considerations or other
 * XML based anomalies.  Where a CLOB file is used to hold additional text details
 * the DMX process expects the content to the in the DMX file, however the extract
 * process currently puts the CLOB data in a separate file.  
 * 
 * This class will check a node for the use of CLOB columns and when found search the content 
 * for references to external CLOB files and replace the references with the actual content 
 * of the file within the original Node.
 * 
 * @author Simon Rodway
 */
public class DMXClobImport {

	private static final String CLOB_TEXT = "/clob/";
	private static final String VALUE = "value";
	private static final String ATTRIBUTE_TYPE_CLOB = "clob";
	private static final String ATTRIBUTE_TYPE = "type";
	private static final String COLUMN = "column";

	private String baseToClobDir;

	
	/**
	 * The initial directory parameter must to the base path to the location that contains the /clob/ directory
	 * that in turns contains the clob files to the processed.
	 * 
	 * @param initialDataDir
	 */
	public DMXClobImport(final String initialDataDir) {
		this.baseToClobDir = initialDataDir;
		
	}
	
	
	/**
	 * Check that the DMX node is first something that contains CLOB based column data.  If this 
	 * is true then process all the child nodes within the DMX node searching and replacing all 
	 * references to external CLOB files with the contain of the actual file.	
	 * 
	 * @param Node DMX File
	 * @return Node containing all the original details plus updated CLOB reference content
	 */
	public Node fixClobReferences(final Node node) {
		
		// Is the node part of a DMX file that contains CLOB references
		if (!containsClobColumn(node)) {
			return node;
		}
		
		processClobNodes(node.getChildNodes());
		
		return node;
	}


	
	/**
	 * Search for and return a Node that contains "/clob/" node value text
	 * 
	 * @param node
	 * @return
	 */
	private NodeList processClobNodes(final NodeList items) {
		for (int i = 0; items != null && i < items.getLength(); i++) {
			final Node clobNode = items.item(i);
			if (isRequiredNode(clobNode, Node.ELEMENT_NODE, VALUE)) {
				if (containsClobText(clobNode.getFirstChild())) {
					final Node child = clobNode.getFirstChild();
					final String clobFileRef = child.getNodeValue();
					final String clobFileName = clobFileRef.substring((clobFileRef.indexOf(CLOB_TEXT) + CLOB_TEXT.length()));
					final String clobFile = this.baseToClobDir + CLOB_TEXT + clobFileName;
					
					final String content = getFileContent(clobFile);
					if (content != null) {
						child.setNodeValue(content);
					}
				}
			}
			if (clobNode.hasChildNodes()) {
				processClobNodes(clobNode.getChildNodes());
			}
		}
		return items;
	}

	
	/**
	 * Read the file content from the defined CLOB file.
	 * 
	 * @param fileName
	 * @return String containing the content of the file.
	 */
	private String getFileContent(final String fileName) {
		String clobContent = null;
		FileInputStream inputStream = null;
		try {
			inputStream = new FileInputStream(fileName);
		    clobContent = IOUtils.toString(inputStream);
		} catch (FileNotFoundException e1) {
			System.out.println("Error: Unable to locate "+fileName);
			e1.printStackTrace();
		} catch (IOException e) {
			System.out.println("Error: Unable to read "+fileName);
			e.printStackTrace();
		} finally {
		    try {
				inputStream.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}	
		return clobContent;
	}
	
	
	
	
	/**
	 * Read through the Node searching for an <column> node that might have a
	 * type value of "clob" *
	 * 
	 * @param node
	 * @return true if Node contains column of type clob, false otherwise.
	 */
	private boolean containsClobColumn(final Node node) {
		boolean containsClob = false;
		
		if (node == null) 
			return containsClob;
		
		if (node.getNodeType() == Node.ELEMENT_NODE && node.getNodeName().equals(COLUMN) && node.hasAttributes()) {
			final Node attribute = node.getAttributes().getNamedItem(ATTRIBUTE_TYPE);
			if (attribute != null && attribute.getNodeValue() != null
					&& attribute.getNodeValue().equals(ATTRIBUTE_TYPE_CLOB)) {
				containsClob = true;
			}
		} else {
			NodeList list = node.getChildNodes();
			for (int i = 0; i < list.getLength() && !containsClob; i++) {
				containsClob = containsClobColumn(list.item(i));
			}
		}

		return containsClob;
	}

	
	
	/**
	 *  
	 * @param node
	 * @return true if Node contains column of type clob, false otherwise.
	 */
	private boolean containsClobText(final Node node) {
		if (node != null && node.getNodeValue() != null && node.getNodeValue().contains(CLOB_TEXT))
			return true;
				
		return false;
	}

	
	
	/**
	 * Read through the Node searching for an <column> node that might have a
	 * type value of "clob" *
	 * 
	 * @param node
	 * @return true if Node contains column of type clob, false otherwise.
	 */
	private boolean isRequiredNode(final Node node, final short nodeType, final String nodeName) {
		if (node != null && node.getNodeName().equals(nodeName)) {
			return true;
		}

		return false;
	}

}
