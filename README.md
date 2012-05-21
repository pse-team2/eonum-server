# Eonum Server installation and usage instruction

Instrustions on how to compile and use the eonum server.

## Getting the sources and compiling

To get the sources and compile you need to have git and maven installed.

    git://github.com/pse-team2/eonum-server.git
    cd eonum-server
    mvn install

This will generated two OSGi bundles to install in Apache Clerezza.

The two jars are located in ontologies/target and core/target.

## Install and Start Apache Clerezza

Get and start Clerezza following the instructions at <http://incubator.apache.org/clerezza/getting-started/>

## installation

before installing the two bundles compiled above you need to install
jettison, you can do this on the clerezza console with with

    zz>start("mvn:org.codehaus.jettison/jettison/1.3")

You can install and start the two eonun bundles the same way (using file:///-URLs) or accessing the webconsole over http://localhost:8080/system/console/services.

## Usage

### Importing data

On the Clerezza console:

  zz>import ch.eonum.health.locator.server._
  zz>import java.io._ 
  zz>val f = new File("/path/to/adderesses.csv")
  zz>val i = $[Importer]
  zz>i.importFile(f)


### Adding missing location

This can be accessed via web at 
http://localhost:8080/eonum/manager

Note that this will abort on 3 consequtive "over query limit" exceptions.
