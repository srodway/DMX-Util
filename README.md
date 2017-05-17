# DMX-Util
DMX data delta migration generations tools

This toolset will generate a new IBM Curam DMX file containing only the differences taken from two separate DMX files.
 
The DMXUtil class also addresses a number of other fix issues related to the creation of DEMO DMX files, necessary to ensure that importing the new data does not damage existing standard data already loaded in the database. As such it will also set the table override attribute to true. The concept is that all data in OOTB components will be ignored in favour of that placed in the initial folder of a component.
  
Also because CLOB data can be present in separate extract files, when the import is expecting it to be present in the DMX file, this is also imported back into the main file.

Current implementation assumes the tool will be used in conjunction with an implementation of Cúram.  As such the default directory structure is assumes to be based on a Cúram implementation, with the resulting DMX output being used as part of a component for 'demonstration' samples.  As such the directory structure will be based on the following: 

	/CuramBaseDirectory/EJBServerPath/component/ComponentName/data
	
The initial extract is assumed to be taken from the implementation of the Cúram deployment and will form the starting point, or baseline for the data comparison.  The second extract is assume to have been take from the same implementation as the baseline, but containing any modification or additions.  The tool will then compare the baseline with the second extract and generate a 'demo' set of DMX and CTX files that contain only the additional details found in the second extract.
 
USAGE:
 
DMXUtil has a number of parameters that are required for the successful processing of DMX data.
 
	usage: DMXUtil [-codetable] -compare <Compare DMX Directory> -component
       <Component Name> [-ctdir <Codetable Output Directory>]
       [-curamdatapath] [-ignore <List of attributes to ignore>] -master <Master DMX
       Directory> -output <DMX Output Directory> [-override]
       
	Perform Cúram DMX data comparison process, outputting DMX files with <row>'s different from the master data source

 	-master <Master DMX Directory>         Master set of DMX files, forming the baseline data to be compared against
 	-compare <Compare DMX Directory>       Set of DMX files that are to be compared against the master set
 	-output <DMX Output Directory>         Location where the resulting DMX files are written

 	-codetable                             Generated Cúram codetable files from related DMX files
 	-ctdir <Codetable Output Directory>    Location where the resulting CTX files are written, if option codetable is defined.
 	-ignore <List of attributes to ignore> List of attributes to ignore when comparing rows

EXECUTION:

This tool in it's current form should be run either on the command line or through you IDE with the defined parameters. The parameters to be passed are as defined above, with an example as follows: -

	/locationOfImplementation/DMX-Util/testdata 
	/locationOfImplementation/DMX-Util/testdata/components/SampleComponent/data/compare 
	SampleComponent
 
Common Libraries (JAR)

	commons-cli-1.3.1.jar
	commons-io-2.4.jar
	jdom-2.0.6.jar
	xml.jar

EXAMPLE: 

From the DMXUtil/bin folder: -

	java -cp ../lib/ant.jar:../lib/commons-cli-1.3.1.jar:../lib/commons-io-2.4.jar:../lib/CuramAnalysisDocumentation.jar:../lib/jdom-2.0.6.jar:../lib/xml.jar:. com.ibm.curam.xml.DMXUtil -codetable -ignore lastwritten timeentered -master /Development/IBM/Curam/DMX-Util/testdata/components/SampleComponent/data/initial -compare /Development/IBM/Curam/DMX-Util/testdata/components/SampleComponent/data/compare -output /Development/IBM/Curam/DMX-Util/testdata/components/SampleComponent/data/demo  -ctdir /Development/IBM/Curam/DMX-Util/testdata/components/SampleComponent/data/codetable
	
TODO:

* Add UI module to allow additional options beyond command line execution
* Extend comparision process to CLOB's and BLOB's
* Copy of CLOB and BLOB files to be reduced to cover only those identified as part of an included new row
* Add key transformation process to replace internal keys with readable (user defined range) values
* Add unit tests

* Outstanding defect/issues to address
	Clob/Blob file paths need changing to reflect potential output path structure @10138