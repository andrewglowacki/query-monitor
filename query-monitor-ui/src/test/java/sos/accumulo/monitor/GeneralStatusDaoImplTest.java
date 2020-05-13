package sos.accumulo.monitor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Set;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

@SpringJUnitConfig
@TestPropertySource("classpath:general-status-dao-test.properties")
@Import(GeneralStatusDaoImpl.class)
public class GeneralStatusDaoImplTest {
    
    @Autowired
    public GeneralStatusDao dao;

    @Test
    public void getAddressTest() {
        dao.register("test-name1", "test-address:11111");
        dao.register("test-name2", "test-address:11112");

        assertEquals("test-address:11111", dao.getAddress("test-name1"));
        assertEquals("test-address:11112", dao.getAddress("test-name2"));
        assertEquals("r1n03-node:54321", dao.getAddress("r1n03-node"));

        assertThrows(IllegalArgumentException.class, () -> {
            dao.getAddress("r1n02-node");
        });
    }

    @Test
    public void getRunnersTest() {
        dao.register("test-name1", "test-address:11111");
        dao.register("test-name2", "test-address:11112");
        dao.register("test-name3", "test-address2:11111");

        Set<String> runners1 = dao.getRunnersOnServer("test-address");

        assertEquals(2, runners1.size());
        assertTrue(runners1.contains("test-name1"));
        assertTrue(runners1.contains("test-name2"));
        
        Set<String> runners2 = dao.getRunnersOnServer("test-address2");
        
        assertEquals(1, runners2.size());
        assertTrue(runners2.contains("test-name3"));
        
        assertTrue(dao.getRunnersOnServer("non-existent").isEmpty());
        assertTrue(dao.getRunnersOnServer("r1n03-node").isEmpty());
    }

}