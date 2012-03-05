# Health Service Locator

This is to become a service to locate health care facilities.

## Current functionality

Partially implemented: import tool

## compiling

TBD

## installation

before installing the two bundles compiled above you need to install
jettison, you can do this with

    start("mvn:org.codehaus.jettison/jettison/1.3")

## Usage

On the Clerezza console

  zz>import ch.eonum.health.locator.server._
  zz>import java.io._ 
  zz>val f = new File("/path/to/adderesses.csv")
  zz>val i = $[Importer]
  zz>i.importFile(f)


