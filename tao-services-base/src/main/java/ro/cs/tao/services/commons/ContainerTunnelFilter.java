package ro.cs.tao.services.commons;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.*;
import org.apache.commons.compress.utils.IOUtils;
import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.methods.RequestBuilder;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.impl.client.CloseableHttpClient;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import ro.cs.tao.configuration.ConfigurationManager;
import ro.cs.tao.docker.ContainerInstance;
import ro.cs.tao.persistence.ContainerInstanceProvider;
import ro.cs.tao.utils.WrappedCloseableHttpClient;

import javax.net.ssl.*;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.security.Principal;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * An abstract implementation of the {@link TunnelFilter} interface, designed to filter incoming
 * requests in a containerized tunneling environment. It processes HTTP requests and routes them
 * to the appropriate container instance based on the user context.
 *
 * This class provides common functionality, such as logging, configuration management, and
 * proxying HTTP requests to target container instances. Concrete implementations are expected
 * to define the specific filter expression through the {@link #filterExpression()} method.
 */
public abstract class ContainerTunnelFilter implements TunnelFilter {

    public static final String TUNNEL_INSTANCE_PARAMETER = "taoContainerInstanceId";
    protected static ContainerInstanceProvider containerInstanceProvider;
    private static final ExecutorService WEB_SOCKET_EXECUTOR = Executors.newCachedThreadPool(r -> {
        Thread thread = new Thread(r, "container-tunnel-websocket");
        thread.setDaemon(true);
        return thread;
    });
    protected static final String staticPrefix = ConfigurationManager.getInstance().getValue("site.static.prefix", "/ui");
    protected Logger logger = Logger.getLogger(TunnelFilter.class.getName());

    public static void setContainerInstanceProvider(ContainerInstanceProvider provider) {
        containerInstanceProvider = provider;
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        final HttpServletRequest httpServletRequest = (HttpServletRequest) request;
        final HttpServletResponse httpServletResponse = (HttpServletResponse) response;
        String path = requestPath(httpServletRequest);
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && !path.startsWith(staticPrefix) && filterExpression().matcher(path).find()) {
            final String userId = userId(authentication);
            if (tunnel(httpServletRequest, httpServletResponse, userId)) {
                return;
            }
        }
        chain.doFilter(request, response);
    }

    public boolean tunnel(HttpServletRequest request, HttpServletResponse response, String userId) throws IOException, ServletException {
        final ContainerInstance instance = containerInstance(request, userId);
        if (instance != null) {
            final String newPath = targetUrl(instance, request);
            try {
                if (isWebSocketUpgrade(request)) {
                    proxyWebSocket(request, response, newPath);
                    return true;
                }
                HttpUriRequest proxiedRequest = createHttpUriRequest(request,
                                                                     newPath,
                                                                     forwardedQueryString(request));

                try (final CloseableHttpClient client = WrappedCloseableHttpClient.createHttpClient(new BasicCookieStore(), null)) {
                    HttpResponse proxiedResponse = client.execute(proxiedRequest);
                    writeToResponse(proxiedResponse, response);
                }
                return true;
            } catch (URISyntaxException | IOException e) {
                if (isConnectionAbort(e)) {
                    return true;
                } else {
                    logger.warning(e.getMessage());
                }
            }
        }
        return false;
    }

    protected ContainerInstance containerInstance(HttpServletRequest request, String userId) {
        final String requestedInstanceId = tunnelInstanceId(request);
        if (requestedInstanceId != null && !requestedInstanceId.isEmpty()) {
            final ContainerInstance requestedInstance = containerInstanceProvider.get(requestedInstanceId);
            if (requestedInstance != null && userId.equals(requestedInstance.getUserId())) {
                return requestedInstance;
            }
        }
        final List<ContainerInstance> instances = containerInstanceProvider.getByUserId(userId);
        return instances != null && !instances.isEmpty() ? instances.get(0) : null;
    }

    private String tunnelInstanceId(HttpServletRequest request) {
        String instanceId = request.getParameter(TUNNEL_INSTANCE_PARAMETER);
        if ((instanceId == null || instanceId.isEmpty()) && request.getCookies() != null) {
            for (Cookie cookie : request.getCookies()) {
                if (TUNNEL_INSTANCE_PARAMETER.equals(cookie.getName())) {
                    instanceId = cookie.getValue();
                    break;
                }
            }
        }
        return instanceId;
    }

    protected String userId(Authentication authentication) {
        final Object principal = authentication.getPrincipal();
        return principal instanceof Principal ? ((Principal) principal).getName() : principal.toString();
    }

    protected String targetUrl(ContainerInstance instance, HttpServletRequest request) {
        String servicesPath = ConfigurationManager.getInstance().getValue("tao.services.base");
        if (servicesPath.endsWith("/")) {
            servicesPath = servicesPath.substring(0, servicesPath.length() - 1);
        }
        final URI servicesUri = URI.create(servicesPath);
        final StringBuilder builder = new StringBuilder();
        builder.append(targetScheme(servicesUri))
               .append("://")
               .append(targetHost(servicesUri))
               .append(":")
               .append(instance.getPort())
               .append(requestPath(request));
        return builder.toString();
    }

    protected String targetHost(URI servicesUri) {
        final String configuredHost = ConfigurationManager.getInstance().getValue("container.tunnel.host");
        if (configuredHost != null && !configuredHost.isBlank()) {
            return configuredHost;
        }
        final String servicesHost = servicesUri.getHost();
        return isLocalHost(servicesHost) ? "127.0.0.1" : servicesHost;
    }

    protected String targetScheme(URI servicesUri) {
        return servicesUri.getScheme();
    }

    private boolean isLocalHost(String host) {
        if (host == null || host.isBlank()) {
            return false;
        }
        if ("localhost".equalsIgnoreCase(host) || "127.0.0.1".equals(host) || "::1".equals(host)) {
            return true;
        }
        try {
            final InetAddress[] hostAddresses = InetAddress.getAllByName(host);
            final InetAddress[] localAddresses = InetAddress.getAllByName(InetAddress.getLocalHost().getHostName());
            for (InetAddress hostAddress : hostAddresses) {
                if (hostAddress.isAnyLocalAddress() || hostAddress.isLoopbackAddress()) {
                    return true;
                }
                for (InetAddress localAddress : localAddresses) {
                    if (hostAddress.equals(localAddress)) {
                        return true;
                    }
                }
            }
        } catch (UnknownHostException ignored) {
        }
        return false;
    }

    protected String requestPath(HttpServletRequest request) {
        String path = request.getRequestURI();
        final String contextPath = request.getContextPath();
        if (contextPath != null && !contextPath.isEmpty() && path.startsWith(contextPath)) {
            path = path.substring(contextPath.length());
        }
        if (path == null || path.isEmpty()) {
            path = request.getServletPath();
        }
        return path;
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

    protected String forwardedQueryString(HttpServletRequest request) {
        final String queryString = request.getQueryString();
        if (queryString == null || queryString.isEmpty()) {
            return null;
        }
        final String query = Arrays.stream(queryString.split("&"))
                                   .filter(parameter -> !isTunnelParameter(parameter))
                                   .collect(Collectors.joining("&"));
        return query.isEmpty() ? null : query;
    }

    private boolean isTunnelParameter(String parameter) {
        return "tokenKey".equals(parameter)
                || parameter.startsWith("tokenKey=")
                || TUNNEL_INSTANCE_PARAMETER.equals(parameter)
                || parameter.startsWith(TUNNEL_INSTANCE_PARAMETER + "=");
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

    protected boolean isWebSocketUpgrade(HttpServletRequest request) {
        final String upgrade = request.getHeader("Upgrade");
        return upgrade != null && "websocket".equalsIgnoreCase(upgrade);
    }

    protected void proxyWebSocket(HttpServletRequest request, HttpServletResponse response, String baseUrl)
            throws IOException, ServletException, URISyntaxException {
        final String queryString = forwardedQueryString(request);
        final URI targetUri = new URI(baseUrl + (queryString != null ? "?" + queryString : ""));
        Socket backendSocket = null;
        try {
            backendSocket = createSocket(targetUri);
            writeWebSocketHandshake(request, targetUri, backendSocket.getOutputStream());
            final WebSocketHandshakeResponse backendResponse = readWebSocketHandshakeResponse(backendSocket.getInputStream());
            if (backendResponse.statusCode != HttpServletResponse.SC_SWITCHING_PROTOCOLS) {
                closeQuietly(backendSocket);
                response.sendError(backendResponse.statusCode, backendResponse.statusMessage);
                return;
            }
            response.setStatus(HttpServletResponse.SC_SWITCHING_PROTOCOLS);
            for (Map.Entry<String, String> header : backendResponse.headers) {
                response.addHeader(header.getKey(), header.getValue());
            }
            final WebSocketProxyUpgradeHandler handler = request.upgrade(WebSocketProxyUpgradeHandler.class);
            handler.backendSocket = backendSocket;
            backendSocket = null;
        } finally {
            closeQuietly(backendSocket);
        }
    }

    private Socket createSocket(URI uri) throws IOException {
        final String scheme = uri.getScheme();
        final String host = uri.getHost();
        final int port = uri.getPort() > -1 ? uri.getPort() : ("https".equalsIgnoreCase(scheme) || "wss".equalsIgnoreCase(scheme) ? 443 : 80);
        if ("https".equalsIgnoreCase(scheme) || "wss".equalsIgnoreCase(scheme)) {
            final SSLSocket socket = (SSLSocket) trustAllSocketFactory().createSocket(host, port);
            socket.startHandshake();
            return socket;
        }
        return new Socket(host, port);
    }

    private SSLSocketFactory trustAllSocketFactory() throws IOException {
        try {
            final SSLContext ctx = SSLContext.getInstance("TLS");
            ctx.init(new KeyManager[0], new TrustManager[]{
                    new X509TrustManager() {
                        @Override
                        public void checkClientTrusted(X509Certificate[] certificates, String authType) {
                        }

                        @Override
                        public void checkServerTrusted(X509Certificate[] certificates, String authType) {
                        }

                        @Override
                        public X509Certificate[] getAcceptedIssuers() {
                            return new X509Certificate[0];
                        }
                    }},
                    new SecureRandom());
            return ctx.getSocketFactory();
        } catch (Exception e) {
            throw new IOException(e);
        }
    }

    private void writeWebSocketHandshake(HttpServletRequest request, URI targetUri, OutputStream outputStream) throws IOException {
        final StringBuilder builder = new StringBuilder();
        builder.append(request.getMethod()).append(" ")
               .append(targetUri.getRawPath())
               .append(targetUri.getRawQuery() != null ? "?" + targetUri.getRawQuery() : "")
               .append(" HTTP/1.1\r\n");
        builder.append("Host: ").append(targetUri.getHost()).append(":").append(targetUri.getPort()).append("\r\n");
        builder.append("Upgrade: websocket\r\n");
        builder.append("Connection: Upgrade\r\n");
        final Enumeration<String> headerNames = request.getHeaderNames();
        while (headerNames != null && headerNames.hasMoreElements()) {
            final String headerName = headerNames.nextElement();
            if (!isWebSocketHandshakeHeaderWrittenExplicitly(headerName)) {
                final Enumeration<String> values = request.getHeaders(headerName);
                while (values != null && values.hasMoreElements()) {
                    builder.append(headerName).append(": ").append(values.nextElement()).append("\r\n");
                }
            }
        }
        builder.append("\r\n");
        outputStream.write(builder.toString().getBytes(StandardCharsets.ISO_8859_1));
        outputStream.flush();
    }

    private boolean isWebSocketHandshakeHeaderWrittenExplicitly(String headerName) {
        final String lowerCaseHeader = headerName.toLowerCase(Locale.ROOT);
        return "host".equals(lowerCaseHeader)
                || "upgrade".equals(lowerCaseHeader)
                || "connection".equals(lowerCaseHeader)
                || "content-length".equals(lowerCaseHeader)
                || "transfer-encoding".equals(lowerCaseHeader);
    }

    private WebSocketHandshakeResponse readWebSocketHandshakeResponse(InputStream inputStream) throws IOException {
        final ByteArrayOutputStream responseBytes = new ByteArrayOutputStream();
        int b;
        int matched = 0;
        final byte[] separator = new byte[]{'\r', '\n', '\r', '\n'};
        while ((b = inputStream.read()) != -1) {
            responseBytes.write(b);
            matched = b == separator[matched] ? matched + 1 : (b == separator[0] ? 1 : 0);
            if (matched == separator.length) {
                break;
            }
            if (responseBytes.size() > 65536) {
                throw new IOException("WebSocket handshake response headers are too large");
            }
        }
        final String response = responseBytes.toString(StandardCharsets.ISO_8859_1);
        final String[] lines = response.split("\r\n");
        if (lines.length == 0) {
            throw new IOException("Empty WebSocket handshake response");
        }
        final String[] statusParts = lines[0].split(" ", 3);
        final int statusCode = statusParts.length > 1 ? Integer.parseInt(statusParts[1]) : HttpServletResponse.SC_BAD_GATEWAY;
        final String statusMessage = statusParts.length > 2 ? statusParts[2] : "";
        final List<Map.Entry<String, String>> headers = new ArrayList<>();
        for (int i = 1; i < lines.length; i++) {
            final int separatorIndex = lines[i].indexOf(':');
            if (separatorIndex > 0) {
                headers.add(new AbstractMap.SimpleImmutableEntry<>(
                        lines[i].substring(0, separatorIndex),
                        lines[i].substring(separatorIndex + 1).trim()));
            }
        }
        return new WebSocketHandshakeResponse(statusCode, statusMessage, headers);
    }

    private void closeQuietly(Socket socket) {
        if (socket != null) {
            try {
                socket.close();
            } catch (IOException ignored) {
            }
        }
    }

    private static void closeQuietly(WebConnection connection, Socket socket) {
        try {
            if (connection != null) {
                connection.close();
            }
        } catch (Exception ignored) {
        }
        try {
            if (socket != null) {
                socket.close();
            }
        } catch (IOException ignored) {
        }
    }

    private static void copy(InputStream inputStream, OutputStream outputStream) throws IOException {
        final byte[] buffer = new byte[8192];
        int read;
        while ((read = inputStream.read(buffer)) != -1) {
            outputStream.write(buffer, 0, read);
            outputStream.flush();
        }
    }

    private boolean isConnectionAbort(Throwable throwable) {
        Throwable current = throwable;
        while (current != null) {
            final String message = current.getMessage();
            if (message != null) {
                final String lowerCaseMessage = message.toLowerCase(Locale.ROOT);
                if (lowerCaseMessage.contains("established connection was aborted")
                        || lowerCaseMessage.contains("connection reset")
                        || lowerCaseMessage.contains("broken pipe")) {
                    return true;
                }
            }
            current = current.getCause();
        }
        return false;
    }

    private static class WebSocketHandshakeResponse {
        private final int statusCode;
        private final String statusMessage;
        private final List<Map.Entry<String, String>> headers;

        private WebSocketHandshakeResponse(int statusCode, String statusMessage, List<Map.Entry<String, String>> headers) {
            this.statusCode = statusCode;
            this.statusMessage = statusMessage;
            this.headers = headers;
        }
    }

    public static class WebSocketProxyUpgradeHandler implements HttpUpgradeHandler {
        private Socket backendSocket;

        @Override
        public void init(WebConnection connection) {
            final Socket socket = backendSocket;
            WEB_SOCKET_EXECUTOR.execute(() -> {
                try {
                    copy(connection.getInputStream(), socket.getOutputStream());
                } catch (IOException ignored) {
                } finally {
                    closeQuietly(connection, socket);
                }
            });
            WEB_SOCKET_EXECUTOR.execute(() -> {
                try {
                    copy(socket.getInputStream(), connection.getOutputStream());
                } catch (IOException ignored) {
                } finally {
                    closeQuietly(connection, socket);
                }
            });
        }

        @Override
        public void destroy() {
            closeQuietly(null, backendSocket);
        }
    }
}
