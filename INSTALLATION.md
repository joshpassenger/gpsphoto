Hi Chris,

Here is the link to the latest GPS photo tool:

https://phototransect-deployment-ap-southeast-2.s3.ap-southeast-2.amazonaws.com/gps-photo/releases/gpsphoto_v1-1.0.10.zip

To install download and extract the zip file into the c:\GPSPhoto directory on your local computer (I think I named it this)

This will create a versioned directory which contains the current release (gpsphoto-1.0.7) for the rest of this document I will call this <installationdir> ie:

<installationdir> for this release is c:\GPSPhoto\gpsphoto-1.0.10\

Since you do not have administrator privledges on all machines and I require an environment variable to be set (JAVA_HOME) you may can either:

Option 1

Try and add an environment variable for the current user:

http://viralpatel.net/blogs/windows-7-set-environment-variable-without-admin-access/

Insert a JAVA_HOME variable pointing to the correct Java installation location

OR

Option 2

You could do why I did previously and edit the batch file and insert the variable into:

<installdir>/bin/gpsphoto.bat

Edit the file to add the highlighted line below

@rem Set local scope for the variables with windows NT shell
if "%OS%"=="Windows_NT" setlocal

set JAVA_HOME=<insert path to your java installation>

set DIRNAME=%~dp0
if "%DIRNAME%" == "" set DIRNAME=.
set APP_BASE_NAME=%~n0
set APP_HOME=%DIRNAME%..


You will find the correct value for JAVA_HOME by looking in a previous installation version's <installdir>/bin/gpsphoto.bat

On a new machine you will need to install Java V1.8 (Java 8) before using the system and set JAVA_HOME.

The last step is to update the short cut on the desktop (for existing installations).

Right click and select properties on the desktop GPS Photo link and change the working directory and target to the new installation directory.

For new machines, simply create a new shortcut to <installationdir>/bin/gpsphoto.bat

IMPORTANT the working directory of the short cut must be set to: <installationdir> (the directory above /bin)

When you start up the program check the title bar has the correct version.

Yell if you have any issues,

Josh
