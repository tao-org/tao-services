package ro.cs.tao.services.geostorm;

import org.apache.commons.codec.binary.Base64;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.PropertySource;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import ro.cs.tao.services.geostorm.model.Resource;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.nio.charset.Charset;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

/**
 * Geostorm client used for calling Geostorm services
 */
@Component
@PropertySource("classpath:geostorm.properties")
public class GeostormClient {

    @Autowired
    private RestTemplate restTemplate;

    @Value("${geostorm.rest.base.url}")
    private String geostormRestBaseURL;

    @Value("${geostorm.rest.catalog.resource.endpoint}")
    private String geostormRestCatalogResourceEndpoint;

    @Value("${geostorm.admin.username}")
    private String geostormUsername;

    @Value("${geostorm.admin.password}")
    private String geostormPassword;

    private static final Logger log = LoggerFactory.getLogger(GeostormClient.class);

    public String getResources()
    {
        trustSelfSignedSSL();

        ResponseEntity<String> result = null;

        final HttpHeaders headers = createHeaders(geostormUsername, geostormPassword);
        headers.setContentType(MediaType.APPLICATION_JSON);

        final HttpEntity<String> httpEntity = new HttpEntity<String>(headers);

        final String url = geostormRestBaseURL + geostormRestCatalogResourceEndpoint;

        log.info("URL = "+ url);

        result = restTemplate.exchange(url, HttpMethod.GET, httpEntity, String.class);

        try {
            log.info("getResources result:" + new ObjectMapper().writeValueAsString(result));
        }catch (JsonProcessingException e)
        {
            log.error("getResources(): JSON exception", e);
        }

        return result.getBody();
    }

    public String addResource(Resource resource)
    {
        trustSelfSignedSSL();

        ResponseEntity<String> result = null;

        final HttpHeaders headers = createHeaders(geostormUsername, geostormPassword);
        headers.setContentType(MediaType.APPLICATION_JSON);

        final HttpEntity<Resource> httpEntity = new HttpEntity<Resource>(resource, headers);

        final String url = geostormRestBaseURL + geostormRestCatalogResourceEndpoint;

        log.info("URL = "+ url);
        log.info("Headers = " + headers.toString());
        try{
            log.info("Body = " + new ObjectMapper().writeValueAsString(httpEntity));
        }catch (JsonProcessingException e)
        {
            log.error("addResource(): Body JSON exception", e);
        }

        result = restTemplate.postForEntity(url, httpEntity, String.class );

        try {
            log.info("addResource result:" + new ObjectMapper().writeValueAsString(result));
        }catch (JsonProcessingException e)
        {
            log.error("addResource(): Result JSON exception", e);
        }

        return result.getBody();
    }

    private void prepareSSL() {
        System.setProperty("javax.net.ssl.trustStore", "*");
        System.setProperty("javax.net.ssl.trustStorePassword", "");
  }

    public static void trustSelfSignedSSL() {
        log.info("trustSelfSignedSSL");
        try {
            SSLContext ctx = SSLContext.getInstance("TLS");
            X509TrustManager tm = new X509TrustManager() {

                public void checkClientTrusted(X509Certificate[] xcs, String string) throws CertificateException {
                }

                public void checkServerTrusted(X509Certificate[] xcs, String string) throws CertificateException {
                }

                public X509Certificate[] getAcceptedIssuers() {
                    return null;
                }
            };
            ctx.init(null, new TrustManager[]{tm}, null);
            SSLContext.setDefault(ctx);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    HttpHeaders createHeaders(String username, String password){
        return new HttpHeaders() {{
            String auth = username + ":" + password;
            byte[] encodedAuth = Base64.encodeBase64(
              auth.getBytes(Charset.forName("US-ASCII")));
            String authHeader = "Basic " + new String( encodedAuth );
            set( "Authorization", authHeader );
        }};
    }

    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }

    public String getGeostormRestBaseUrl(){
        return geostormRestBaseURL;
    }

    public String getGeostormRestCatalogResourceEndpoint(){
        return geostormRestCatalogResourceEndpoint;
    }
}
