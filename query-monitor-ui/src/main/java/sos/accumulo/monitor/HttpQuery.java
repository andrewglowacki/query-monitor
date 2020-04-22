package sos.accumulo.monitor;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.apache.commons.io.IOUtils;
import org.apache.http.HttpEntity;
import org.apache.http.NameValuePair;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;

public class HttpQuery {

    private static final ObjectMapper MAPPER = new ObjectMapper()
        .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
        .copy();
    private static final RequestConfig normalConfig = RequestConfig.custom()
        .setConnectTimeout(30000)
        .setSocketTimeout(15000)
        .build();
    private static final RequestConfig quickRequestConfig = RequestConfig.custom()
        .setConnectTimeout(2000)
        .setSocketTimeout(2000)
        .build();
    
    public static <T> T normalQuery(String url, Class<T> clazz) throws IOException {
        return executeQuery(url, normalConfig, new SuccessHandler<T>() {
            @Override
            public T handle(InputStream inStream) throws IOException {
                return MAPPER.readValue(inStream, clazz);
            }
        });
    }
    public static <T> T normalPostQuery(String url, List<NameValuePair> params, Class<T> clazz) throws IOException {
        try (CloseableHttpClient client = createClient(normalConfig)) {
            HttpPost post = new HttpPost(url);
            post.setEntity(new UrlEncodedFormEntity(params));
            try (CloseableHttpResponse response = client.execute(post)) {
                if (response.getStatusLine().getStatusCode() != 200) {
                    throw new IOException("Response from runner/executor was: " + response.getStatusLine().getStatusCode() + ": " + getErrorReason(response));
                }
                
                try (InputStream inStream = response.getEntity().getContent()) {
                    return MAPPER.readValue(inStream, clazz);
                }
            }
        }
    }

    public static <T> T normalQuery(String url, JavaType valueType) throws IOException {
        return executeQuery(url, normalConfig, new SuccessHandler<T>() {
            @Override
            public T handle(InputStream inStream) throws IOException {
                return MAPPER.readValue(inStream, valueType);
            }
        });
    }

    public static <T> T quickQuery(String url, Class<T> clazz) throws IOException {
        return executeQuery(url, quickRequestConfig, new SuccessHandler<T>() {
            @Override
            public T handle(InputStream inStream) throws IOException {
                return MAPPER.readValue(inStream, clazz);
            }
        });
    }

    private static <T> T executeQuery(String url, RequestConfig requestConfig, SuccessHandler<T> handler) throws IOException {
        try (CloseableHttpClient client = createClient(requestConfig)) {
            HttpGet get = new HttpGet(url);
            try (CloseableHttpResponse response = client.execute(get)) {
                if (response.getStatusLine().getStatusCode() != 200) {
                    throw new IOException("Response from runner/executor was: " + response.getStatusLine().getStatusCode() + ": " + getErrorReason(response));
                }
                
                try (InputStream inStream = response.getEntity().getContent()) {
                    return handler.handle(inStream);
                }
            }
        }
    }

    private static interface SuccessHandler<T> { 
        public T handle(InputStream inStream) throws IOException;
    }

    protected static String getErrorReason(CloseableHttpResponse response) throws IOException {
        HttpEntity entity = response.getEntity();
        if (entity == null || entity.getContentLength() == 0) {
            return response.getStatusLine().getReasonPhrase();
        }

        ByteArrayOutputStream bytesOut = new ByteArrayOutputStream();
        try (InputStream inStream = response.getEntity().getContent()) {
            IOUtils.copy(inStream, bytesOut);
        }

        return new String(bytesOut.toByteArray(), StandardCharsets.UTF_8);
    }

    protected static CloseableHttpClient createClient(RequestConfig config) {
        return HttpClientBuilder.create()
            .setDefaultRequestConfig(normalConfig)
            .build();
    }
}