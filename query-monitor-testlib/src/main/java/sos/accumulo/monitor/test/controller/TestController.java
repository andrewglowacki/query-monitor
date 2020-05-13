package sos.accumulo.monitor.test.controller;

import static org.junit.Assert.assertEquals;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.net.BindException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.apache.catalina.Context;
import org.apache.catalina.LifecycleException;
import org.apache.catalina.startup.Tomcat;

public class TestController extends HttpServlet implements Closeable {
    private static final long serialVersionUID = 1L;
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final Map<String, Object> returnObjects = new ConcurrentHashMap<>();
    private final List<Invocation> invocations = Collections.synchronizedList(new ArrayList<>());
    private final Tomcat tomcat;
    private volatile Object defaultNextReturnObject = null;

    public TestController(int port) {
        File workDir = new File("target/tomcat-test-controller");
        workDir.mkdirs();
        tomcat = new Tomcat();
        tomcat.setPort(port);
        tomcat.setSilent(false);
        tomcat.setBaseDir(workDir.getAbsolutePath());
        Context context = tomcat.addContext("", workDir.getAbsolutePath());
        Tomcat.addServlet(context, "TestController", this);
        context.addServletMappingDecoded("/", "TestController");
        tomcat.getConnector();
        for (int i = 0; i < 25; i++) {
            try {
                tomcat.start();
                break;
            } catch (LifecycleException ex) {
                if (ex.getCause() instanceof BindException) {
                    try {
                        Thread.sleep(200);
                    } catch (InterruptedException ex2) {
                        throw new RuntimeException(ex2);
                    }
                } else {
                    throw new RuntimeException(ex);
                }
            }
        }
    }


    @Override
    protected void service(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        Invocation invocation = new Invocation(request);
        invocations.add(invocation);
        Object retObj = returnObjects.get(invocation.getPath());
        if (retObj == null) {
            retObj = defaultNextReturnObject;
        }
        if (retObj != null) {
            response.setContentType("application/json");
            try (OutputStream outStream = response.getOutputStream()) {
                MAPPER.writeValue(outStream, retObj);
            }
        }
        response.setStatus(200);
    }

    public void reset() {
        this.returnObjects.clear();
        this.defaultNextReturnObject = null;
        this.invocations.clear();
    }
    public void setNextReturnObject(Object nextReturnObject) {
        this.defaultNextReturnObject = nextReturnObject;
        this.returnObjects.clear();
    }
    public void setNextReturnObject(String path, Object nextReturnObject) {
        this.defaultNextReturnObject = null;
        this.returnObjects.put(path, nextReturnObject);
    }
    public TestController assertInvocations(int expectedCount) {
        assertEquals(expectedCount, invocations.size());
        return this;
    }
    public List<Invocation> getInvocationsByPath(String path) {
        return invocations.stream()
            .filter(invocation -> invocation.getPath().equals(path))
            .collect(Collectors.toList());
    }

    public List<Invocation> getInvocations() {
        return invocations;
    }

    @Override
    public void close() {
        try {
            tomcat.stop();
        } catch (Exception ex) { }
    }
}