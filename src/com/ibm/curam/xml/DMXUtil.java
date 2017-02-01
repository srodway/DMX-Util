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
package com.ibm.curam.xml;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.ibm.curam.cli.ProgressIndicator;
import com.ibm.curam.utils.FileUtil;
import com.ibm.curam.xml.ctx.impl.DMX2CTXConvertor;
import com.ibm.curam.xml.dmx.impl.DMXClobImport;
import com.ibm.curam.xml.dmx.impl.DMXDifference;
import com.ibm.curam.xml.dmx.impl.DMXFile;

/**
 * <p>This class will generate a new DMX file containing only the differences taken from two separate DMX files.</p>
 * 
 * <p>The class also addresses a number of other fix issues related to the creation of DEMO DMX files, necessary
 * to ensure that importing the new data does not damage existing standard data already loaded in the database.
 * As such it will also set the table override attribute to true. The concept is that all data in OOTB components 
 * will be ignored in favour of that placed in the initial folder of a component.</p>
 * 
 * <p>Also because CLOB data can be present in separate extract files, when the import is expecting it to be present 
 * in the DMX file, this is also imported back into the main file.</p>
 *
 * <p>USAGE:</p>
 * 
 * DMXUtil has a number of parameters that are required for the successful processing of DMX data.<br/>
 * <ul>
 * 	 <li>path to baseline EJBServer directory (this can be an initial data extract from a baseline build)</li>
 *   <li>path to modified EJBServer directory (this can be a data extract from a modified build containing updated/changed data)</li>
 *   <li>Component Name (the name of the component that is to be processed)</li>
 * </ul>
 * </p>
 * 
 * @author Simon Rodway
 */
public class DMXUtil {
	
	private static final String CMD_OPTION_CODETABLE = "codetable";
	private static final String CMD_OPTION_CODETABLEDIR = "ctdir";
	private static final String CMD_OPTION_OVERRIDE = "override";
	private static final String CMD_OPTION_COMPARE = "compare";
	private static final String CMD_OPTION_MASTER = "master";
	private static final String CMD_OPTION_OUTPUT = "output";
	private static final String CMD_OPTION_IGNORELASTWRITTEN = "ignorelastwritten";

	private static final String ROW = "<row>";
	
	private String serverDir;
	private String compareDir;
	private String initialDataDir;
	private String demoDataDir;
	private String codetableDir;
	private String[] ignoreFiles;
	private String[] clobExceptionFiles;
	
	private boolean processCodetables = false;
	private boolean processOverride = false;
	private boolean ignoreLastWritten = false;
	
	private static final String CLOB_TEXT = "/clob/";
	private static final String BLOB_TEXT = "/blob/";
	private static final String VALUE = "value";
	
	private DMXUtil(final CommandLine cmd, final String[] ignoreFiles, String[] clobException) {
		if(cmd.hasOption(CMD_OPTION_CODETABLE)) {
			processCodetables=true;
		}
		
		if (cmd.hasOption(CMD_OPTION_OVERRIDE)) {
			processOverride=true;
		}
				
		if (cmd.hasOption(CMD_OPTION_IGNORELASTWRITTEN)) {
			ignoreLastWritten=true;
		}
		
		this.ignoreFiles = ignoreFiles;
		this.clobExceptionFiles = clobException;
		
		initialise(cmd);
	}
	
	
	private void initialise(final CommandLine cmd) {
		this.serverDir = cmd.getOptionValue(CMD_OPTION_MASTER);
		this.compareDir = cmd.getOptionValue(CMD_OPTION_COMPARE);
		
		this.initialDataDir = this.serverDir;
		this.demoDataDir = cmd.getOptionValue(CMD_OPTION_OUTPUT);
		this.codetableDir = cmd.getOptionValue(CMD_OPTION_CODETABLEDIR);
	}
	
	
	public static void main(String[] args) {
	
		try {
			final String[] ignore = {
				"AUDITTRAIL", "BATCHPARAMDEF", "BATCHPARAMDESC", "BATCHPARAMDESCTRANSLATION", 
				"BATCHPROCDEF", "BATCHPROCDESC", "BATCHPROCDESCTRANSLATION", 
				//"CODETABLEDATA", "CODETABLEHEADER","CODETABLEHIERARCHY", "CODETABLEITEM", 
				"CTDISPLAYNAME", "EVENTCLASS", "EVENTTYPE", "EVENTWAIT", 
				"FUNCTIONIDENTIFIER", "TASKASSIGNMENT", "BIZOBJASSOCIATION", "TASKHISTORY", "TASK", "ALERT", 
				"WORKFLOWDEADLINE", "TRANSITIONINSTANCE", "PROCINSTWDODATA", "PROCESSINSTANCE", "ACTIVITYINSTANCE", 
				"PROCESSDEFINITION", "PROCESSDEFINITIONTRANSLATION", "RULESETINFORMATION", "SECURITYFIDSID", 
				"SECURITYGROUPSID", "SECURITYIDENTIFIER", "WDOVALUESHISTORY"
			};
					
			final CommandLine cmd = getCommandLine(args);
							
			final String[] clobException = {"USERPAGECONFIG", "TEXTTRANSLATION"};
			
			final DMXUtil util = new DMXUtil(cmd, ignore, clobException);
			
			util.createUploadData();
			util.generateCTX();		
			
		} catch (IOException e) {
			e.printStackTrace();
		}		
	}

	
	
