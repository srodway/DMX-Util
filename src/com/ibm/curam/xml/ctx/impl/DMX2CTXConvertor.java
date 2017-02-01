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
package com.ibm.curam.xml.ctx.impl;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.xml.namespace.QName;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactoryConfigurationError;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Task;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import curam.util.tools.datadictionary.DomUtils;
//import curam.util.resources.Trace;

/**
 * Converts extracted codetable dmx files into ctx files.
 * 
 * @since 6.0.5
 */
public class DMX2CTXConvertor extends Task {

	private static final String CODETABLE_FILE_SUFFIX = ".ctx";

	private static final String CODETABLE_FILE_PREFIX = "CT_";

	/**
	 * The name of the CTDISPLAYNAME dmx file.
	 */
	private static final String CTDISPLAYNAME_DMX = "CTDISPLAYNAME.dmx";

	/**
	 * The name of the CODETABLEITEM dmx file.
	 */
	private static final String CODETABLEITEM_DMX = "CODETABLEITEM.dmx";

	/**
	 * The name of the CODETABLEHEADER dmx file.
	 */
	private static final String CODETABLEHEADER_DMX = "CODETABLEHEADER.dmx";

	private String dmxDir;

	private String outputDir;

	/**
	 * The XPath instance used by the class.
	 */
	private final javax.xml.xpath.XPath xPath;

	/**
	 * Default constructor.
	 */
	public DMX2CTXConvertor() {
		xPath = javax.xml.xpath.XPathFactory.newInstance().newXPath();
	}

	/**
	 * Main function.
	 * 
	 * @param args
	 *            An array of arguments in the following order: <path to dmx
	 *            directory>, <path to ctx directory>
	 */
	public static void main(final String[] args) {
		final DMX2CTXConvertor dmx2ctxConvertor = new DMX2CTXConvertor();
		dmx2ctxConvertor.setDmxDir(args[0]);
		dmx2ctxConvertor.setOutputDir(args[1]);
		dmx2ctxConvertor.execute();
	}

	/**
	 * Execute the conversion.
	 */
	@Override
	public void execute() {

		// Trace.kToolsLogger.info("=== Start DMX 2 CTX Conversion ===");

		boolean ctxProcessAllowed=true;
		
		try {
			validateParams();
		} catch (BuildException be) {
			System.out.println(be.getMessage());
			
			//TODO: Probably because their are no CTX differences.  Need to handle this possibility
			ctxProcessAllowed=false;
		}

		if (!ctxProcessAllowed) {
			return;
		}
		
		final File ctHeaderDMXFile = new File(getDmxDir() + File.separator + CODETABLEHEADER_DMX);
		final File ctItemDMXFile = new File(getDmxDir() + File.separator + CODETABLEITEM_DMX);
		final File ctDisplayNameDMXFile = new File(getDmxDir() + File.separator + CTDISPLAYNAME_DMX);

		// Load the codetable dmx documents
		final Document ctHeaderDMXDoc = parseDMXFile(ctHeaderDMXFile);
		final Document ctItemDMXDoc = parseDMXFile(ctItemDMXFile);
		final Document ctDisplayNameDMXDoc = parseDMXFile(ctDisplayNameDMXFile);

		// Clear the ctx directory, if it exists
		final File ctxDir = new File(getOutputDir());
		ctxDir.mkdirs();

		for (final Element headerRowElement : getRows(ctHeaderDMXDoc)) {
			final String tableName = getAttributeValue(headerRowElement, "tableName");
			String codetablePath = ctxDir.getAbsoluteFile() + File.separator + CODETABLE_FILE_PREFIX + tableName + CODETABLE_FILE_SUFFIX;
			// Trace.kToolsLogger.info("Creating ctx file [" + codetablePath +
			// "]");

			final Document ctxDocument = DomUtils.createNewDocument();
			final Element codetables = ctxDocument.createElement("codetables");
			ctxDocument.appendChild(codetables);
			codetables.setAttribute("package", "curam.codetable");

			final Element codetable = ctxDocument.createElement("codetable");
			codetables.appendChild(codetable);
			codetable.setAttribute("name", tableName);
			codetable.setAttribute("java_identifier", tableName.toUpperCase());

			// Add the display name details
			addDisplayNames(ctxDocument, codetable, tableName, ctDisplayNameDMXDoc);

			// Add the code table items
			addCodeElements(ctxDocument, codetable, tableName, ctItemDMXDoc);

			// Write the CTX file to disk
			final File ctxfile = new File(codetablePath);
			writeXMLFile(ctxfile, ctxDocument);
		}

		// Trace.kToolsLogger.info("=== End DMX 2 CTX Conversion ===");
	}

