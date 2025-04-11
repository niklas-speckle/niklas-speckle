package at.qe.skeleton.ui.controllers;

import at.qe.skeleton.model.AccessPoint;
import at.qe.skeleton.model.TemperaDevice;
import at.qe.skeleton.model.Userx;
import at.qe.skeleton.services.AccessPointService;
import at.qe.skeleton.services.TemperaDeviceService;
import at.qe.skeleton.services.UserService;
import at.qe.skeleton.ui.beans.TemperaDeviceBean;
import at.qe.skeleton.ui.converter.TemperaDeviceConverter;
import jakarta.annotation.PostConstruct;
import jakarta.faces.model.SelectItem;
import lombok.Getter;
import lombok.Setter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

@Component
@Scope("view")
public class TemperaDeviceListController implements Serializable {

    @Autowired
    private transient TemperaDeviceService temperaDeviceService;

    @Autowired
    private transient UserService userService;

    @Autowired
    private transient AccessPointService accessPointService;

    @Setter
    @Getter
    private List<TemperaDeviceBean> temperaDeviceBeans;

    /**
     * Cashes all TemperaDevices for sorting purposes in ui.
     */
    @PostConstruct
    public void init() {
        updateTemperaDeviceBeans();
    }

    /**
     * @return a list of all TemperaDevices
     */
    private List<TemperaDeviceBean> updateTemperaDeviceBeans(){
        List<TemperaDevice> temperaDeviceList = temperaDeviceService.getAllTemperaDevices();
        temperaDeviceBeans = new ArrayList<>();
        for(TemperaDevice temperaDevice : temperaDeviceList){
            Userx connectedUser = userService.getUserByTemperaDevice(temperaDevice);
            AccessPoint connectedAccessPoint = accessPointService.getAccessPointByTemperaDevice(temperaDevice);

            TemperaDeviceBean temperaDeviceBean = new TemperaDeviceBean();
            temperaDeviceBean.setTemperaDevice(temperaDevice);

            if (connectedAccessPoint != null) {
                temperaDeviceBean.setAccessPoint(connectedAccessPoint);
            }

            if (connectedUser != null) {
                temperaDeviceBean.setUser(connectedUser);
            }

            temperaDeviceBeans.add(temperaDeviceBean);
        }

        return temperaDeviceBeans;
    }

    /**
     * @return a list of select items of all TemperaDevices plus a null option. Used for assigning TemperaDevices to access points in frontend
     */
    public List<SelectItem> getSelectItemsTemperaDeviceList() {
        List<SelectItem> optionsTempereDeviceList = new ArrayList<>();
        optionsTempereDeviceList.add(new SelectItem(null, TemperaDeviceConverter.NO_TEMPERA_DEVICE_STRING));
        optionsTempereDeviceList.addAll(temperaDeviceService.getAllTemperaDevices().stream().map(a -> new SelectItem(a, a.getId().toString())).toList());
        return optionsTempereDeviceList;
    }


    /**
     * Called by TemperaDeviceDetailController after creating, editing or deleting temperaDevices in the ui.
     */
    public void update(){
        updateTemperaDeviceBeans();
    }
}
