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

import oracle.xml.diff.XmlUtils;
import oracle.xml.diff.Options;

import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.Text;

import com.ibm.curam.xml.dmx.intf.DMXDifference;

import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;

import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * This purpose of this class is to read two DMX files, a baseline or master DMX
 * file and one that may contain modified data rows. The class will then compare
 * the two files and output a version of the DMX file that is syntactically
 * correct, but only contains the new rows that where present in the modified
 * data and not in the master.
 * 
 * 
 * VERSION 2: Create an collection of <rows> from the two files being compared.
 * Then test if the row in the compared file is present in the master file. Only
 * entries that are not present should be added to the delta demo file.
 * 
 * @author Simon Rodway
 */
public class DMXDifferenceImpl implements DMXDifference {

	final XmlUtils xmlUtils = new XmlUtils();
	final Options opts = new Options();

	private String masterFile;
	private String modifiedFile;
	private List<String> ignoreAttributes;	

	public DMXDifferenceImpl(final String masterFile, final String modifiedFile) {
		super();
		this.masterFile = masterFile;
		this.modifiedFile = modifiedFile;
	}


	/**
	 * Returns a Node that contains the rows from the DMX files that are in the
	 * modified file, but not present in the master file.
	 * 
	 * @return Node containing data rows
	 */
	@Override
	public Node getModifiedData() {

		Node master = null;
		Node modified = null;
		Node diffNode = null;

		try {
			master = DMXFile.getNode(masterFile);
			modified = DMXFile.getNode(modifiedFile);
		} catch (IOException ioe) {
			// This is because either the master file or the modified file are
			// not present
		}

		if (master != null && modified != null) {
			diffNode = dmxDataDiff(master, modified);
		} else if (master == null && modified != null) {
			diffNode = modified;
		}

		cleanNode(diffNode);

		return diffNode;
	}

	
	
	@Override
	public void setIgnoreAttributes(final List<String> attributes) {
		if (ignoreAttributes == null) {
			ignoreAttributes = new ArrayList<String>(attributes.size()); 
		}
		for (String s : attributes) {
			ignoreAttributes.add(s.toLowerCase());
		}
	}
	
	
	/**
	 * The method is responsible for processing the Nodes and returning a new
	 * updated Node that contains the delta of the two Nodes passed in.
	 * 
	 * @param Node
	 *            master - the baseline Node containing the details that will be
	 *            seeded on initial install
	 * @param Node
	 *            modified - updated Node containing baseline details plus any
	 *            additional information added
	 * @return Node delta - Node containing the baseline table details, plus any
	 *         additional rows not present in the baseline.
	 */
	private Node dmxDataDiff(final Node master, final Node modified) {
		final Node updated = master.cloneNode(true);

		final Map<Node, Node> masterRows = new HashMap<Node, Node>();
		getRows(master, masterRows);
		
		final Map<Node, Node> altRows = new HashMap<Node, Node>();
		getRows(modified, altRows);
		
		removeAll(updated, Node.ELEMENT_NODE, DATA_ROW);
		
		produceUpdatedRows(masterRows, altRows, updated.getLastChild());
		
		setTableOverride(updated);

		return updated;
	}

	
	/**
	 * The method replaces all the occurrences of 'lastWritten' with a default value.
	 * 
	 * @param node
	 * @param defaultValue
	 */
	private void setIgnoreAttributes(final Node node) {
		if (ignoreAttributes == null || ignoreAttributes.size() < 1) {
			return;
		}

		setIgnoreAttributes(node, ignoreAttributes, "IGNORE");
	}
	
	/**
	 * The method replaces all the occurrences of attributes with 'valueName' with a default value.
	 * 
	 * @param node
	 * @param valueName
	 * @param defaultValue
	 */
	private void setIgnoreAttributes(final Node node, final List<String> valueName, final String defaultValue) {
		final String nodeName = node.getNodeName();
		
		if ("attribute".equals(nodeName)) {
			if (node.hasAttributes()) {
				final NamedNodeMap attrib = node.getAttributes();
				final Node attribName = attrib.getNamedItem("name");
				
				if (attribName != null && valueName.contains(attribName.getNodeValue().toLowerCase())) {
					if (node.hasChildNodes()) {
						NodeList nodes = node.getChildNodes();
						
						for (int i = 0; i < nodes.getLength(); i++) {
							Node valueNode = nodes.item(i);

							if ("value".equals(valueNode.getNodeName())) {
								
								Element newValueNode = node.getOwnerDocument().createElement("value");
						        Text nametextNode = node.getOwnerDocument().createTextNode(defaultValue);
						        newValueNode.appendChild(nametextNode);
								
								node.replaceChild(newValueNode, valueNode);
							}
						}
					}
				}
			}
		} else {
			NodeList list = node.getChildNodes();
			for (int i = 0; i < list.getLength(); i++) {
				setIgnoreAttributes(list.item(i), valueName, defaultValue);
			}
		}
	}	
	