	/**
	 * Adds the code elements to the code table ctx document.
	 * 
	 * @param ctxDocument
	 *            The document to add the codes to.
	 * @param codetable
	 *            The codetable element to add the codes to.
	 * @param codeTableName
	 *            The name of the codetable;
	 * @param ctItemDMXDoc
	 *            The code table item dmx file to parse.
	 */
	private void addCodeElements(final Document ctxDocument, final Element codetable, final String codeTableName,
			final Document ctItemDMXDoc) {
		// Create a set to track the processed code values
		final Set<String> processedCodes = new HashSet<String>();

		// Loop through the list of codetable items
		final List<Element> ctItemRows = listRowsByAttributeValue(ctItemDMXDoc, "tableName", codeTableName);
		for (final Element ctItemRow : ctItemRows) {
			final String codeValue = getAttributeValue(ctItemRow, "code");
			/*
			 * Check if the code value has already been processed, this can
			 * happen if there are multiple locale descriptions for a code. In
			 * this case we need to add to the existing code.
			 */
			Element code;
			if (processedCodes.contains(codeValue)) {
				code = (Element) evaluateXPath(codetable, "code[@value= \"" + codeValue + "\"]", XPathConstants.NODE);
			} else {
				// Create a new code element and set the attributes
				code = ctxDocument.createElement("code");
				codetable.appendChild(code);
				final String isEnabled = getAttributeValue(ctItemRow, "isEnabled");
				final String statusValue = "1".equals(isEnabled) ? "ENABLED" : "DISABLED";
				code.setAttribute("status", statusValue);
				code.setAttribute("value", codeValue);
			}

			// Add the locale element to the code
			final Element locale = ctxDocument.createElement("locale");
			code.appendChild(locale);

			final String localeIdentifier = getAttributeValue(ctItemRow, "localeIdentifier");
			final String[] localeValues = localeIdentifier.split("_");
			locale.setAttribute("language", localeValues[0]);
			if (localeValues.length > 1) {
				locale.setAttribute("country", localeValues[1]);
			}
			locale.setAttribute("sort_order", getAttributeValue(ctItemRow, "sortOrder"));

			// Add the description to the locale
			final Element description = ctxDocument.createElement("description");
			locale.appendChild(description);
			description.setTextContent(getAttributeValue(ctItemRow, "description"));

			// Add the annotation
			final String annotationValue = getAttributeValue(ctItemRow, "annotation");
			if (annotationValue != null && !"".equals(annotationValue)) {
				final Element annotation = ctxDocument.createElement("annotation");
				locale.appendChild(annotation);
				annotation.setTextContent(annotationValue);
			}

			// Add the comments
			final String commentsValue = getAttributeValue(ctItemRow, "comments");
			if (commentsValue != null && !"".equals(commentsValue)) {
				final Element comments = ctxDocument.createElement("comments");
				locale.appendChild(comments);
				comments.setTextContent(commentsValue);
			}

			// Add the code to the set of processed items
			processedCodes.add(codeValue);
		}
	}

	/**
	 * Adds the 'displaynames' element and child 'locale' elements to a
	 * codetable.
	 * 
	 * @param ctxDocument
	 *            The parent ctx document being created.
	 * @param codetable
	 *            The codetable element to add the display names to.
	 * @param codeTableName
	 *            The name of the codetable being generated.
	 * @param ctDisplayNameDMXDoc
	 *            The display names dmx document to retrieve the display names
	 *            from.
	 * @return The display names element, or <code>null</code> if no display
	 *         names were found in the provided dmx document.
	 */
	private void addDisplayNames(final Document ctxDocument, final Element codetable, final String codeTableName,
			final Document ctDisplayNameDMXDoc) {

		if (ctDisplayNameDMXDoc == null) {
			return;
		}

		final List<Element> displayNameRows = listRowsByAttributeValue(ctDisplayNameDMXDoc, "tableName", codeTableName);

		if (displayNameRows.size() > 0) {
			final Element displaynames = ctxDocument.createElement("displaynames");
			codetable.appendChild(displaynames);
			for (final Element displayNameRow : displayNameRows) {
				final Element locale = ctxDocument.createElement("locale");
				displaynames.appendChild(locale);
				final String localeIdentifier = getAttributeValue(displayNameRow, "localeIdentifier");
				final String[] localeValues = localeIdentifier.split("_");
				final String text = getAttributeValue(displayNameRow, "text");
				locale.setAttribute("language", localeValues[0]);
				if (localeValues.length > 1) {
					locale.setAttribute("country", localeValues[1]);
				}
				locale.setTextContent(text);
			}
		}
	}

	/**
	 * Retrieves a list of 'row' elements from the dmx document.
	 * 
	 * A list of row elements, or an empty list if none are found.
	 */
	private List<Element> getRows(final Document dmxDoc) {
		final Element tableElement = dmxDoc.getDocumentElement();
		return DomUtils.getChildrenByName(tableElement, "row");
	}

