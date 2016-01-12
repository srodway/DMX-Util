package com.ibm.curam.xml;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.ibm.curam.xml.dmx.DMXClobImport;
import com.ibm.curam.xml.dmx.DMXDifference;

/**
 * This class will generate a new DMX file containing only the differences taken from two separate DMX files.
 * 
 * The class also addresses a number of other fix issues related to the creation of DEMO DMX files, necessary
 * to ensure that importing the new data does not damage existing standard data already loaded in the database.
 * As such it will also set the table override attribute to true. The concept is that all data in OOTB components 
 * will be ignored in favour of that placed in the initial folder of a component.
 * 
 * Also because CLOB data can be present in separate extract files, when the import is expecting it to be present 
 * in the DMX file, this is also imported back into the main file.
 * 
 * 
 * USAGE:
 * 
 * DMXUtil has a number of parameters that are required for the successful processing of DMX data.
 * 
 * 	 path to baseline EJBServer directory (this can be an initial data extract from a baseline build)
 *   path to modified EJBServer directory (this can be a data extract from a modified build containing updated/changed data)
 *   Component Name (the name of the component that is to be processed)
 * 
 * 
 * @author Simon Rodway
 */
public class DMXUtil {
	
	private String serverDir;
	private String compareDir;
	private String initialDataDir;
	private String demoDataDir;
	private String[] ignoreFiles;
	private String[] clobExceptionFiles;
	
	private static final String CLOB_TEXT = "/clob/";
	private static final String BLOB_TEXT = "/blob/";
	private static final String VALUE = "value";

	
	public DMXUtil(final String serverDir, final String compareDir, final String component, final String[] ignoreFiles, final String[] clobExceptionFiles, final boolean overrideTable) {
		super();
		this.serverDir = serverDir;
		this.compareDir = compareDir;
		this.initialDataDir = this.serverDir + "/components/" + component + "/data/initial";  
		this.demoDataDir = this.serverDir + "/components/" + component + "/data/demo";
		this.ignoreFiles = ignoreFiles;
		this.clobExceptionFiles = clobExceptionFiles;
	}
	
	
	public DMXUtil(final String serverDir, final String compareDir, final String component, final String[] ignoreFiles, final String[] clobException) {
		this(serverDir, compareDir, component, ignoreFiles, clobException, true);
	}
	
	public static void main(String[] args) {

		if (args.length < 3) {
						
			System.out.println("\n\nInvalid parameters passed.");
			System.out.println("\t usage: DMXUtil \"path to baseline EJBServer\" \"path to modified EJBServer\" \"Component Name\".\n\n");
			System.exit(1);
		}
	
		try {
			String[] ignore = {
				"AUDITTRAIL", "BATCHPARAMDEF", "BATCHPARAMDESC", "BATCHPARAMDESCTRANSLATION", 
				"BATCHPROCDEF", "BATCHPROCDESC", "BATCHPROCDESCTRANSLATION", "CODETABLEDATA", "CODETABLEHEADER", 
				"CODETABLEHIERARCHY", "CODETABLEITEM", "CTDISPLAYNAME", "EVENTCLASS", "EVENTTYPE", "EVENTWAIT", 
				"FUNCTIONIDENTIFIER", "TASKASSIGNMENT", "BIZOBJASSOCIATION", "TASKHISTORY", "TASK", "ALERT", 
				"WORKFLOWDEADLINE", "TRANSITIONINSTANCE", "PROCINSTWDODATA", "PROCESSINSTANCE", "ACTIVITYINSTANCE", 
				"PROCESSDEFINITION", "PROCESSDEFINITIONTRANSLATION", "RULESETINFORMATION", "SECURITYFIDSID", 
				"SECURITYGROUPSID", "SECURITYIDENTIFIER", "WDOVALUESHISTORY"
			};
			
			
			String[] clobException = {"USERPAGECONFIG", "TEXTTRANSLATION"};
			
			
			new DMXUtil(args[0], args[1], args[2], ignore, clobException).createUploadData();
		} catch (IOException e) {
			e.printStackTrace();
		}		
	}

	

	public void createUploadData() throws IOException {
		
		ignoreFiles();
		
		// list the dmxs
		File[] dmxFiles = new File(initialDataDir).listFiles(new FilenameFilter() {	
			@Override
			public boolean accept(File dir, String name) {
				return name.endsWith(".dmx");
			}
		});
		
		createDemoDataDiff(dmxFiles);
	}

	

	/**
	 * Process the list of files in the initial data directory, renaming those files that 
	 * are to be ignored during this process.
	 * 
	 */
	protected void ignoreFiles() {
		if (this.ignoreFiles == null || this.ignoreFiles.length == 0) {
			return;
		}
		
		for (String ignoreFile : this.ignoreFiles) {
			File f = new File(this.initialDataDir + FileUtil.PATH_MARKER + ignoreFile + ".dmx");
			f.renameTo(new File(this.initialDataDir + FileUtil.PATH_MARKER + ignoreFile + ".dmx.ignore"));
		}
	}
	

	
	
