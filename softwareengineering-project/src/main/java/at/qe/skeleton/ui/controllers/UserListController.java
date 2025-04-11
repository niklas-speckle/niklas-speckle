package at.qe.skeleton.ui.controllers;

import at.qe.skeleton.model.*;
import at.qe.skeleton.services.UserService;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import at.qe.skeleton.ui.beans.SessionInfoBean;
import at.qe.skeleton.ui.converter.UserConverter;
import lombok.Getter;
import lombok.Setter;

import jakarta.faces.model.SelectItem;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

/**
 * Controller for the user list view.
 * <p>
 * This class is part of the skeleton project provided for students of the
 * course "Software Engineering" offered by the University of Innsbruck.
 */
@Getter
@Setter
@Component
@Scope("view")
public class UserListController implements Serializable {

    @Autowired
    private transient UserService userService;

    @Autowired
    private SessionInfoBean sessionInfoBean;

    @Getter
    private Collection<Userx> users;

    /**
     * Cashes all users for sorting purposes  in ui.
     */
    public void init() {
        users = userService.getAllUsers();
    }

    /**
     * Cashes coworkers from the current user's work groups for sorting purposes in the welcome page.
     */
    public void initWorkGroupCoworkers() {
        users = userService.getWorkGroupCoworkers(sessionInfoBean.getCurrentUser());
    }


    /**
     * Returns a select item list of all users with GROUP_LEADER role for selectMany UI component.
     */
    public List<SelectItem> getSelectItemsGroupLeadersList() {
        List<SelectItem> optionList = new ArrayList<>();
        List<Userx> groupLeader = userService.getUsersByRole(UserxRole.GROUP_LEADER);
        optionList.addAll(groupLeader.stream().map(u -> new SelectItem(u, u.getUsername())).toList());
        optionList.add(new SelectItem(null, UserConverter.NO_USER_STRING));
        return optionList;
    }



    /**
     * Returns a select item list of all users with MANAGER role for UI component.
     *
     * @return
     */
    public List<SelectItem> getSelectItemsManagerList() {
        List<SelectItem> optionList = new ArrayList<>();
        optionList.add(new SelectItem(null, UserConverter.NO_USER_STRING));
        optionList.addAll(userService.getUsersByRole(UserxRole.MANAGER).stream().map(u -> new SelectItem(u, u.getUsername())).toList());
        return optionList;
    }

    /**
     * Returns a select item list of all users for UI component with null option.
     *
     * @return
     */
    public List<SelectItem> getSelectItemsUserListWithNullOption() {
        List<SelectItem> optionList = new ArrayList<>();
        optionList.add(new SelectItem(null, UserConverter.NO_USER_STRING));
        optionList.addAll(userService.getAllEnabled().stream().map(u -> new SelectItem(u, u.getUsername())).toList());
        return optionList;
    }

    /**
     * Returns a select item list of all users for UI component without null option.
     */
    public List<SelectItem> getSelectItemsUserListWithoutNullOption() {
        List<SelectItem> optionList = new ArrayList<>();
        optionList.addAll(userService.getAllEnabled().stream().map(u -> new SelectItem(u, u.getUsername())).toList());
        return optionList;
    }







    /**
     * Updates the user list. Called by userDetailController after creating, editing or deleting a user in the ui.
     * If the current user is not an admin, they have no access to the full user list and it is not updated.
     */
    public void update() {
        if (sessionInfoBean.getCurrentUser().getRoles().contains(UserxRole.ADMINISTRATOR)) {
            users = userService.getAllUsers();
        }
    }


}
