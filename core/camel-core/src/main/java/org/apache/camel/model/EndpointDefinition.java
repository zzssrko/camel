package org.apache.camel.model;

import java.util.HashMap;
import java.util.Map;

import javax.xml.bind.annotation.XmlTransient;

import org.apache.camel.CamelContext;
import org.apache.camel.Component;
import org.apache.camel.Endpoint;
import org.apache.camel.NoSuchEndpointException;
import org.apache.camel.spi.RouteContext;

@XmlTransient
public class EndpointDefinition<T extends EndpointDefinition> {

    protected final String scheme;
    protected final String path;
    protected final Map<String, Object> properties = new HashMap<>();

    public EndpointDefinition(String scheme, String path) {
        this.scheme = scheme;
        this.path = path;
    }

    public Endpoint resolve(RouteContext context) throws NoSuchEndpointException {
        return context.resolveEndpoint(scheme, path, properties);
    }

    public interface Consumer {
        Endpoint resolve(RouteContext context) throws NoSuchEndpointException;
    }

    public interface Producer {
        Endpoint resolve(RouteContext context) throws NoSuchEndpointException;
    }

}
