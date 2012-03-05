package ch.eonum.health.locator.server;

import ch.eonum.health.locator.server.ontologies.ADDRESSES;
import ch.eonum.health.locator.server.ontologies.WGS84_POS;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.clerezza.rdf.core.BNode;
import org.apache.clerezza.rdf.core.MGraph;
import org.apache.clerezza.rdf.core.UriRef;
import org.apache.clerezza.rdf.core.access.NoSuchEntityException;
import org.apache.clerezza.rdf.core.access.TcManager;
import org.apache.clerezza.rdf.ontologies.RDFS;
import org.apache.clerezza.rdf.utils.GraphNode;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;

/**
 *
 * @author reto
 */
@Component
@Service(Importer.class)
public class Importer {
	
	@Reference
	private TcManager tcm;
	private UriRef DATA_GRAPH_URI = new UriRef("http://ontologies.eonum.ch/health-locator-data5");
	
	public void importFile(File file) throws IOException {
		final FileInputStream fileInputStream = new FileInputStream(file);
		final InputStreamReader inputStreamReader = new InputStreamReader(fileInputStream, "iso-8859-1");
		//final FileReader fileReader = new FileReader(file);
		final BufferedReader in = new BufferedReader(inputStreamReader);
		final MGraph dataGraph = getDataGraph();
		int abortCounter = 0;
		for (String line = in.readLine(); line != null; line = in.readLine()) {
			if (abortCounter++ > 10) {
				break;
			}
			final GraphNode entry = new GraphNode(new BNode(), dataGraph);
			entry.addPropertyValue(RDFS.comment, line);
			final String[] tokens = line.replace('|',';').split(";");
			/* Wey Andreas|Dr. med. Facharzt FMH für Allgemeinmedizin|
			 * Bürenstrasse 2, 3296 Arch|awey@bluewin.ch|be|allgemeinaerzte */
			entry.addPropertyValue(ADDRESSES.name, tokens[0]);
			entry.addPropertyValue(ADDRESSES.description, tokens[1]);
			entry.addPropertyValue(ADDRESSES.address, tokens[2]);
			if (!tokens[3].equals("")) entry.addPropertyValue(ADDRESSES.email, tokens[3]);
			entry.addPropertyValue(ADDRESSES.canton, tokens[4]);
			entry.addPropertyValue(ADDRESSES.category, tokens[5]);
			addGeoPos(entry);
		}
		
	}

	private MGraph getDataGraph() {
		try {
			return tcm.getMGraph(DATA_GRAPH_URI);
		} catch (NoSuchEntityException e) {
			return tcm.createMGraph(DATA_GRAPH_URI);
		}
	}

	private void addGeoPos(GraphNode entry) {
		/* http://maps.googleapis.com/maps/api/geocode/json?sensor=false&address=Tscharnerstrasse%2041,%20Bern*/
		final String address = entry.getLiterals(ADDRESSES.address).next().getLexicalForm();
		try {
			final String uriString = "http://maps.googleapis.com/maps/api/"
					+ "geocode/json?sensor=false&address="+URLEncoder.encode(address, "utf-8");
			final URL uri = new URL(uriString);
			final InputStream in = uri.openStream();
			final JSONObject jsonObject = parseJSon(in);
			/* {"lat":46.4916335,"lng":7.557490499999999}*/
			final JSONObject location = jsonObject.getJSONArray("results")
					.getJSONObject(0).getJSONObject("geometry").getJSONObject("location");
			final String lat = location.getString("lat");
			final String long_ = location.getString("lng");
			entry.addPropertyValue(WGS84_POS.lat, lat);
			entry.addPropertyValue(WGS84_POS.long_, long_);
		} catch (JSONException ex) {
			Logger.getLogger(Importer.class.getName()).log(Level.SEVERE, null, ex);
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
