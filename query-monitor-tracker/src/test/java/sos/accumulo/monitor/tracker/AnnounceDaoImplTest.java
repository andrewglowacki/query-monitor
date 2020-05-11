package sos.accumulo.monitor.tracker;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.IOException;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.web.bind.annotation.RequestMethod;

import sos.accumulo.monitor.tracker.controller.Invocation;
import sos.accumulo.monitor.tracker.controller.TestController;


@RunWith(SpringRunner.class)
@ActiveProfiles({"TrackerModeRunner", "AnnounceDaoImplTest"})
@WebMvcTest(
    controllers = {}, 
    useDefaultFilters = false, 
    properties = {
        "announce.address=localhost:43333",
        "runner.name=test-announce-runner"
    }
)
public class AnnounceDaoImplTest {

    @Autowired
    private AnnounceDao dao;

    @Profile("AnnounceDaoImplTest")
    @Configuration
    public static class TestConfig {
        @Bean
        public AnnounceDao announce() {
            return new AnnounceDaoImpl();
        }
        @Bean
        public TrackerAddress address() {
            TrackerAddress address = mock(TrackerAddress.class);
            when(address.get()).thenReturn("test-tracker:12345");
            return address;
        }
    }

    @Test
    public void announceTest() throws IOException {
        try (TestController controller = new TestController(43333)) {
            assertEquals("localhost:43333", dao.getAnnounceAddress());

            dao.announceRunner();

            assertEquals(1, controller.getInvocations().size());
            Invocation invocation = controller.getInvocations().get(0);
        
            invocation.assertMethodEquals(RequestMethod.POST);
            invocation.assertPathEquals("/api/runner/register");
            invocation.assertNumParams(2);
            invocation.assertParamEquals("name", "test-announce-runner");
            invocation.assertParamEquals("address", "test-tracker:12345");
        }
    }
}