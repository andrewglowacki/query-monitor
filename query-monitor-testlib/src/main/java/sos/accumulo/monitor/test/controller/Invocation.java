package sos.accumulo.monitor.test.controller;

import static org.junit.Assert.assertEquals;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.io.IOUtils;
import org.springframework.web.bind.annotation.RequestMethod;

public class Invocation {

    private final RequestMethod method;
    private final String path;
    private final Map<String, String> params;
    private final String body;

    public Invocation(HttpServletRequest request) {
        this.method = RequestMethod.valueOf(request.getMethod());
        try {
            this.path = new URL(request.getRequestURL().toString()).getPath();
        } catch (MalformedURLException ex) {
            throw new RuntimeException(ex);
        }
        Map<String, String> params = new HashMap<>();
        for (Entry<String, String[]> entry : request.getParameterMap().entrySet()) {
            if (entry.getValue() == null || entry.getValue().length == 0) {
                continue;
            }
            params.put(entry.getKey(), entry.getValue()[0]);
        }

        if (request.getContentType() != null && request.getContentType().contains("application/json")) {
            ByteArrayOutputStream bytesOut = new ByteArrayOutputStream();
            try {
                try (InputStream inStream = request.getInputStream()) {
                    IOUtils.copy(inStream, bytesOut);
                }
                body = bytesOut.toString("UTF-8");
            } catch (Exception ex) {
                throw new RuntimeException(ex);
            }
        } else {
            body = null;
        }

        this.params = params;
    }
    
    public String getBody() {
        return body;
    }
    public void assertMethodEquals(RequestMethod expected) {
        assertEquals(expected, method);
    }
    public void assertPathEquals(String expectedPath) {
        assertEquals(expectedPath, path);
    }
    public void assertNumParams(int expected) {
        assertEquals(expected, params.size());
    }
    public void assertParamEquals(String param, String expected) {
        String actual = params.get(param);
        assertEquals(expected, actual);
    }
}