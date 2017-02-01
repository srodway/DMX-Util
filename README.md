# DMX-Util
DMX data delta migration generations tools

This toolset will generate a new IBM Curam DMX file containing only the differences taken from two separate DMX files.
 
The DMXUtil class also addresses a number of other fix issues related to the creation of DEMO DMX files, necessary to ensure that importing the new data does not damage existing standard data already loaded in the database. As such it will also set the table override attribute to true. The concept is that all data in OOTB components will be ignored in favour of that placed in the initial folder of a component.
  
Also because CLOB data can be present in separate extract files, when the import is expecting it to be present in the DMX file, this is also imported back into the main file.

Current implementation assumes the tool will be used in conjunction with an implementation of Cúram.  As such the default directory structure is assumes to be based on a Cúram implementation, with the resulting DMX output being used as part of a component for 'demonstration' samples.  As such the directory structure will be based on the following: 

	/CuramBaseDirectory/EJBServerPath/component/ComponentName/data
	
The initial extract is assumed to be taken from the implementation of the Cúram deployment and will form the starting point, or baseline for the data comparision.  The second extract is assume to have been take from the same implementation as the baseline, but containing any modification or additions.  The tool will then compare the baseline with the second extract and generate a 'demo' set of DMX and CTX files that contain only the additional details found in the second extract.
 
USAGE:
 
DMXUtil has a number of parameters that are required for the successful processing of DMX data.
 
	path to baseline EJBServer directory (this can be an initial data extract from a baseline build)
	path to modified EJBServer directory (this can be a data extract from a modified build containing updated/changed data)
	Component Name (the name of the component that is to be processed)

Internally when generating codetable content, the tool assumes that a codetable directory within the Cúram data folder exists.

	$SERVER_DIR/component/ComponentName/data/codetable

EXECUTION:

This tool in it's current form should be run either on the command line or through you IDE with the defined parameters. The parameters to be passed are as defined above, with an example as follows: -

	/locationOfImplementation/DMX-Util/testdata 
	/locationOfImplementation/DMX-Util/testdata/components/SampleComponent/data/compare 
	SampleComponent
 
Common Libraries (JAR)

	commons-io-2.4.jar
	jdom-2.0.6.jar
	xml.jar
  
TODO:

* Update tool to report progress through
* Add UI module to allow additional options beyond command line execution
* Provide [option] switches to address: 
	1. Direction of comparision, baseline with second extract, second extract with baseline, both directions
	2. Exclusion of attributes within DMX, e.g. exclude comparision of last modified timestamps
	3. Definition of directories, to support options beyond assumed Cúram data directory structure
* Extend comparision process to CLOB's and BLOB's
* Copy of CLOB and BLOB files to be reduced to cover only those identified as part of an included new row
* Add unit tests
