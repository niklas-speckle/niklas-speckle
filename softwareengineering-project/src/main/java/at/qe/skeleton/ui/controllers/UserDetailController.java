package at.qe.skeleton.ui.controllers;

import at.qe.skeleton.exceptions.EntityValidationException;
import at.qe.skeleton.model.*;
import at.qe.skeleton.services.UserService;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import at.qe.skeleton.services.timeTracking.ProjectService;
import jakarta.faces.application.FacesMessage;
import jakarta.faces.context.FacesContext;
import at.qe.skeleton.ui.beans.SessionInfoBean;
import lombok.Getter;
import lombok.Setter;
import org.primefaces.PrimeFaces;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

/**
 * Controller for the user detail view.
 * <p>
 * This class is part of the skeleton project provided for students of the
 * course "Software Engineering" offered by the University of Innsbruck.
 */
@Component
@Scope("view")
@Getter
@Setter
public class UserDetailController implements Serializable {

    @Autowired
    private UserListController userListController;

    @Autowired
    private transient UserService userService;

    @Autowired
    private transient ProjectService projectService;

    @Autowired
    private SessionInfoBean sessionInfoBean;

    /**
     * Attribute to cache the currently displayed user
     * Returns the currently displayed user.
     *
     * @return
     */
    private Userx user;

    private List<UserxRole> selectedRoles = new ArrayList<>();

    // for profile page
    private String newPassword;

    private final List<WorkModeVisibility> workModeVisibilities = List.of(WorkModeVisibility.values());

    /**
     * Sets the currently displayed user and reloads it form db. This user is
     * targeted by any further calls of
     * {{@link #doSaveUser()} and
     * {@link #doDeleteUser()}.
     *
     * @param user
     */
    public void setUser(Userx user) {
        this.user = user;
        selectedRoles.addAll(this.user.getRoles());
    }

    public List<Project> getProjectsForUser() {
        return projectService.getProjectsByUser(this.user);
    }

    /**
     * Action to save the currently displayed user.
     */
    public void doSaveUser() {
        this.user.setRoles(selectedRoles);
        if (selectedRoles.contains(UserxRole.ADMINISTRATOR)) {user.setWorkModeVisibility(WorkModeVisibility.PUBLIC);}
        try {
            this.userService.saveUser(this.user);
            this.userListController.update();
            FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_INFO, "Success", "User saved."));
            PrimeFaces.current().ajax().update("userForm");
            doResetUser();
        } catch (EntityValidationException e) {
            FacesContext.getCurrentInstance().validationFailed();
            FacesContext.getCurrentInstance().addMessage("createForm:createMsg",
                    new FacesMessage(FacesMessage.SEVERITY_ERROR, "Error", e.getMessage()));
        }
    }

    /**
     * Action to delete the currently displayed user.
     */
    public void doDeleteUser() {
        this.userService.deleteUser(this.user);
        this.userListController.update();
        doResetUser();
    }

    public void doCreateUser() {
        this.user = userService.createUser();
        // do not reset user here, as user needs to be edited before saving
    }

    public void doResetUser() {
        this.user = null;
        this.selectedRoles = new ArrayList<>();
    }

    public void doChangePassword() {
        try {
            userService.changePassword(this.user, this.newPassword);
            FacesContext.getCurrentInstance().addMessage(null, new FacesMessage(FacesMessage.SEVERITY_INFO, "Success", "Password was changed."));
        } catch (EntityValidationException e) {
            FacesContext.getCurrentInstance().addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR, "Error", e.getMessage()));
        }
    }
}
