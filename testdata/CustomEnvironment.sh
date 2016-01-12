#
# Curam Custom Development Environment Configuration
#
export DEV_HOME=/Users/simonrodway/Development

#
# JAVA Configuration details
#
export JAVA_HOME=/Library/Java/JavaVirtualMachines/jdk1.8.0_66.jdk/Contents/Home
#export JAVA_HOME=/Library/Java/JavaVirtualMachines/jdk1.7.0_80.jdk/Contents/Home
#export JAVA_HOME=/Library/Java/JavaVirtualMachines/1.6.0.jdk/Contents/Home
#
# Tools options for ISO-8859-1 is required because of the use for ú in Cúram 
#
export JAVA_TOOL_OPTIONS=-Dfile.encoding=ISO-8859-1
export J2EE_PATH=$DEV_HOME/Library
export J2EE_JAR=$J2EE_PATH/j2ee.jar
#
#
#
export XIM_OUTPUT=true

#
# ANT Configuration details
#
export ANT_HOME=$DEV_HOME/Tools/apache-ant-1.9.4
export ANT_OPTS="-Xmx2048m -Xms512m"

#
# Database H2
#
export H2_HOME=$DEV_HOME/Databases/h2

export PATH=$ANT_HOME/bin:$JAVA_HOME/bin:$H2_HOME/bin:$PATH
#
# Application Server Defaults
#
export WAS_HOME=/opt/IBM/WebSphere/Liberty
