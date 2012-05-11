package ch.eonum.health.locator.server;

import java.net.URISyntaxException;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.Iterator;

import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.apache.clerezza.platform.typerendering.scalaserverpages.ScalaServerPagesService;
import org.apache.clerezza.rdf.core.BNode;
import org.apache.clerezza.rdf.core.Literal;
import org.apache.clerezza.rdf.core.MGraph;
import org.apache.clerezza.rdf.core.NonLiteral;
import org.apache.clerezza.rdf.core.Resource;
import org.apache.clerezza.rdf.core.Triple;
import org.apache.clerezza.rdf.core.TripleCollection;
import org.apache.clerezza.rdf.core.UriRef;
import org.apache.clerezza.rdf.core.access.NoSuchEntityException;
import org.apache.clerezza.rdf.core.access.TcManager;
import org.apache.clerezza.rdf.core.impl.SimpleMGraph;
import org.apache.clerezza.rdf.ontologies.RDF;
import org.apache.clerezza.rdf.utils.GraphNode;
import org.apache.clerezza.rdf.utils.RdfList;
import org.apache.clerezza.rdf.utils.UnionMGraph;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.eonum.health.locator.server.ontologies.ADDRESSES;
import ch.eonum.health.locator.server.ontologies.WGS84_POS;

/**
 *
 * @author reto
 */
@Component
@Service(value = Object.class)
@Property(name = "javax.ws.rs", boolValue = true)
@Path("finder")
public class AddressFinder {
	
	//final static int MAX_RESULTS = 100;

	private static final Logger logger = LoggerFactory.getLogger(AddressFinder.class);
	@Reference
	private ScalaServerPagesService scalaServerPagesService;
	@Reference
	private TcManager tcm;
	private UriRef DATA_GRAPH_URI = new UriRef("http://ontologies.eonum.ch/health-locator-data");

	public void activate(ComponentContext context) throws URISyntaxException {
		scalaServerPagesService.registerScalaServerPage(getClass().getResource(
				"addresses-json.ssp"), ADDRESSES.AddressList, null,
				MediaType.APPLICATION_JSON_TYPE, context.getBundleContext());
	}

	/**
	 * this currently uses a fixed size square of side 2*71.38 km, of which the
	 * passed point is is in the center
	 */
	@GET
	@Produces(MediaType.APPLICATION_JSON)
	public GraphNode entry(@QueryParam("long1") final Double long1, @QueryParam("lat1") final Double lat1,
			@QueryParam("long2") final Double long2, @QueryParam("lat2") final Double lat2,
			@QueryParam("category") final String category, @QueryParam("count") @DefaultValue("20") final int count) {
		if ((lat1 == null) || (lat2 == null) || (long1 == null) || (long2 == null)) {
			throw new WebApplicationException(Response.status(400).entity(
					"lat1, lat2, long1 and long2 are required query arguments").build());
		}
		return AccessController.doPrivileged(new PrivilegedAction<GraphNode>() {

			@Override
			public GraphNode run() {
				final TripleCollection data = getDataGraph();
				final Double minLong = Math.min(long1, long2);
				final Double maxLong = Math.max(long1, long2);
				final Double minLat = Math.min(lat1, lat2);
				final Double maxLat = Math.max(lat1, lat2);
				final MGraph resultGraph = new UnionMGraph(new SimpleMGraph(), data);
				final GraphNode result = new GraphNode(new BNode(), resultGraph);
				result.addProperty(RDF.type, ADDRESSES.AddressList);
				final RdfList resultList = new RdfList(result);
				final Iterator<Triple> triples = data.filter(null, RDF.type, ADDRESSES.Address);
				int resultCount = 0;
				while (triples.hasNext()) {
					NonLiteral address = triples.next().getSubject();
					GraphNode graphNode = new GraphNode(address, data); 
					if (((category == null) || matchCategory(graphNode, category)) && isInRange(graphNode,
							minLong, maxLong, minLat, maxLat)) {
						resultList.add(address);
						if (resultList.size() >= count) {
							break;
						}
					}
				}
				return result;
			}

		});

	}

	private MGraph getDataGraph() {
		try {
			return tcm.getMGraph(DATA_GRAPH_URI);
		} catch (NoSuchEntityException e) {
			return tcm.createMGraph(DATA_GRAPH_URI);
		}
	}

	private boolean isInRange(GraphNode graphNode, double minLong, double maxLong, double minLat, double maxLat) {
		try {
			double long_ = Double.parseDouble(((Literal) graphNode.getObjects(WGS84_POS.long_).next()).getLexicalForm());
			double lat = Double.parseDouble(((Literal) graphNode.getObjects(WGS84_POS.lat).next()).getLexicalForm());
			return (long_ > minLong) && (long_ < maxLong) && (lat > minLat) && (lat < maxLat);
		} catch (Exception e) {
			logger.debug("Exception geeting location of address: " + e);
			return false;
		}
	}
	
	private boolean matchCategory(GraphNode graphNode, String category) {
		Iterator<Resource> categories = graphNode.getObjects(ADDRESSES.category);
		while(categories.hasNext()) {
			if (category.equals(((Literal)categories.next()).getLexicalForm())) {
				return true;
			}
		}
		
		return false;
	}
}
