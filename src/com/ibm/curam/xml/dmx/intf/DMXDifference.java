package com.ibm.curam.xml.dmx.intf;

import java.util.List;

import org.w3c.dom.Node;

public interface DMXDifference {

	public static final String DEFAULT_OVERRIDE_STATE = "false";
	public static final String TABLE_OVERRIDE = "override";
	public static final String DATA_ROW = "row";
	public static final String TABLE_DEF = "table";
	
	/**
	 * Returns a Node that contains the rows from the DMX files that are in the
	 * modified file, but not present in the master file.
	 * 
	 * @return Node containing data rows
	 */
	public Node getModifiedData();
	
	
	/**
	 * When comparing rows, it is possible to define a list of attributes, by name that
	 * can be ignored in the compare process.
	 * 
	 * @param attributes
	 */
	public void setIgnoreAttributes(final List<String> attributes);
	
	
}