	/**
	 * Create an DMX file for each DMX in the baseline (initial) folder and populate it with the data rows that are
	 * present in the comparator folder but not in the baseline.
	 * 
	 * @param dmxFiles
	 */
	protected void createDemoDataDiff(final File[] dmxFiles) {
		if ( dmxFiles == null || dmxFiles.length < 1) {
			System.out.println("createDemoDataDiff has no files to process");
			return;
		}
		
		final DMXClobImport fix = new DMXClobImport(compareDir);

		for (File f : dmxFiles) {			
			final DMXDifference diff = new DMXDifference(f.getAbsolutePath(),compareDir + FileUtil.PATH_MARKER +f.getName());	
			
			final Node node = diff.getModifiedData();
			if (isClobExceptionFile(f.getName())) {
				fix.fixClobReferences(node);
			}
			
			final List <String> clobRef = getClobReferences(node);
			final List <String> blobRef = getBlobReferences(node);
			
			copyReferences(clobRef, demoDataDir+"/clob/");
			copyReferences(blobRef, demoDataDir+"/blob/");
				
			final StringBuffer buf = new StringBuffer(DMXDifference.getStringFromNode(node));
			try {
				createFile(buf, demoDataDir + FileUtil.PATH_MARKER + f.getName());
			} catch (IOException e) {
				System.out.println("Unable to save data file " + f.getName());
				e.printStackTrace();
			}
		}
	}	
	
	
	private void copyReferences(List <String> fileReferences, String destDir) {
		if (fileReferences == null || fileReferences.isEmpty()) {
			// Nothing to copy
			return;
		}
		
		FileUtil.copyFiles(fileReferences, destDir);
	}
	
	
	private List <String> getClobReferences(Node node) {
		final List <String> clobList = new ArrayList<String> ();
		
		buildReferences(node, clobList, CLOB_TEXT);
		
		return clobList;
	}
	
	private List <String> getBlobReferences(Node node) {
		final List <String> blobList = new ArrayList<String> ();
		
		buildReferences(node, blobList, BLOB_TEXT);
		
		return blobList;
	}
	
	
	/**
	 * Examine the provided Node for the occurrence on the reference (e.g. /clob/) 
	 * within a VALUE element.  This will result in a list (refList) of references
	 * to blob or clob files that can be processed later (copied to the demo directory).
	 * 
	 * @param node
	 * @param List of references of files extracted from the node
	 * @param reference string to be searched for within the node and added to the reference list.
	 */
	private void buildReferences(Node node, List <String> refList, String reference) {
		if (node == null) {
			return;
		}
		
		final NodeList items = node.getChildNodes();
		
		for (int i = 0; items != null && i < items.getLength(); i++) {
			final Node clobNode = items.item(i);
			if (isRequiredNode(clobNode, Node.ELEMENT_NODE, VALUE)) {
				if (containsText(clobNode.getFirstChild(), reference)) {
					final Node child = clobNode.getFirstChild();
					final String fileRef = child.getNodeValue();
					final String fileName = fileRef.substring((fileRef.indexOf(FileUtil.PATH_MARKER)));
					final String fullFileName = compareDir+fileName;
					refList.add(fullFileName);
				}
			}
			if (clobNode.hasChildNodes()) {
				buildReferences(clobNode, refList, reference);
			}
		}
	}
	
	
	/**
	 *  
	 * @param node
	 * @return true if Node contains column of type clob, false otherwise.
	 */
	private boolean containsText(Node node, String text) {
		if (node != null && node.getNodeValue() != null && node.getNodeValue().contains(text))
			return true;
				
		return false;
	}
	
	/**
	 * Read through the Node searching for an <column> node that might have a
	 * type value of "nodeName"
	 * 
	 * @param node
	 * @return true if Node contains column of type clob, false otherwise.
	 */
	private boolean isRequiredNode(Node node, short nodeType, String nodeName) {
		if (node != null && node.getNodeName().equals(nodeName)) {
			return true;
		}

		return false;
	}	
	
	
	
	private boolean isClobExceptionFile(String filename) {
		boolean isClobFile = false;
		for (String s : clobExceptionFiles) {
			if (filename != null && filename.contains(s)) { 
				isClobFile = true;
			}
		}
		return isClobFile;
	}
	
	
	/**
	 * 
	 * @param buf
	 * @param fullName
	 * @throws IOException
	 */
	private void createFile(final StringBuffer buf, final String fullName) throws IOException {
		FileOutputStream os = new FileOutputStream(new File(fullName));
	    os.write(buf.toString().getBytes());
		os.close();
	}
	
}

