package at.qe.skeleton.ui.controllers;

import at.qe.skeleton.exceptions.IdNotFoundException;
import at.qe.skeleton.model.Project;
import at.qe.skeleton.model.Userx;
import at.qe.skeleton.model.UserxRole;
import at.qe.skeleton.model.WorkGroup;
import at.qe.skeleton.services.timeTracking.ProjectService;
import at.qe.skeleton.services.timeTracking.WorkGroupService;
import at.qe.skeleton.ui.beans.SessionInfoBean;
import at.qe.skeleton.ui.converter.FacesMessageSummaryStrings;
import jakarta.faces.application.FacesMessage;
import jakarta.faces.context.FacesContext;
import jakarta.faces.model.SelectItem;
import lombok.Getter;
import lombok.Setter;
import org.primefaces.PrimeFaces;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.io.Serializable;
import java.util.*;

/**
 * Controller for the project detail view.
 */

@Component
@Scope("view")
public class ProjectDetailController implements Serializable {

    @Autowired
    private ProjectListController projectListController;

    @Autowired
    private SessionInfoBean sessionInfoBean;

    @Autowired
    private transient ProjectService projectService;

    @Autowired
    private transient WorkGroupService workGroupService;

    /**
     * Attribute to cache the currently displayed project
     * -- GETTER --
     *
     * @return the currently displayed project

     */
    @Getter
    private Project project;

    @Getter
    @Setter
    /**
     * A group leader can only assign his own work group members to a project.
     */
    private List<Userx> usersToAssign;

    @Getter
    @Setter
    private List<SelectItem> selectItemsForAssignUsersDialog;

    @Getter
    @Setter
    private List<Userx> selectedUsers;

    @Setter
    @Getter
    private Userx selectedManager;

    @Getter
    @Setter
    private List<WorkGroup> selectedWorkGroups;

    /**
     * Sets the currently displayed project and reloads it form db. This project is
     * targeted by any further calls of
     * {{@link #doSaveProject()} and
     * {@link #doDeleteProject()}.
     *
     * @param project
     */
    public void setProject(Project project) {
        this.project = projectService.getProjectById(project.getId()).orElseThrow();
        this.usersToAssign = getUsersToAssignForCurrentUser();
        this.selectedUsers = getSelectedUsersForUserSearchDialog();
        this.selectedManager = project.getProjectManager();
        this.selectedWorkGroups = project.getWorkGroups() == null ? new ArrayList<>() : new ArrayList<>(project.getWorkGroups());
    }


    /**
     * Assigns the selected manager, workgroups and users attributes from this controller to the project. This should be done before saving the project.
     */
    private Project assignFieldsToProject(Project project){
            project.setProjectManager(selectedManager);
            project.setWorkGroups(selectedWorkGroups);
            project.setName(this.project.getName());
            project.setDescription(this.project.getDescription());
        return project;
    }


