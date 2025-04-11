package at.qe.skeleton.ui.controllers;

import at.qe.skeleton.model.WorkGroup;
import at.qe.skeleton.services.timeTracking.WorkGroupService;
import jakarta.annotation.PostConstruct;
import jakarta.faces.model.SelectItem;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

@Component
@Scope("view")
public class WorkGroupListController implements Serializable {


    @Autowired
    private transient WorkGroupService workGroupService;


    /**
     * Cached list of workGroups for list view. Cached needed for sort- and filter functions.
     */
    @Getter
    private List<WorkGroup> workGroups;


    /**
     * Returns a list of all work groups.
     * @return A list of work groups to be shown in the workgroups view.
     */
    @PostConstruct
    private void init() {
            workGroups = workGroupService.getAll();
    }


    /**
     * Returns a SelectItem list of relevant workgroups to assign to a project in an ui select many menu.
     * @return a SelectItem list of relevant workgroups
     */
    public List<SelectItem> getSelectItemsWorkGroupList() {
        List<SelectItem> optionList = new ArrayList<>();
        optionList.addAll(workGroups.stream().map(w -> new SelectItem(w, w.getName())).toList());
        return optionList;
    }

    /**
     * Updates the list of work groups. Method is called by WorkGroupDetailController after editing, creating or deleting work groups in the ui.
     */
    public void update() {
            workGroups = workGroupService.getAll();
    }


}
