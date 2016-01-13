package com.ibm.curam.xml.dmx;

import oracle.xml.diff.XmlUtils;
import oracle.xml.diff.Options;

import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

import javax.xml.transform.*;
import javax.xml.transform.dom.*;
import javax.xml.transform.stream.*;

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
public class DMXDifference {

	private static final String TABLE_OVERRIDE = "override";
	private static final String DATA_ROW = "row";
	private static final String TABLE_DEF = "table";

	final XmlUtils xmlUtils = new XmlUtils();
	final Options opts = new Options();
	
	private String masterFile;
	private String modifiedFile;

	public DMXDifference(final String masterFile, final String modifiedFile) {
		super();
		this.masterFile = masterFile;
		this.modifiedFile = modifiedFile;
	}

	public static void main(String[] args) {

		if (args.length < 3) {

			System.out.println("\nThe purpose of this application is to read two DMX files, a baseline or master DMX");
			System.out.println("file and one that may contain modified data rows. The application will then compare");
			System.out.println("the two files and output a version of the DMX file that is syntactically");
			System.out.println("correct, but only contains the new rows that where present in the modified");
			System.out.println("data and not in the master.");

			System.out.println("\n\nInvalid parameters passed.");
			System.out.println("\t usage: DMXDifference DMXFileName MasterDir UpdateDir.\n\n");
			System.exit(1);
		}

		final String masterFileName = args[0];
		final String baselineDir = args[1];
		final String updateDir = args[2];

		try {
			DMXDifference diff = new DMXDifference(baselineDir + "/" + masterFileName,
					updateDir + "/" + masterFileName);
			System.out.println(getStringFromNode(diff.getModifiedData()));
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * Returns a Node that contains the rows from the DMX files that are in the
	 * modified file, but not present in the master file.
	 * 
	 * @return Node containing data rows
	 */
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

	/**
	 * The method is responsible for processing the Nodes and returning a new updated Node that
	 * contains the delta of the two Nodes passed in.
	 * 
	 * @param Node master - the baseline Node containing the details that will be seeded on initial install
	 * @param Node modified - updated Node containing baseline details plus any additional information added
	 * @return Node delta - Node containing the baseline table details, plus any additional rows not present in the baseline.
	 */
	private Node dmxDataDiff(final Node master, final Node modified) {
		final Node updated = master.cloneNode(true);

		try {

			final Document diffAsDom = xmlUtils.diffToDoc(master, modified, opts);

			final List<Node> masterRows = new ArrayList<Node>();
			getRows(master, masterRows);

			removeAll(updated, Node.ELEMENT_NODE, DATA_ROW);
			appendAll(masterRows, updated.getLastChild(), diffAsDom, Node.ELEMENT_NODE, DATA_ROW);
			setTableOverride(updated);
		} catch (Exception e) {
			e.printStackTrace();
		}

		return updated;
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
		if (node.getNodeType() == nodeType && (name == null || node.getNodeName().equals(name))) {
			node.getParentNode().removeChild(node);
		} else {
			NodeList list = node.getChildNodes();
			for (int i = 0; i < list.getLength(); i++) {
				removeAll(list.item(i), nodeType, name);
			}
		}
	}

	/**
	 * Method that builds the delta details, by checking the node rows against the rows in the supplied List.
	 * Only when the row doesn't exist in the List is it added to the master.
	 * 
	 * @param rows : list of rows that where present in the baseline
	 * @param master : new delta node that will contain rows when they're not present in the List rows.
	 * @param node : node that contains data to compare with List.  This is typically a node that only contains information different
	 * to the original baseline.  
	 * @param nodeType : the node type that is being compared
	 * @param name : name of the node that will be compared, e.g. <row>
	 */
	private void appendAll(List<Node> rows, Node master, Node node, short nodeType, String name) {
		if (node.getNodeType() == nodeType && (name == null || node.getNodeName().equals(name))) {
			final Node newNode = node.cloneNode(true);

			final XmlUtils xmlUtils = new XmlUtils();
			boolean exists=false;
			
			for (Node n : rows) {
				try {
					if (xmlUtils.equal(n, newNode, opts)) {
						exists=true;
					}
				} catch (Exception e) {
					e.printStackTrace();
				}
				if (exists) break;
			}
			
			if (!exists) {
				master.getOwnerDocument().adoptNode(newNode);
				master.appendChild(newNode);
			}
		} else {
			NodeList list = node.getChildNodes();
			for (int i = 0; i < list.getLength(); i++) {
				appendAll(rows, master, list.item(i), nodeType, name);
			}
		}
	}

	/**
	 * Get the list of <row> elements from the provided Node.
	 * 
	 * @return ArrayList of Nodes
	 */
	private List<Node> getRows(final Node master, final List<Node> rows) {
		if (master.getNodeType() == Node.ELEMENT_NODE && (master == null || master.getNodeName().equals(DATA_ROW))) {
			rows.add(master.cloneNode(true));
		} else {
			NodeList list = master.getChildNodes();
			for (int i = 0; i < list.getLength(); i++) {
				getRows(list.item(i), rows);
			}
		}

		return rows;
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
	 * Method to convert Node to String, which can be used to validation or
	 * output purposes.
	 * 
	 * @param node
	 * @return String contains the representation on the source Document
	 */
	public static String getStringFromNode(Node node) {

		try {
			final StringWriter writer = new StringWriter();
			final Transformer transformer = TransformerFactory.newInstance().newTransformer();
			transformer.setOutputProperty(OutputKeys.INDENT, "yes");
			transformer.transform(new DOMSource(node), new StreamResult(writer));
			return writer.toString();
		} catch (TransformerException ex) {
			ex.printStackTrace();
			return null;
		}
	}

	/**
	 * set Override attribute for table element in the node. This is because the
	 * processor doesn't currently handle table/column changes so we need to
	 * just work with standard data row details and ignore table details.
	 * 
	 * @param node : node that will be searched for the presence on a table override setting. When found this will be reset to TRUE.
	 */
	private void setTableOverride(Node node) {
		if (node.getNodeType() == Node.ELEMENT_NODE && node.getNodeName().equals(TABLE_DEF)) {
			((Element) node).setAttribute(TABLE_OVERRIDE, "true");
		} else {
			NodeList list = node.getChildNodes();
			for (int i = 0; i < list.getLength(); i++) {
				setTableOverride(list.item(i));
			}
		}
	}
}