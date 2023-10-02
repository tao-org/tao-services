package ro.cs.tao.services.commons;

import org.apache.commons.compress.utils.IOUtils;
import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.methods.RequestBuilder;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.impl.client.CloseableHttpClient;
import org.springframework.security.core.context.SecurityContextHolder;
import ro.cs.tao.configuration.ConfigurationManager;
import ro.cs.tao.docker.ContainerInstance;
import ro.cs.tao.persistence.ContainerInstanceProvider;
import ro.cs.tao.utils.WrappedCloseableHttpClient;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Enumeration;
import java.util.List;
import java.util.logging.Logger;

public abstract class ContainerTunnelFilter implements TunnelFilter {

    protected static ContainerInstanceProvider containerInstanceProvider;
    protected Logger logger = Logger.getLogger(TunnelFilter.class.getName());

    public static void setContainerInstanceProvider(ContainerInstanceProvider provider) {
        containerInstanceProvider = provider;
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        final HttpServletRequest httpServletRequest = (HttpServletRequest) request;
        final HttpServletResponse httpServletResponse = (HttpServletResponse) response;
        final URI uri = URI.create((httpServletRequest).getRequestURI());
        String path = uri.getPath();
        if (filterExpression().matcher(path).find()) {
            final String principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal().toString();
            final List<ContainerInstance> instances = containerInstanceProvider.getByUserId(principal);
            if (instances != null && !instances.isEmpty()) {
                String servicesPath = ConfigurationManager.getInstance().getValue("tao.services.base");
                if (servicesPath.endsWith("/")) {
                    servicesPath = servicesPath.substring(0, servicesPath.length() - 1);
                }
                final ContainerInstance instance = instances.get(0);
                final String newPath = servicesPath + ":" + instance.getPort() + httpServletRequest.getServletPath();
                try {
                    HttpUriRequest proxiedRequest = createHttpUriRequest(httpServletRequest,
                                                                         newPath,
                                                                         httpServletRequest.getQueryString());

                    try (final CloseableHttpClient client = WrappedCloseableHttpClient.createHttpClient(new BasicCookieStore(), null)) {
                        HttpResponse proxiedResponse = client.execute(proxiedRequest);
                        writeToResponse(proxiedResponse, httpServletResponse);
                    }
                } catch (URISyntaxException | IOException e) {
                    logger.warning(e.getMessage());
                }
            }
            chain.doFilter(httpServletRequest, httpServletResponse);
        }
        chain.doFilter(request, response);
    }

    protected HttpUriRequest createHttpUriRequest(HttpServletRequest request, String baseUrl, String query) throws URISyntaxException {
        final URI uri = new URI(baseUrl + (query != null ? "?" + query : ""));
        final RequestBuilder rb = RequestBuilder.create(request.getMethod());
        rb.setUri(uri);
        final Enumeration<String> headerNames = request.getHeaderNames();
        while(headerNames.hasMoreElements()){
            String headerName = headerNames.nextElement();
            if(!headerName.equalsIgnoreCase("accept-encoding")) {
                rb.addHeader(headerName, request.getHeader(headerName));
            }
        }
        return rb.build();
    }

    protected void writeToResponse(HttpResponse proxiedResponse, HttpServletResponse response) throws IOException {
        for (Header header : proxiedResponse.getAllHeaders()) {
            if ((! header.getName().equals("Transfer-Encoding")) || (! header.getValue().equals("chunked"))) {
                response.addHeader(header.getName(), header.getValue());
            }
        }
        try (InputStream is = proxiedResponse.getEntity().getContent();
             OutputStream os = response.getOutputStream()) {
            IOUtils.copy(is, os);
        }
    }
}
