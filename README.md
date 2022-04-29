# nbexportlib
utility for export netbeans library for migration to other computer

this is a simple utility to export the content of 'Libraries' of your Netbeans installation
into a directory suitable for import into another computer with a fresh installation of Netbeans.

it's is a common task when you work with many computers and/or virtual machines.

the utility read the libraries in your installation (/home/nicola/.netbeans/12.2/config/org-netbeans-api-project-libraries/Libraries)
end create in a new directory the xml files (one per library) and all the need jar; the xml files have the correct path for your destination.

EXAMPLE
java -jar nbexportlib.jar --output /tmp/java-libs-all-tocopy --dir-name /home/nicola/java-libs

then copy the directory /tmp/java-libs-all-tocopy to your new computer and rename in /home/nicola/java-libs; then in new computer copy all xml
files in /home/nicola/java-libs into your fresh installed netbeans libs directory (make it if not exist):

/home/nicola/.netbeans/13/config/org-netbeans-api-project-libraries/Libraries

restart fresh Netbeans and all libraries are migrated and correctly configurated.

see the source for detail.
