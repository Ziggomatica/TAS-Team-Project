# Time and Attendance System

This project contains a library of methods for accessing and manipulating time and attendance data within an SQL database. It was developed within Netbeans as a team project using the SCRUM process for Software Engineering I at Jacksonville State University.

## Getting Started (Prerequisites and Installation)

The best way to experience this software is within [Netbeans (Java EE)](https://netbeans.org/downloads/start.html?platform=windows&lang=en&option=javaee) with the following modules installed and added to the project library:

* Apache Tomcat (choose this option upon installation of Netbeans (Java EE))
* [Hamcrest](http://search.maven.org/remotecontent?filepath=org/hamcrest/hamcrest-core/1.3/hamcrest-core-1.3.jar)
* [JUnit](http://search.maven.org/remotecontent?filepath=junit/junit/4.12/junit-4.12.jar)
* [MYSQL Connector/J](https://dev.mysql.com/get/Downloads/Connector-J/mysql-connector-java-5.1.44.zip)
* [JSON](https://storage.googleapis.com/google-code-archive-downloads/v2/code.google.com/json-simple/json-simple-1.1.1.jar)

You will also need the [MySQL Database Management System](https://jsu-my.sharepoint.com/personal/jsnellen_jsu_edu/Documents/Forms/All.aspx?slrid=fe78319e-100f-4000-ca36-bf299e9b1ab8&RootFolder=%2Fpersonal%2Fjsnellen_jsu_edu%2FDocuments%2FCS488&FolderCTID=0x012000BF2BCBE5BD28A84080FABCFB1C7A92B7) installed. After installing, the [sample database](https://drive.google.com/open?id=1Ke-trp4itXPN8lzzTa9r2JaX7ZcwWuj1) should be imported from either the MySQL shell or from the MySQL GUI Tools or Workbench.

You will also need to create a local user account to access the database. Open the "MySQL Command Line Client" from the "MySQL" program group on the Start menu.  You will be prompted to log in using your root password.  After logging in as root, enter the following commands at the MySQL prompt (make sure the username (tasuser) and password (snellybelly42) are exact!):

```
CREATE USER 'tasuser'@'localhost' IDENTIFIED BY 'snellybelly42';
GRANT ALL ON tas.* TO 'tasuser'@'localhost';
FLUSH PRIVILEGES;
```

Each of these commands should get a "Query OK" response at the prompt.

After the necessary prerequisites are met, you are ready to test the software!

## Testing the Software

This project contains 5 J-Unit test files which test the software based on 5 different features that were implemented during each sprint in the Scrum process.  To run these tests within Netbeans, simply right-click their name under the Test Packages folder and click "Run File".

### Feature 1

This feature involves the creation of a connection to the database, and the successful retrieval of data from it in the form of objects. These objects include the Shift, Badge, and Punch classes.  The Shift class contains data that pertains to the rules of a given shift.  The Badge class holds information about the employees' names and their IDs.  The Punch class contains all the relevant information for a given time punch.  The test file tests for these features by comparing an expected string with the actual data retrieved from the database in the form of a string.

#### Example of Feature 1 Test

```
@Test
    public void testGetBadges() {
		
		/* Retrieve Badges from Database */

        Badge b1 = db.getBadge("12565C60");
        Badge b2 = db.getBadge("08D01475");
        Badge b3 = db.getBadge("D2CC71D4");
		
		/* Compare to Expected Values */

        assertEquals(b1.toString(), "#12565C60 (Chapman, Joshua E)");
        assertEquals(b2.toString(), "#08D01475 (Littell, Amie D)");
        assertEquals(b3.toString(), "#D2CC71D4 (Lawson, Matthew J)");
        
    }
```

### Feature 2

This feature involves the addition of a new method *insertPunch()* which allows a new punch to be inserted into the database.  The test file tests for this feature by creating a new punch with the *insertPunch()* method and then retrieving it's relevant data and comparing it to an expected string.

### Feature 3

This feature implements punch time adjustment according to the current shift's rules.  The test file tests for this feature by getting the shift rules and punch data, calling the *adjust()* method, and comparing the adjusted time stamp to an expected string.

#### Example of Feature 3 Test

```
@Test
    public void testAdjustPunchesShift2Weekday() {
		
        /* Get Shift Ruleset and Punch Data */
        
        Shift s2 = db.getShift(2);

        Punch p1 = db.getPunch(4943);
        Punch p2 = db.getPunch(5004);
		
        /* Adjust Punches According to Shift Rulesets */
        
        p1.adjust(s2);
        p2.adjust(s2);
        
        /* Compare Adjusted Timestamps to Expected Values */

        assertEquals(p1.printOriginalTimestamp(), "#08D01475 CLOCKED IN: TUE 09/19/2017 11:59:33");
        assertEquals(p1.printAdjustedTimestamp(), "#08D01475 CLOCKED IN: TUE 09/19/2017 12:00:00 (Shift Start)");
        
        assertEquals(p2.printOriginalTimestamp(), "#08D01475 CLOCKED OUT: TUE 09/19/2017 21:30:27");
        assertEquals(p2.printAdjustedTimestamp(), "#08D01475 CLOCKED OUT: TUE 09/19/2017 21:30:27 (None)");
        
    }
```

### Feature 4

This feature implements the ability to compute the total number of minutes accrued in a day for a given punch.  The test file tests for this feature by calling the *getMinutesAccrued()* method with a certain punch as a parameter, and comparing the expected value to the result.

#### Example of Feature 4 Test

```
 @Test
    public void testMinutesAccruedShift1Weekday() {
		
		/* Get Punch */
        
        Punch p = db.getPunch(3634);
		
		/* Compute Pay Period Total */
        
        int m = db.getMinutesAccrued(p);
		
		/* Compare to Expected Value */
        
        assertEquals(m, 480);
        
    }
```

### Feature 5

This feature prepares the library created by the previous features for integration into a web application. The main component of this feature is the *getPunchListAsJSON()* method, which collects all punches in a day from a given punch argument, totals the minutes worked in that day, and compresses this data into a JSON string.  The test file tests for this feature by comparing an expected JSON string with the actual JSON string that is retrieved by the *getPunchListAsJSON()* method.

## Built With

* [Netbeans (Java EE)](https://netbeans.org/downloads/start.html?platform=windows&lang=en&option=javaee)
* [MySQL Database Management System](https://jsu-my.sharepoint.com/personal/jsnellen_jsu_edu/Documents/Forms/All.aspx?slrid=fe78319e-100f-4000-ca36-bf299e9b1ab8&RootFolder=%2Fpersonal%2Fjsnellen_jsu_edu%2FDocuments%2FCS488&FolderCTID=0x012000BF2BCBE5BD28A84080FABCFB1C7A92B7)

## Future Endeavors

Since this project is essentially a library of methods for a time and attendance system, it can be used as the back-end for other things, such as a web application.  An extension of this project has been started with the [TAS Web App](https://github.com/Ziggomatica/TAS-Web-App).  *Massive credit goes to our instructor, Jay Snellen, for supplying the Web Server code.*

## Contributing

Future contributions to the project may be considered by our instructor, Jay Snellen (jsnellen@jsu.edu).

## Authors

* Jay Snellen - *Product Owner/Instructor*
* Christopher Owen - *Lead Developer*
* Travis Cotney - *Development Team*
* Morgan Sweatman - *Development Team*
* Diane Lee - *Development Team*
* Alexander Tolbert - *Development Team*
* Brandon Taylor - *Development Team*
* Ashleigh Wilcox - *Scrum Master*

## Acknowledgements

* A big thank you to our instructor, Jay Snellen
* The creators of NetBeans
* Alka-Seltzer and Coffee

