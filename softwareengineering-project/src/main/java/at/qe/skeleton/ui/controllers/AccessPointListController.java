package at.qe.skeleton.ui.controllers;

import at.qe.skeleton.model.AccessPoint;
import at.qe.skeleton.services.AccessPointService;
import at.qe.skeleton.ui.converter.AccessPointConverter;
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
public class AccessPointListController implements Serializable {

    @Autowired
    private transient AccessPointService accessPointService;

    @Getter
    private List<AccessPoint> accessPoints;


    @PostConstruct
    public void init() {
        accessPoints = accessPointService.getAllAccessPoints();
    }


    public List<SelectItem> getSelectItemsAccessPointList() {
        List<SelectItem> optionList = new ArrayList<>();
        optionList.add(new SelectItem(null, AccessPointConverter.NO_ACCESS_POINT_STRING));
        optionList.addAll(accessPointService.getAllAccessPoints().stream().map(a -> new SelectItem(a, a.getId().toString())).toList());
        return optionList;
    }

    /**
     * Updates attributes of controller. Is called after any changes to the accessPoints list in the database.
     * e.g. after creating a new accessPoint the AccessPointDetailController calls this method to update the list in the ui.
     */
    public void update(){
        accessPoints = accessPointService.getAllAccessPoints();
    }

}