	/**
	 * Gets the value of a column from the specified row.
	 * 
	 * @param row
	 *            The row element.
	 * @param attributeName
	 *            The name of the child attribute to retrieve the value of.
	 * @return The text value of the column.
	 */
	private String getAttributeValue(final Element row, final String attributeName) {
		return (String) evaluateXPath(row, "attribute[@name= \"" + attributeName + "\"]/value/text()",
				XPathConstants.STRING);
	}

	private List<Element> listRowsByAttributeValue(final Document doc, final String attributeName,
			final String attributeValue) {
		final List<Element> rows = new ArrayList<Element>();

		final NodeList nodeList = (NodeList) evaluateXPath(doc,
				"//row[attribute[@name= \"" + attributeName + "\"]/value = \"" + attributeValue + "\"]",
				XPathConstants.NODESET);
		for (int i = 0; i < nodeList.getLength(); i++) {
			rows.add((Element) nodeList.item(i));
		}
		return rows;
	}

	/**
	 * Validate that all parameters have been specified correctly.
	 */
	private void validateParams() throws BuildException {
		final File dmxDir = new File(getDmxDir());
		if (!dmxDir.exists()) {
			throw new BuildException("The dmx directory '" + dmxDir + "' doesn't exist.");
		}

		final File ctHeaderDMX = new File(getDmxDir() + File.separator + CODETABLEHEADER_DMX);
		if (!ctHeaderDMX.exists()) {
			throw new BuildException(
		   			"The code table header dmx file '" + ctHeaderDMX.getAbsolutePath() + "' doesn't exist.");
		}

		final File ctItemDMX = new File(getDmxDir() + File.separator + CODETABLEITEM_DMX);
		if (!ctItemDMX.exists()) {
			throw new BuildException(
					"The code table item dmx file '" + ctItemDMX.getAbsolutePath() + "' doesn't exist.");
		}
	}

	/**
	 * Retrieves the specified dmx file as a document.
	 * 
	 * @param dmxFile
	 *            The dmx file to parse.
	 * @return The dmx file as a document, or null if the file doesn't exist.
	 */
	private Document parseDMXFile(final File dmxFile) {
		// Check if the file exists
		if (!dmxFile.exists()) {
			return null;
		}

		// Parse the dmx file into a dom document
		try {
			return DomUtils.parseXml(new FileInputStream(dmxFile));
		} catch (final FileNotFoundException e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * Evaluates an XPath Query on the item provided.
	 * 
	 * @param item
	 *            The item to evaluate the query on.
	 * @param query
	 *            The x path query to execute.
	 * @param returnType
	 *            The return type of the expression.
	 * @return The return object of the x path expression.
	 */
	private Object evaluateXPath(final Object item, final String query, final QName returnType) {
		try {
			return xPath.compile(query).evaluate(item, returnType);
		} catch (final XPathExpressionException e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * Writes an xml document out to a file.
	 * 
	 * @param outputFile
	 *            The output file to write the document to.
	 * @param document
	 *            The document contents to output.
	 * @throws BuildException
	 *             If an exception occurs writing the document.
	 */
	private void writeXMLFile(final File outputFile, final Document document) {
		try {
			// Prepare the DOM document for writing
			final javax.xml.transform.Source source = new javax.xml.transform.dom.DOMSource(document);

			// Prepare the output file
			final javax.xml.transform.Result result = new javax.xml.transform.stream.StreamResult(outputFile);

			// Write the DOM document to the file
			final javax.xml.transform.Transformer xformer = javax.xml.transform.TransformerFactory.newInstance()
					.newTransformer();
			xformer.setOutputProperty(javax.xml.transform.OutputKeys.METHOD, "xml");
			xformer.setOutputProperty(javax.xml.transform.OutputKeys.OMIT_XML_DECLARATION, "no");
			xformer.setOutputProperty(javax.xml.transform.OutputKeys.VERSION, "1.0");
			xformer.setOutputProperty(javax.xml.transform.OutputKeys.ENCODING, "UTF-8");
			xformer.setOutputProperty(javax.xml.transform.OutputKeys.INDENT, "yes");
			xformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");
			xformer.transform(source, result);
		} catch (final TransformerConfigurationException e) {
			throw new RuntimeException(e);
		} catch (final TransformerFactoryConfigurationError e) {
			throw new RuntimeException(e);
		} catch (final TransformerException e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * Gets the dmxDir field value.
	 * 
	 * @return The dmxDir field.
	 */
	public String getDmxDir() {
		return dmxDir;
	}

	/**
	 * Sets the dmxDir field value.
	 * 
	 * @param dmxDir
	 *            The dmxDir to set.
	 */
	public void setDmxDir(final String dmxDir) {
		this.dmxDir = dmxDir;
	}

	/**
	 * Gets the outputDir field value.
	 * 
	 * @return The outputDir field.
	 */
	public String getOutputDir() {
		return outputDir;
	}

	/**
	 * Sets the outputDir field value.
	 * 
	 * @param outputDir
	 *            The outputDir to set.
	 */
	public void setOutputDir(final String outputDir) {
		this.outputDir = outputDir;
	}

}
