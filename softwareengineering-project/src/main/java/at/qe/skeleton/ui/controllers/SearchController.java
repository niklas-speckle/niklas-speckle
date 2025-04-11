package at.qe.skeleton.ui.controllers;

import at.qe.skeleton.model.Userx;
import at.qe.skeleton.model.WorkMode;
import at.qe.skeleton.services.timeTracking.TimeRecordService;
import at.qe.skeleton.services.UserService;
import at.qe.skeleton.ui.beans.SessionInfoBean;
import lombok.Getter;
import lombok.Setter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.io.Serializable;
import java.util.List;

@Getter
@Setter
@Component
@Scope("request")
public class SearchController implements Serializable {
    @Autowired
    private transient UserService userService;
    @Autowired
    private transient TimeRecordService timeRecordService;
    @Autowired
    private SessionInfoBean sessionInfoBean;

    private String userNameForSearch;

    public List<Userx> completeUser(String partialName) {
        return userService.findUserContaining(partialName, sessionInfoBean.getCurrentUser());
    }

    public String getUserWorkModeName(Userx user) {
        WorkMode currentWorkMode =  timeRecordService.getCurrentWorkModeOfUser(user);

        return currentWorkMode != null ? currentWorkMode.getName() : "No work mode";
    }

    public String getUserWorkModeColour(Userx user) {
        WorkMode currentWorkMode =  timeRecordService.getCurrentWorkModeOfUser(user);

        return currentWorkMode != null ? currentWorkMode.getColour() : "#808080";
    }

}
