package at.qe.skeleton.services;

import at.qe.skeleton.model.AuditLog;
import at.qe.skeleton.repositories.LogRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Component;

import java.util.Collection;

@Component
@Scope("application")
public class LogService {

    @Autowired
    private LogRepository logRepository;

    /**
     * The method Returns all Logs.
     *
     * @return a Collection of all log entries.
     */
    @PreAuthorize("hasAuthority('ADMINISTRATOR')")
    public Collection<AuditLog> getAllLogEntries() {
        return logRepository.findAll();
    }



}
