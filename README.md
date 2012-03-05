= Health Service Locator

This is to become a service to locate health care facilities.

== Current functionality

Partially implemented: import tool


== installation
start("mvn:org.codehaus.jettison/jettison/1.3")

=== Usage

On the Clerezza console

  zz>import ch.eonum.health.locator.server._
  zz>import java.io._ 
  zz>val f = new File("/path/to/adderesses.csv")
  zz>val i = $[Importer]
  zz>i.importFile(f)

import ch.eonum.health.locator.server._
import java.io._ 
val f = new File("/home/reto/workspace/pse/aerzte_adresse_bern.csv")
val i = $[Importer]
i.importFile(f)

