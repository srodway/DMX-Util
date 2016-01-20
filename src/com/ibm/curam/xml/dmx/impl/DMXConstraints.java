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
import java.util.HashMap;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;

import javax.xml.namespace.QName;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

//import curam.datadictionary.DomUtils;

/**
 * Helper class that wraps a constraints xml document to provide functions for
 * retrieving constraint details about database tables.
 * 
 * @since 6.0.5
 */

public abstract class DMXConstraints {

	/**
	 * Map of entity names to primary key fields that are to be extracted.
	 */
	private Map<String, SortedSet<String>> tableKeyMap;

	/**
	 * The XPathFactory instance used by the class.
	 */
	private javax.xml.xpath.XPathFactory xPathFactory;

	private Document constraintsDoc;

	/**
	 * Constructor to the class.
	 * 
	 * @param constraintsXmlFile
	 *            The constraints xml file to parse.
	 */
	public DMXConstraints(final File constraintsXmlFile) {
		tableKeyMap = new HashMap<String, SortedSet<String>>();
		xPathFactory = javax.xml.xpath.XPathFactory.newInstance();
		// constraintsDoc = DomUtils.parseXml(constraintsXmlFile);
	}

	/**
	 * Retrieves a list containing the names of the primary key columns for an
	 * entity.
	 * 
	 * @param entity
	 *            The name of the entity to get the primary key columns of.
	 * @return A list of primary key columns.
	 */
	public SortedSet<String> getPrimaryKeyColumns(final String entity) {

		// Check if we haven't retrieved the key set already
		if (tableKeyMap.containsKey(entity)) {
			return tableKeyMap.get(entity);
		}

		final SortedSet<String> columns = new TreeSet<String>();

		final StringBuilder queryBuilder = new StringBuilder();
		queryBuilder.append("//primarykeys/name[@tablename = '").append(entity).append("']/attribute");

		final NodeList columnList = (NodeList) evaluateXPath(constraintsDoc, queryBuilder.toString(),
				XPathConstants.NODESET);
		for (int i = 0; i < columnList.getLength(); i++) {
			final Element attribute = (Element) columnList.item(i);
			columns.add(attribute.getAttribute("keyname"));
		}

		// Add the columns to the map
		tableKeyMap.put(entity, columns);

		return columns;
	}

	/**
	 * Checks if the specified field is a primary key field for the entity.
	 * 
	 * @param entity
	 *            The name of the entity to check.
	 * @param field
	 *            The name of the field to check.
	 * @return <code>true</code> if the field is part of the primary key for the
	 *         entity, <code>false</code> otherwise.
	 */
	public boolean isPrimaryKeyField(final String entity, final String field) {
		return getPrimaryKeyColumns(entity).contains(field) ? true : false;
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
		final XPath xpath = xPathFactory.newXPath();
		try {
			return xpath.compile(query).evaluate(item, returnType);
		} catch (final XPathExpressionException e) {
			throw new RuntimeException(e);
		}
	}
}