	public static final void outputCommandLineHelp() {
		 final String header = "Perform Cúram DMX data comparision process, outputing DMX files with <row>'s different from the master data source\n\n";
		 final String footer = "\nPlease report issues to Cúram UKI Team at IBM";
		 
		 final HelpFormatter formatter = new HelpFormatter();
		 formatter.printHelp("DMXUtil", header, getCommandLineOptions(), footer, true);
	}
	
	

	public static final Options getCommandLineOptions() {
		final Options dmxUtilOptions = new Options();
		
		final Option master = Option.builder(CMD_OPTION_MASTER)
				                    .argName("Master DMX Directory")
				                    .hasArg()
				                    .required()
				                    .desc("Master set of DMX files, forming the baseline data to be compared against")
				                    .build();

		final Option compare = Option.builder(CMD_OPTION_COMPARE)
				                     .argName("Compare DMX Directory")
				                     .hasArg()
				                     .required()
				                     .desc("Set of DMX files that are to be compared against the master set")
				                     .build();

		final Option output = Option.builder(CMD_OPTION_OUTPUT)
						            .argName("DMX Output Directory")
						            .hasArg()
						            .required()
						            .desc("Location where the resulting DMX files are written")
						            .build();
		
		final Option ctdir = Option.builder(CMD_OPTION_CODETABLEDIR)
						           .argName("Codetable Output Directory")
						           .hasArg()
						           .desc("Location where the resulting CTX files are written, if option "+CMD_OPTION_CODETABLE+" is defined.")
						           .build();
		
		dmxUtilOptions.addOption(master);
		dmxUtilOptions.addOption(compare);
		dmxUtilOptions.addOption(output);
		dmxUtilOptions.addOption(ctdir);
		
		//dmxUtilOptions.addOption(CMD_OPTION_OVERRIDE, "Set DMX 'override' table attribute to true");
		dmxUtilOptions.addOption(CMD_OPTION_CODETABLE, "Generated Cúram codetable files from related DMX files");
		dmxUtilOptions.addOption(CMD_OPTION_IGNORELASTWRITTEN, "Ignore changed to 'last written' date & time when comparing rows.");

		return dmxUtilOptions;
	}
	
	private static final CommandLine getCommandLine(String[] args) {
		final Options dmxUtilOptions = getCommandLineOptions();
		final CommandLineParser parser = new DefaultParser();
		CommandLine cmd = null;
		try {
			cmd = parser.parse(dmxUtilOptions, args);
		} catch (ParseException e) {
			outputCommandLineHelp();
			// e.printStackTrace();
			System.exit(1);
		}
		
		return cmd;
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

	
	
	public void generateCTX() {
		if (!isProcessCodetables()) {
			return;
		}
		
		final DMX2CTXConvertor dmx2ctxConvertor = new DMX2CTXConvertor();
		dmx2ctxConvertor.setDmxDir(demoDataDir);
		dmx2ctxConvertor.setOutputDir(codetableDir);
		dmx2ctxConvertor.execute();
	}
	
	
	public boolean isProcessCodetables() {
		return processCodetables;
	}


	public boolean isProcessOverride() {
		return processOverride;
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
			System.out.println("DMXUtil has no DMX files to process");
			return;
		}
		
		System.out.println("Starting comparision of DMX files");
		int processedFiles = 0;
		
		final ProgressIndicator progressReport = new ProgressIndicator(dmxFiles.length);
		
		final DMXClobImport fix = new DMXClobImport(compareDir);

		for (File f : dmxFiles) {			
			final DMXDifference diff = new DMXDifference(f.getAbsolutePath(),compareDir + FileUtil.PATH_MARKER +f.getName());
			
			diff.setIgnoreLastWritten(this.ignoreLastWritten);
			
			final Node node = diff.getModifiedData();
			if (isClobExceptionFile(f.getName())) {
				fix.fixClobReferences(node);
			}
			
			final List <String> clobRef = getClobReferences(node);
			final List <String> blobRef = getBlobReferences(node);
			
			copyReferences(clobRef, demoDataDir+CLOB_TEXT);
			copyReferences(blobRef, demoDataDir+BLOB_TEXT);
				
			final StringBuffer buf = new StringBuffer(DMXFile.getNodeAsText(node));
			try {
				if (buf.indexOf(ROW)>0) {
					createFile(buf, demoDataDir + FileUtil.PATH_MARKER + f.getName());
				}
			} catch (IOException e) {
				System.out.println("Unable to save data file " + f.getName());
				e.printStackTrace();
			}
			
			progressReport.updateProgress(processedFiles++);
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

