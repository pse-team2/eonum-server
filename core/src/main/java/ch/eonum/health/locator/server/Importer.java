package ch.eonum.health.locator.server;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.locks.Lock;

import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.UriInfo;

import org.apache.clerezza.rdf.core.BNode;
import org.apache.clerezza.rdf.core.MGraph;
import org.apache.clerezza.rdf.core.NonLiteral;
import org.apache.clerezza.rdf.core.Triple;
import org.apache.clerezza.rdf.core.UriRef;
import org.apache.clerezza.rdf.core.access.LockableMGraph;
import org.apache.clerezza.rdf.core.access.NoSuchEntityException;
import org.apache.clerezza.rdf.core.access.TcManager;
import org.apache.clerezza.rdf.core.impl.TripleImpl;
import org.apache.clerezza.rdf.ontologies.RDF;
import org.apache.clerezza.rdf.ontologies.RDFS;
import org.apache.clerezza.rdf.utils.GraphNode;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;

import ch.eonum.health.locator.server.ontologies.ADDRESSES;
import ch.eonum.health.locator.server.ontologies.WGS84_POS;

/**
 *
 * @author reto
 */
@Component
@Service({Importer.class, Object.class})
@Property(name = "javax.ws.rs", boolValue = true)
@Path("eonum/manager")
public class Importer {
	
	private Map<String, FixJob> fixJobIdMap = new HashMap<String, FixJob>();
	
	@Reference
	private TcManager tcm;
	private UriRef DATA_GRAPH_URI = new UriRef("http://ontologies.eonum.ch/health-locator-data");
	
	public String version() {
		return "0.1-1";
	}
	
	@GET
	@Produces("text/html")
	public String entry() {
		StringWriter writer = new StringWriter();
		PrintWriter out = new PrintWriter(writer);
		out.println("<html>");
		out.println("<form method=\"POST\" action=\"manager/geofix\"><input type=\"submit\" value=\"geofix\"></form>");
		out.println("</html>");
		return writer.toString();
	}
	
	@POST
	@Path("geofix")
	public FixJob geofix(@Context UriInfo uriInfo) throws MalformedURLException {
		synchronized (fixJobIdMap) {
			String id = Integer.toString(fixJobIdMap.size());
			URL jobUrl = new URL(uriInfo.getAbsolutePath().toURL(), "fixjob/"+id);
			FixJob result = new FixJob(jobUrl, getDataGraph());
			fixJobIdMap.put(id, result);
			return result;
		}
	}
	
	@GET
	@Path("fixjob/{id}")
	public FixJob job(@PathParam("id") String id) {
		synchronized (fixJobIdMap) {
			return fixJobIdMap.get(id);
		}
	}
	
	
	public void importFile(File file) throws IOException {
		final FileInputStream fileInputStream = new FileInputStream(file);
		final InputStreamReader inputStreamReader = new InputStreamReader(fileInputStream, "iso-8859-1");
		//final FileReader fileReader = new FileReader(file);
		final BufferedReader in = new BufferedReader(inputStreamReader);
		final MGraph dataGraph = getDataGraph();
		//int abortCounter = 0;
		for (String line = in.readLine(); line != null; line = in.readLine()) {
			/*if (abortCounter++ > 10) {
				break;
			}*/
			final GraphNode entry = new GraphNode(new BNode(), dataGraph);
			entry.addPropertyValue(RDFS.comment, line);
			final String[] tokens = line.replace('|',';').split(";");
			/* Wey Andreas|Dr. med. Facharzt FMH für Allgemeinmedizin|
			 * Bürenstrasse 2, 3296 Arch|awey@bluewin.ch|be|allgemeinaerzte */
			entry.addPropertyValue(ADDRESSES.name, tokens[0]);
			entry.addPropertyValue(ADDRESSES.description, tokens[1]);
			entry.addPropertyValue(ADDRESSES.address, tokens[2]);
			if (!tokens[3].equals("")) entry.addPropertyValue(ADDRESSES.email, tokens[3]);
			entry.addPropertyValue(ADDRESSES.tel, tokens[4]);
			entry.addPropertyValue(ADDRESSES.fax, tokens[5]);
			entry.addPropertyValue(ADDRESSES.canton, tokens[6]);
			entry.addPropertyValue(ADDRESSES.category, tokens[7]);
			entry.addProperty(RDF.type, ADDRESSES.Address);
			addGeoPos(entry);
		}
	}
	
