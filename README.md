# ACE Authorization Server

## Prerequisites
The following software needs to be installed for this project to compile and run:
* Java JDK 8+
* Gradle
* PostgreSQL

This project also depends on the ace-java (https://bitbucket.org/marco-tiloca-sics/ace-java) and aaiot-lib (https://github.com/SEI-TTG/aaiot-lib) 
libraries. You should download, compile, and deploy both of them to a local Maven repo, so that this project will
find them when resolving its dependencies.
 
## Configuration
The only minimum configuration that is needed before running ace-as is in the config.json file, the "root_db_pwd" 
parameter. This has to be set to the postgres (root) password of the PostgreSQL installation of the current computer.
 
## Usage
The main entry class is edu.cmu.sei.ttg.aaiot.as.gui.FXApplication. This starts the GUI.
