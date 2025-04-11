package at.qe.skeleton.ui.controllers;


import at.qe.skeleton.model.DeviceStatus;
import jakarta.faces.model.SelectItem;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.io.Serializable;
import java.util.Arrays;
import java.util.List;

@Component
@Scope("view")
public class DeviceStatusListController implements Serializable {

    public List<SelectItem> getSelectItemsDeviceStatusList(){
        return Arrays.stream(DeviceStatus.values()).map(d -> new SelectItem(d, d.toString())).toList();
    }
}
