package ch.eonum.health.locator.server;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.URL;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.locks.Lock;

import org.apache.clerezza.rdf.core.Triple;
import org.apache.clerezza.rdf.core.access.LockableMGraph;
import org.apache.clerezza.rdf.ontologies.RDF;
import org.apache.clerezza.rdf.utils.GraphNode;

import ch.eonum.health.locator.server.ontologies.ADDRESSES;
import ch.eonum.health.locator.server.ontologies.WGS84_POS;

public class FixJob {

	private URL jobUrl;
	private LockableMGraph dataGraph;
	private StringWriter logBuffer = new StringWriter();
	private PrintWriter log = new PrintWriter(logBuffer);
	private Date startDate, endDate, abortDate;
	
	

	private class JobThread extends Thread {
		@Override
		public void run() {
			Set<GraphNode> toFix = new HashSet<GraphNode>();
			Lock l = dataGraph.getLock().readLock();
			l.lock();
			try {
				Iterator<Triple> addressTypeTriples = dataGraph.filter(null, RDF.type, ADDRESSES.Address);
				while (addressTypeTriples.hasNext()) {
					GraphNode address = new GraphNode(addressTypeTriples.next().getSubject(), dataGraph);
					if (!address.hasProperty(WGS84_POS.lat, null)) {
						log.println("Scheduling for process: "+address.getLiterals(ADDRESSES.name).next().getLexicalForm());
						toFix.add(address);
					}
				}
			} finally {
				l.unlock();
			}
			int exceptionAllowed = 3;
			for (GraphNode address : toFix) {
				try {
					log.println("Procession "+address);
					Importer.addGeoPos(address);
					log.println("Las is now:  "+address.getLiterals(WGS84_POS.lat).next().getLexicalForm());
					Thread.sleep(1000);
				} catch (UnknownStatusException e) {
					log.println("Uknown Status: "+e.getStatus());
				} catch (Exception e) {
					log.println("Ups: ");
					e.printStackTrace(log);
					log.println();
					if (exceptionAllowed-- == 0) {
						abortDate = new Date();
						return;
					}
				}
			}
			endDate = new Date();
			
		}
	}
	
	public FixJob(URL jobUrl, LockableMGraph dataGraph) {
		this.jobUrl = jobUrl;
		this.dataGraph = dataGraph;
		this.startDate = new Date();
		Thread thread = new JobThread();
		thread.start();
	}

	public URL getJobUrl() {
		return jobUrl;
	}
	
	public Date getStartDate() {
		return startDate;
	}

	public Date getEndDate() {
		return endDate;
	}
	
	public Date getAbortDate() {
		return abortDate;
	}
	
	public String getLog() {
		return logBuffer.toString();
	}

}
