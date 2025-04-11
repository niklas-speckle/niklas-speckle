# tempera
This project is based on the swe-skeleton (read me instruction to it as well as the contributors are listed below), although the memory was changed from H2 (swe-skeleton) to PostgreSQL 15. We are also working with 3 different kinds of devices: a server (e.g. on a pc or notebook), Access Points (Raspberry Pi) and Tempera Devices (Arduino). The three devices (or more) together create the system.

## ProstgreSQL
For the SQL-client we use pgAdmin. The program is set up with the following settings:
- user: postgres
- password: SuperSecret
- port: 5432
- and should an e-mail-adress be required: pgadmin@example.com
(These are the settings are the same as used in PS Database Systems)
If you use different settings please adapt application.properties to your settings.

After that you can now create a new database "tempera" under localhost.

## Application Log
Log files for the application are stored in g4t2\logs by default. There is no need to create the directory, the application 
handles this automatically. The location of the log files can be changed in the application.properties file located at g4t2\src\main\resources

Please note that the Access Point has its own logfiles (c.v. hardware\accessPoint\notesAccessPoint.txt).

## Access Points
We use a Raspberry Pi 4B for our Access Point. How to setup an Access Point correctly is written in hardware\accessPoint\notesAccessPoint.txt as well as in the App on the page 'Access Point Setup'. To get there you have to log in as an administration (e.g. admin) und then select 'Admin Submenu' in the topbar and 'Access Points' from the drow-down menu. This page allows admins to create new Access Points in the Database and on the bottom of the page a button "Setup Information" will appear. This button will direct you to the before mentioned setup page. There is also a button for "Detailed Configurations", where all the possible configurations of Access Points (conf.yaml) are described in more detail.

## Tempera Devices
We use an Arduino Nano 33 ble for our Tempera Device. How to setup a Tempera Device correctly is written in hardware\temperaStation\notesTemperaStation.txt as well as in the App on the page 'Tempera Device Setup'. To get there you have to log in as an administartor (e.g. admin) and then select 'Admin Submenu' in the topbar and 'Tempera Devices' from the drop-down menu. This page allows admins to create new Tempera Devices in the Databse and on the bottom of the page a button "Setup Information" will appear. This button will direct you to the before mentioned setup page.


## contributors to tempera
- Astrid Reisinger
- Jonas Bayer
- Lea Hof
- Manuela Niedrist
- Niklas Speckle

## swe-skeleton
SKEL: Skeleton Project
This project provides a starting point for development of projects during the
course "Software Engineering". It is a simple web application offering nearly
no "real" functionality. Its main purpose is to help you getting started quickly
by providing a suitable starting point.
It utilizes Spring Boot and is configured as a Maven web application project with:

all relevant Spring Framework features enabled
embedded Tomcat with support for JSF2
embedded H2 in-memory database (including H2 console)
support for PrimeFaces
basic functionality for user management and Spring web security

Execute "mvn spring-boot:run" to start the skeleton project and connect to
http://localhost:8080/ to access the skeleton web application. You may login
with "admin" and "passwd".
Feel free to use this skeleton project as you see fit - but keept in mind that
this project is primarilly provided to be used for educational purposes. Don't
use it for production.

### Contributors to swe-skeleton:
- Christian Sillaber
- Michael Brunner
- Clemens Sauerwein
- Andrea Mussmann
- Alexander Blaas

## Requirements
- Java 17
