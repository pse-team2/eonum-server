package ch.eonum.health.locator.server;

import ch.eonum.health.locator.server.ontologies.ADDRESSES;
import ch.eonum.health.locator.server.ontologies.WGS84_POS;
import java.net.URISyntaxException;
import java.util.Iterator;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import org.apache.clerezza.platform.typerendering.scalaserverpages.ScalaServerPagesService;
import org.apache.clerezza.rdf.core.BNode;
import org.apache.clerezza.rdf.core.Literal;
import org.apache.clerezza.rdf.core.MGraph;
import org.apache.clerezza.rdf.core.NonLiteral;
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

/**
 *
 * @author reto
 */
@Component
@Service(value=Object.class)
@Property(name="javax.ws.rs", boolValue=true)
@Path("finder")
public class AddressFinder {
	
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
	public GraphNode entry(@QueryParam("long") Double long_, @QueryParam("lat") Double lat) {
		final TripleCollection data = getDataGraph();
		final Double minLong = long_ - 0.1; //that's 71.38 Km
		final Double maxLong = long_ + 0.1;
		final Double minLat = lat - 0.1;
		final Double maxLat = lat + 0.1;
		final MGraph resultGraph = new UnionMGraph(new SimpleMGraph(), data);
		final GraphNode result = new GraphNode(new BNode(), resultGraph);
		result.addProperty(RDF.type,  ADDRESSES.AddressList);
		final RdfList resultList = new RdfList(result);
		final Iterator<Triple> triples  = data.filter(null, RDF.type, ADDRESSES.Address);
		while (triples.hasNext()) {
			NonLiteral address = triples.next().getSubject();
			if (isInRange(new GraphNode(address, data),
					minLong, maxLong, minLat, maxLat)) {
				resultList.add(address);
			}
		}
		return result;
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
			double long_ = Double.parseDouble(((Literal)
					graphNode.getObjects(WGS84_POS.long_).next()).getLexicalForm());
			double lat = Double.parseDouble(((Literal)
					graphNode.getObjects(WGS84_POS.lat).next()).getLexicalForm());
			return (long_ > minLong) && (long_ < maxLong) && (lat > minLat) && (lat < maxLat);
		} catch (Exception e) {
			logger.debug("Exception geeting location of address: "+e);
			return false;
		}
	}


	
}
