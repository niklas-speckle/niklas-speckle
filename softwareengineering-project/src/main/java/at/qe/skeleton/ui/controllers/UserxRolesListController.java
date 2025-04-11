package at.qe.skeleton.ui.controllers;

import at.qe.skeleton.model.UserxRole;
import jakarta.faces.model.SelectItem;
import lombok.Getter;
import lombok.Setter;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;


/**
 * Controller for UserxRoles checkbox to assign user roles to user.
 */
@Component
@Scope("view")
public class UserxRolesListController implements Serializable {

    public UserxRolesListController() {
        this.allUserRoles = new ArrayList<>();
        for (UserxRole userRole : UserxRole.values()) {
            allUserRoles.add(new SelectItem(userRole, userRole.toString()));
        }
    }

    @Getter
    private List<SelectItem> allUserRoles;


    @Getter
    @Setter
    private List<UserxRole> selectedUserRoles;

}