    /**
     * Action to save the currently displayed project.
     */
    public void doSaveProject() {
        try {
            if (!project.isNew()) {
                Project projectFromRepo = projectService.getProjectById(project.getId()).orElseThrow();
                project = assignFieldsToProject(projectFromRepo);
            }
            projectService.save(project);
            projectListController.update();
            FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_INFO, FacesMessageSummaryStrings.SUCCESS, "Project saved."));
            PrimeFaces.current().ajax().update("projectForm");
        }  catch (Exception e) {
            FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_ERROR, FacesMessageSummaryStrings.ERROR, "Project name must be unique. A project with this name already exists."));
        } finally {
            doResetProject();
        }
    }

    /**
     * Action to delete the currently displayed project.
     */
    public void doDeleteProject() {
        try {
            this.projectService.delete(project.getId());
            projectListController.update();
            FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_INFO, FacesMessageSummaryStrings.SUCCESS, "Project deleted."));
        } catch (IdNotFoundException e) {
            FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_ERROR, FacesMessageSummaryStrings.ERROR, e.getMessage()));
        } finally {
            doResetProject();
        }
    }

    /**
     * Sets the currently displayed project to null.
     */
    public void doResetProject() {
        this.project = null;
        this.selectedUsers = null;
        this.selectedManager = null;
        this.selectedWorkGroups = null;
        this.usersToAssign = null;
    }


    public void doSetUsers() {

        try{
            List<Userx> projectUsers = project.getUsers();
            // remove users which can not be added/removed by the currently logged in user
            projectUsers.removeAll(usersToAssign);
            // add users which are selected in the user search dialog
            projectUsers.addAll(selectedUsers);

            projectService.setUsers(project, projectUsers);
            FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_INFO, FacesMessageSummaryStrings.SUCCESS, "Users updated."));
            projectListController.update();
            PrimeFaces.current().ajax().update("projectForm");
        } catch (Exception e){
            FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_ERROR, FacesMessageSummaryStrings.ERROR, e.getMessage()));
        } finally {
            doResetProject();
        }
    }


    public void createNewProject() {
        this.project = projectService.createNewProject();
        Userx currentUser = sessionInfoBean.getCurrentUser();
        if (currentUser.getRoles().contains(UserxRole.MANAGER)) {
            this.project.setProjectManager(currentUser);
        }
    }

    /**
     * Group leader are only able to assign users to projects when the user is in the group assigned to the project. shows all options of users who can be added to the project
     */
    public List<Userx> getUsersToAssignForCurrentUser(){
        List<Userx> users;
        if (sessionInfoBean.hasRole("ADMINISTRATOR") || sessionInfoBean.hasRole("MANAGER")) {
            users = getAllUsersInProjectWorkGroups();
        } else {
            users = getUsersForGroupLeader();
        }
        selectItemsForAssignUsersDialog = users.stream()
                .map(user -> new SelectItem(user, user.getUsername()))
                .toList();
        return users;
    }

    private List<Userx> getAllUsersInProjectWorkGroups() {
        return project.getWorkGroups().stream()
                .map(WorkGroup::getUsers)
                .flatMap(Collection::stream)
                .distinct()
                .toList();
    }

    private List<Userx> getUsersForGroupLeader() {
        List<WorkGroup> userWorkGroups = workGroupService.getGroupsByGroupLeader(sessionInfoBean.getCurrentUser());

        if (userWorkGroups.isEmpty() || project == null) {
            return Collections.emptyList();
        }

        List<WorkGroup> projectWorkGroups = project.getWorkGroups();
        if (projectWorkGroups == null) {
            return Collections.emptyList();
        }

        List<WorkGroup> relevantWorkGroups = new ArrayList<>(userWorkGroups);
        relevantWorkGroups.retainAll(projectWorkGroups);

        return relevantWorkGroups.stream()
                .map(WorkGroup::getUsers)
                .flatMap(Collection::stream)
                .distinct()
                .toList();
    }

    /**
     * To have users preselected in the edit user dialog.
     * Sets the selectedUsers attribute such that only those users are highlighted which are already in the project and
     * can be added/removed by the current user. Group leaders can only add/remove users from their own work groups.
     * Accordingly, there might be additional users in the project which are not shown in the user search dialog.
     **/
    public List<Userx> getSelectedUsersForUserSearchDialog() {
        // tick those which are already in the project
        List<Userx> selectedUsersForUserSearch = new ArrayList<>(this.usersToAssign);
        selectedUsersForUserSearch.retainAll(this.project.getUsers() == null ? new ArrayList<>() : this.project.getUsers());
        return selectedUsersForUserSearch;
    }

    public boolean groupLeaderWorksOnProject(Project projectToEdit) {
        if (!sessionInfoBean.hasRole("GROUP_LEADER")) {
            return false;
        }
        List<WorkGroup> groups = workGroupService.getGroupsByGroupLeader(sessionInfoBean.getCurrentUser());
        List<Project> projects = new ArrayList<>();
        if (groups != null) {
            for (WorkGroup group : groups) {
                projects.addAll(group.getProjects());
            }
        }
        return projects.contains(projectToEdit);
    }

    /**
     * Deselects all work groups in the jsf component.
     */
    public void doDeselectAllWorkGroups() {
        this.selectedWorkGroups = new ArrayList<>();
    }



    /**
     * Deselects all users in the jsf component.
     */
    public void doDeselectAllUsers() {
        this.selectedUsers = new ArrayList<>();
    }
}