	/**
	 * Considering name and address as combined inverse functional property
	 */
	public void smush() {
		final Map<String, Set<GraphNode>> equalityMap = new HashMap<String, Set<GraphNode>>();
		final LockableMGraph dataGraph = getDataGraph();
		Lock l = dataGraph.getLock().writeLock();
		l.lock();
		try {
			final Iterator<Triple> triples  = dataGraph.filter(null, RDF.type, ADDRESSES.Address);
			while (triples.hasNext()) {
				final GraphNode gn = new GraphNode(triples.next().getSubject(), dataGraph);
				final String name = gn.getLiterals(ADDRESSES.name).next().getLexicalForm();
				final String address = gn.getLiterals(ADDRESSES.address).next().getLexicalForm();
				final String key = name+address;
				final Set<GraphNode> set = equalityMap.containsKey(key)? equalityMap.get(key) : new HashSet<GraphNode>();
				set.add(gn);
				equalityMap.put(key, set);
			}
		} finally {
			l.unlock();
		}
		for (Set<GraphNode> equalitySet : equalityMap.values()) {
			final Iterator<GraphNode> iter = equalitySet.iterator();
			final GraphNode first = iter.next();
			while(iter.hasNext()) {
				iter.next().replaceWith((NonLiteral)first.getNode());
			}
		}
	}
	
	public void addType() {
		final LockableMGraph dataGraph = getDataGraph();
		Lock l = dataGraph.getLock().writeLock();
		l.lock();
		try {
			final Iterator<Triple> triples  = dataGraph.filter(null, ADDRESSES.name, null);
			final Collection<NonLiteral> addresses = new HashSet<NonLiteral>();
			while (triples.hasNext()) {
				NonLiteral address = triples.next().getSubject();
				addresses.add(address);
			}
			for (NonLiteral address : addresses) {
				dataGraph.add(new TripleImpl(address, RDF.type, ADDRESSES.Address));
			}
		} finally {
			l.unlock();
		}
	}

	private LockableMGraph getDataGraph() {
		try {
			return tcm.getMGraph(DATA_GRAPH_URI);
		} catch (NoSuchEntityException e) {
			return tcm.createMGraph(DATA_GRAPH_URI);
		}
	}

	static void addGeoPos(GraphNode entry) {
		/* http://maps.googleapis.com/maps/api/geocode/json?sensor=false&address=Tscharnerstrasse%2041,%20Bern*/
		final String address = entry.getLiterals(ADDRESSES.address).next().getLexicalForm();
		try {
			final String uriString = "http://maps.googleapis.com/maps/api/"
					+ "geocode/json?sensor=false&address="+URLEncoder.encode(address, "utf-8");
			final URL uri = new URL(uriString);
			final InputStream in = uri.openStream();
			final JSONObject jsonObject = parseJSon(in);
			/* {"lat":46.4916335,"lng":7.557490499999999}*/
			//System.out.println(jsonObject.toString(2));
			String status = (String) jsonObject.get("status");
			//System.out.println(status);
			if (status.equals("OVER_QUERY_LIMIT")) {
				throw new RuntimeException("over query limit");
			}
			if (status.equals("OK")) {
				final JSONObject location = jsonObject.getJSONArray("results")
						.getJSONObject(0).getJSONObject("geometry").getJSONObject("location");
				final String lat = location.getString("lat");
				final String long_ = location.getString("lng");
				entry.addPropertyValue(WGS84_POS.lat, lat);
				entry.addPropertyValue(WGS84_POS.long_, long_);
			} else {
				throw new UnknownStatusException(status);
			}
		} catch (JSONException ex) {
			throw new RuntimeException(ex);
		} catch (IOException ex) {
			throw new RuntimeException(ex);
		}
	}
	
	private static JSONObject parseJSon(InputStream in) throws IOException, JSONException {
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		for (int ch = in.read(); ch != -1; ch = in.read()) {
			baos.write(ch);
		}
		in.close();
		JSONObject result;
		String jsonString = new String(baos.toByteArray(), "utf-8");
		//System.out.println("jsonString: "+jsonString);
		try {
			result = new JSONObject(jsonString);
		} catch (JSONException ex) {
			throw new RuntimeException(ex);
		}
		return result;
	}
	
}
