package ro.cs.tao.services;

import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.methods.RequestBuilder;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.impl.client.CloseableHttpClient;
import org.h2.util.IOUtils;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.http.server.ServletServerHttpResponse;
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

public class RedirectFilter implements javax.servlet.Filter {

    private static ContainerInstanceProvider containerInstanceProvider;

    public static void setContainerInstanceProvider(ContainerInstanceProvider provider) {
        containerInstanceProvider = provider;
    }

    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain) throws IOException, ServletException {
        ServletServerHttpRequest request = new ServletServerHttpRequest((HttpServletRequest) servletRequest);
        ServletServerHttpResponse response = new ServletServerHttpResponse((HttpServletResponse) servletResponse);
        final URI uri = request.getURI();
        String path = uri.getPath();
        if (path.startsWith("/lab") || path.startsWith("/static/lab") || path.startsWith("/api")) {
            final String principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal().toString();
            final List<ContainerInstance> instances = containerInstanceProvider.getByUserId(principal);
            if (instances != null && !instances.isEmpty()) {
                String servicesPath = ConfigurationManager.getInstance().getValue("tao.services.base");
                if (servicesPath.endsWith("/")) {
                    servicesPath = servicesPath.substring(0, servicesPath.length() - 1);
                }
                final ContainerInstance instance = instances.get(0);
                final String newPath = servicesPath + ":" + instance.getPort() + request.getServletRequest().getServletPath();
                try {
                    HttpUriRequest proxiedRequest = createHttpUriRequest(request.getServletRequest(),
                                                                         newPath,
                                                                         request.getServletRequest().getQueryString());

                    try (final CloseableHttpClient client = WrappedCloseableHttpClient.createHttpClient(new BasicCookieStore(), null)) {
                        HttpResponse proxiedResponse = client.execute(proxiedRequest);
                        writeToResponse(proxiedResponse, response.getServletResponse());
                    }
                } catch (URISyntaxException | IOException e) {
                    e.printStackTrace();
                }
            }
            filterChain.doFilter(request.getServletRequest(), response.getServletResponse());
        }
        filterChain.doFilter(servletRequest, servletResponse);
    }

    private HttpUriRequest createHttpUriRequest(HttpServletRequest request, String baseUrl, String query) throws URISyntaxException {
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

    private void writeToResponse(HttpResponse proxiedResponse, HttpServletResponse response) throws IOException {
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
