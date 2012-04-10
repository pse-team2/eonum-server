package ch.eonum.health.locator.server;

import ch.eonum.health.locator.server.ontologies.ADDRESSES;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.QueryParam;
import org.apache.clerezza.rdf.core.Literal;
import org.apache.clerezza.rdf.core.MGraph;
import org.apache.clerezza.rdf.core.NonLiteral;
import org.apache.clerezza.rdf.core.Triple;
import org.apache.clerezza.rdf.core.TripleCollection;
import org.apache.clerezza.rdf.core.UriRef;
import org.apache.clerezza.rdf.core.access.NoSuchEntityException;
import org.apache.clerezza.rdf.core.access.TcManager;
import org.apache.clerezza.rdf.ontologies.RDF;
import org.apache.clerezza.rdf.utils.GraphNode;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Delivers a list of categories.
 */
@Component
@Service(value = Object.class)
@Property(name = "javax.ws.rs", boolValue = true)
@Path("categories")
public class ListCategories {

	private static final Logger logger = LoggerFactory.getLogger(ListCategories.class);

	@Reference
	private TcManager tcm;
	private UriRef DATA_GRAPH_URI = new UriRef("http://ontologies.eonum.ch/health-locator-data");

	/**
	 * Returns a json object with a single value 'categories' pointing to all 
	 * available categories
	 */
	@GET
	public JSONObject entry(@QueryParam("long") final Double long_, @QueryParam("lat") final Double lat) {
		JSONObject result=new JSONObject();
		JSONArray categoriesJson = AccessController.doPrivileged(new PrivilegedAction<JSONArray>() {

			@Override
			public JSONArray run() {
				final TripleCollection data = getDataGraph();
				Set<String> categories = new HashSet<String>();
				final Iterator<Triple> triples = data.filter(null, RDF.type, ADDRESSES.Address);
				while (triples.hasNext()) {
					NonLiteral address = triples.next().getSubject();
					GraphNode graphNode = new GraphNode(address, data);
					Iterator<Literal> categoryIter = graphNode.getLiterals(ADDRESSES.category);
					while (categoryIter.hasNext()) {
						categories.add(categoryIter.next().getLexicalForm());
					}
				}
				JSONArray result = new JSONArray();
				result.addAll(categories);
				return result;
			}
		});
		result.put("categories", categoriesJson);
		return result;

	}

	private MGraph getDataGraph() {
		try {
			return tcm.getMGraph(DATA_GRAPH_URI);
		} catch (NoSuchEntityException e) {
			return tcm.createMGraph(DATA_GRAPH_URI);
		}
	}


}
