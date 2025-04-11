package at.qe.skeleton.ui.controllers;

import at.qe.skeleton.model.Project;
import at.qe.skeleton.model.Userx;
import at.qe.skeleton.services.timeTracking.ProjectService;
import jakarta.annotation.PostConstruct;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.io.Serializable;
import java.util.List;

/**
 * Controller for the project list view. In this view the a {@link Userx} with {@link at.qe.skeleton.model.UserxRole} MANAGER can see, delete, edit and create {@link at.qe.skeleton.model.Project}.
 */
@Component
@Scope("view")
public class ProjectListController implements Serializable {

    @Autowired
    private transient ProjectService projectService;

    @Getter
    private List<Project> projects;

    /**
     * Cashes all projects for sorting purposes in ui.
     */
    @PostConstruct
    public void init() {
        projects = projectService.getAll();
    }

    /**
     * updates the list of projects. This method is called when a project is deleted or created in the ui.
     */
    public void update() {
        projects = projectService.getAll();
    }


}
