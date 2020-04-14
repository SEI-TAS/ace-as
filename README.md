# ACE Authorization Server

## Prerequisites
The following software needs to be installed for this project to compile and run:
* Java JDK 8+
* Gradle
* PostgreSQL

This project also depends on the ace-java (https://bitbucket.org/sebastian_echeverria/ace-java-postgres) and aaiot-lib (https://github.com/SEI-TTG/aaiot-lib) libraries. You should download, compile, and deploy both of them to a local Maven repo, so that this project will find them when resolving its dependencies.

If it doesn't exist, the database used by ace-as as defined by the ace-java library will be automatically created when ace-as starts.
 
## Configuration
Configuration parameters can be changed in the file config.json. The only minimum configuration that is needed before running ace-as is the "root_db_pwd" parameter. 

 * id: the ID or name used to identify this AS.
 * root_db_pwd: this has to be set to the postgres (root) password of the PostgreSQL installation of the current computer.
 * db_user and db_pwd: user and password to be used to access the ace DB.
 * token_duration_in_mins: how many minutes an issued token will be valid.
 * local_coaps_port: port used by the AS for COAPS connections.
 
## Usage
The main entry class is edu.cmu.sei.ttg.aaiot.as.gui.FXApplication. This starts the GUI. A simple way to start it from gradle is with `./gradlew run` 

Credentials and tokens are stored in the PostgresQL database. Delete the DB manually or clear the appropriate tables to remove them.

For more information, see https://github.com/SEI-TAS/ace-client/wiki
