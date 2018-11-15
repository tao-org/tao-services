/*
 * $Id$
 *
 * Copyright (C) 2010 by Brockmann Consult (info@brockmann-consult.de)
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the
 * Free Software Foundation. This program is distributed in the hope it will
 * be useful, but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */
package ro.cs.tao.wps;

import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;

import java.io.IOException;

public class IntegrationTestHelper {

    public static CloseableHttpResponse getWpsExecuteResponse(String serviceAdress, String xml) throws IOException {
        final CloseableHttpClient httpClient = createAuthenticatedHttpClient();
        final HttpPost request = new HttpPost(serviceAdress);
        request.setEntity(new StringEntity(xml, ContentType.TEXT_XML));
        return httpClient.execute(request);
    }

    static CloseableHttpResponse getHttpGetResponse(String requestURL) throws IOException {
        final CloseableHttpClient httpClient = createAuthenticatedHttpClient();
        return httpClient.execute(new HttpGet(requestURL));
    }

    private static CloseableHttpClient createAuthenticatedHttpClient() {
        final BasicCredentialsProvider credentialsProvider = new BasicCredentialsProvider();
        final UsernamePasswordCredentials usernamePasswordCredentials = new UsernamePasswordCredentials("admin", "admin");
        credentialsProvider.setCredentials(AuthScope.ANY, usernamePasswordCredentials);
        final HttpClientBuilder httpClientBuilder = HttpClientBuilder.create();
        return httpClientBuilder
                .setDefaultCredentialsProvider(credentialsProvider)
                .build();
    }
}
