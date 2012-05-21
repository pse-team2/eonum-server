package ch.eonum.health.locator.server;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;

import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.MessageBodyWriter;
import javax.ws.rs.ext.Provider;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Service;

@Component
@Service(Object.class)
@Property(name = "javax.ws.rs", boolValue = true)
@Provider
@Produces({"text/plain", "*/*"})
public class FixJobWriter implements MessageBodyWriter<FixJob> {

	@Override
	public boolean isWriteable(Class<?> type, Type genericType,
			Annotation[] annotations, MediaType mediaType) {
		if (!FixJob.class.isAssignableFrom(type)) {
			System.out.println("refuting "+type);
			return false;
		} else {
			System.out.println("acception "+type);
			return true;
		}
	}

	@Override
	public long getSize(FixJob t, Class<?> type, Type genericType,
			Annotation[] annotations, MediaType mediaType) {
		return -1;
	}

	@Override
	public void writeTo(FixJob t, Class<?> type, Type genericType,
			Annotation[] annotations, MediaType mediaType,
			MultivaluedMap<String, Object> httpHeaders,
			OutputStream entityStream) throws IOException,
			WebApplicationException {
		PrintWriter out = new PrintWriter(entityStream);
		out.println("Job "+t.getJobUrl());
		out.println("Start: "+t.getStartDate());
		if (t.getEndDate() != null) out.println("End: "+t.getEndDate());
		if (t.getAbortDate() != null) out.println("Aborted at: "+t.getEndDate());
		out.print(t.getLog());
		out.flush();
		
	}

}
