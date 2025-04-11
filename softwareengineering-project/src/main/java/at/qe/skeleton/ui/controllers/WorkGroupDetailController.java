package at.qe.skeleton.ui.controllers;

import at.qe.skeleton.model.WorkGroup;
import at.qe.skeleton.model.Userx;
import at.qe.skeleton.services.timeTracking.WorkGroupService;
import jakarta.faces.application.FacesMessage;
import jakarta.faces.context.FacesContext;
import lombok.Getter;
import lombok.Setter;
import org.primefaces.PrimeFaces;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;

import java.io.Serializable;
import java.util.*;

/**
 * Controller for managing the details of a work group.
 * <p>
 * This class handles the creation, modification, and deletion of work groups.
 * It also manages the selection of users and group leaders within a work group.
 * </p>
 */
@Component
@Scope("view")
public class WorkGroupDetailController implements Serializable {

    @Autowired
    private WorkGroupListController workGroupListController;

    @Autowired
    private transient WorkGroupService workGroupService;

    @Getter
    @Setter
    private WorkGroup workGroup;

    @Setter
    @Getter
    private List<Userx> selectedUsers;

    @Setter
    @Getter
    private Userx selectedGroupLeader;

    /**
     * Sets the current work group and initializes the selected users and group leader.
     *
     * @param workGroup the work group to be set
     */
    public void setWorkGroup(WorkGroup workGroup) {
        this.workGroup = workGroup;
        this.selectedUsers = workGroup.getUsers() == null ? new ArrayList<>() : new ArrayList<>(workGroup.getUsers());
        this.selectedGroupLeader = workGroup.getGroupLeader();
    }

    /**
     * Assigns the selected users and workgroup attributes from this controller to the workgroup.
     * This should be done before saving the workgroup.
     *
     * @param workGroup the work group to be updated
     * @return the updated work group
     */
    private WorkGroup assignFieldsToWorkGroup(WorkGroup workGroup) {
        workGroup.setGroupLeader(selectedGroupLeader);
        workGroup.setUsers(new HashSet<>(selectedUsers));
        workGroup.setName(this.workGroup.getName());
        workGroup.setDescription(this.workGroup.getDescription());
        return workGroup;
    }

    /**
     * Action to save the currently displayed work group.
     * Validates the selected users and group leader before saving.
     * Updates the work group list and displays a success or error message.
     */
    public void doSaveWorkGroup() {

        try {
            if (!workGroup.isNew()) {
                WorkGroup workGroupFromRepo = workGroupService.getGroupById(workGroup.getId()).orElseThrow();
                workGroup = assignFieldsToWorkGroup(workGroupFromRepo);
            }
            workGroupService.save(workGroup);
            workGroupListController.update();
            FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_INFO, "Success", "Work group saved."));
            PrimeFaces.current().ajax().update("workGroupForm");
        } catch (DataIntegrityViolationException e) {
            FacesContext.getCurrentInstance().validationFailed();
            FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_ERROR, "Error", "Group name must be unique. A work group with this name already exists."));
        } finally {
            doResetWorkGroup();

        }
    }

    /**
     * Action to delete the currently displayed work group.
     * Updates the work group list and displays a success message.
     */
    public void doDeleteWorkGroup() {
        this.workGroupService.delete(workGroup.getId());
        doResetWorkGroup();
        workGroupListController.update();
        FacesContext.getCurrentInstance().addMessage(null,
                new FacesMessage(FacesMessage.SEVERITY_INFO, "Success", "Work group deleted."));
    }

    /**
     * Resets the current work group, selected users, and group leader to null.
     */
    public void doResetWorkGroup() {
        this.workGroup = null;
        this.selectedUsers = null;
        this.selectedGroupLeader = null;
    }

    /**
     * Creates a new work group and sets it as the current work group.
     */
    public void createNewWorkGroup() {
        this.workGroup = workGroupService.createNewWorkGroup();
    }

    /**
     * Deselects all users in the selectMany JSF component.
     */
    public void doDeselectAllUsers() {
        this.selectedUsers = new ArrayList<>();
    }
}
