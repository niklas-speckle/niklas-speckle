package at.qe.skeleton.ui.controllers;

import at.qe.skeleton.model.AuditLog;
import at.qe.skeleton.services.LogService;
import jakarta.annotation.PostConstruct;
import lombok.Getter;
import lombok.Setter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.io.Serializable;
import java.util.Collection;

/**
 * Controller for the log list view.
 */
@Getter
@Setter
@Component
@Scope("view")
public class LogListController implements Serializable {

    @Autowired
    private transient LogService logService;

    private Collection<AuditLog> logs;

    /**
     * Cashes all logs for sorting purposes.
     */
    @PostConstruct
    public void init() {
        logs = logService.getAllLogEntries();
    }


}
