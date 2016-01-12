# DMX-Util
DMX data delta migration generations tools

This toolset will generate a new IBM Curam DMX file containing only the differences taken from two separate DMX files.
 
The DMXUtil class also addresses a number of other fix issues related to the creation of DEMO DMX files, necessary to ensure that importing the new data does not damage existing standard data already loaded in the database. As such it will also set the table override attribute to true. The concept is that all data in OOTB components will be ignored in favour of that placed in the initial folder of a component.
  
Also because CLOB data can be present in separate extract files, when the import is expecting it to be present in the DMX file, this is also imported back into the main file.
 
USAGE:
 
DMXUtil has a number of parameters that are required for the successful processing of DMX data.
 
 path to baseline EJBServer directory (this can be an initial data extract from a baseline build)
 path to modified EJBServer directory (this can be a data extract from a modified build containing updated/changed data)
 Component Name (the name of the component that is to be processed)
