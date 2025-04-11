package at.qe.skeleton.tests;

import at.qe.skeleton.model.AuditLog;
import at.qe.skeleton.repositories.LogRepository;
import at.qe.skeleton.services.LogService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.web.WebAppConfiguration;

import java.util.Collection;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
@WebAppConfiguration
public class LogServiceTest {

    @Autowired
    private LogRepository logRepository;

    @Autowired
    private LogService logService;

    @Test
    @WithMockUser(username = "admin", authorities = {"ADMINISTRATOR"})
    public void testGetAllLogEntries() {
        int totalLogs = logRepository.count();
        Collection<AuditLog> logs = logService.getAllLogEntries();
        assertEquals(totalLogs, logs.size());
    }

}