	/**
	 * Remove all the childName data elements from the source document, when
	 * contained within a parentName element
	 * 
	 * @param node
	 * @param nodeType
	 * @param name
	 */
	private void removeAll(final Node node, final short nodeType, final String name) {
		if (name == null || node.getNodeName().equals(name)) {
			node.getParentNode().removeChild(node);
		} else {
			NodeList list = node.getChildNodes();
			for (int i = 0; i < list.getLength(); i++) {
				removeAll(list.item(i), nodeType, name);
			}
		}
	}

	
	
	/**
	 * Method that builds the delta details, by checking the node rows against
	 * the rows in the supplied List. Only when the row doesn't exist in the
	 * List is it added to the master.
	 * 
	 * @param rows
	 *            : list of rows that where present in the baseline
	 * @param master
	 *            : new delta node that will contain rows when they're not
	 *            present in the List rows.
	 * @param node
	 *            : node that contains data to compare with List. This is
	 *            typically a node that only contains information different to
	 *            the original baseline.
	 * @param nodeType
	 *            : the node type that is being compared
	 * @param name
	 *            : name of the node that will be compared, e.g. <row>
	 */
	private void produceUpdatedRows(Map<Node, Node> masterRows, Map<Node,Node>altRows, Node updated) {
		final XmlUtils xmlUtils = new XmlUtils();
		for (Node altKey : altRows.keySet()) {
			boolean exists = false;
			
			for (Node n : masterRows.keySet()) {
				try {
					if (xmlUtils.equal(n, altKey, opts)) {
						exists = true;
					}
				} catch (Exception e) {
					e.printStackTrace();
				}
				if (exists)
					break;
			}	
			
			if (!exists) {
				final Node altRecord = altRows.get(altKey);
				updated.getOwnerDocument().adoptNode(altRecord);
				updated.appendChild(altRecord);
			}
		}
	}
	
	

	/**
	 * Get the list of <row> elements from the provided Node.
	 * 
	 * @return ArrayList of Nodes
	 */
	private Map<Node, Node> getRows(final Node master, Map<Node,Node> rows) {
		final String name = master.getNodeName();
		
		if (DATA_ROW.equals(name)) {
			final Node keyNode = master.cloneNode(true);
			setIgnoreAttributes(keyNode);
			setIgnoreLOBPaths(keyNode);
			
			rows.put(keyNode, master.cloneNode(true));
		} else {
			NodeList list = master.getChildNodes();
			for (int i = 0; i < list.getLength(); i++) {
				getRows(list.item(i), rows);
			}
		}

		return rows;
	}

	
	private void setIgnoreLOBPaths(Node node) {
		// default any value references to /clob or /blob
		// because extract data renumbers these when new records added.
		
		final String nodeName = node.getNodeName();
		
		if ("value".equals(nodeName)) {
			Node value = node.getFirstChild();
			
			if (value != null && (value.getNodeValue().contains("/clob") || value.getNodeValue().contains("/blob"))) {
				value.setNodeValue("CHANGED");
			}
		} else {
			NodeList list = node.getChildNodes();
			for (int i = 0; i < list.getLength(); i++) {
				setIgnoreLOBPaths(list.item(i));
			}
		}	
	}
	
	
	private void cleanNode(final Node node) {

		if (node == null)
			return;

		final NodeList childNodes = node.getChildNodes();

		for (int n = childNodes.getLength() - 1; n >= 0; n--) {
			Node child = childNodes.item(n);
			short nodeType = child.getNodeType();

			if (nodeType == Node.ELEMENT_NODE)
				cleanNode(child);
			else if (nodeType == Node.TEXT_NODE) {
				String trimmedNodeVal = child.getNodeValue().trim();
				if (trimmedNodeVal.length() == 0)
					node.removeChild(child);
				else
					child.setNodeValue(trimmedNodeVal);
			} else if (nodeType == Node.COMMENT_NODE)
				node.removeChild(child);
		}
	}


	/**
	 * set Override attribute for table element in the node. This is because the
	 * processor doesn't currently handle table/column changes so we need to
	 * just work with standard data row details and ignore table details.
	 * 
	 * @param node
	 *            : node that will be searched for the presence on a table
	 *            override setting. When found this will be reset to TRUE.
	 */
	private void setTableOverride(Node node) {
		if (node.getNodeType() == Node.ELEMENT_NODE && node.getNodeName().equals(TABLE_DEF)) {
			((Element) node).setAttribute(TABLE_OVERRIDE, DEFAULT_OVERRIDE_STATE);
		} else {
			NodeList list = node.getChildNodes();
			for (int i = 0; i < list.getLength(); i++) {
				setTableOverride(list.item(i));
			}
		}
	}

}