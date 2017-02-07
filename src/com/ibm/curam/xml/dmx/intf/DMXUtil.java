package com.ibm.curam.xml.dmx.intf;

import java.io.IOException;

public interface DMXUtil {
	
	
	public static final String CLOB_TEXT = "/clob/";
	public static final String BLOB_TEXT = "/blob/";
	public static final String VALUE = "value";

	
	public void processDMXData() throws IOException;
	
}
