package at.qe.skeleton.ui.controllers;


import at.qe.skeleton.exceptions.EntityStillInUseException;
import at.qe.skeleton.exceptions.EntityValidationException;
import at.qe.skeleton.model.AccessPoint;
import at.qe.skeleton.model.TemperaDevice;
import at.qe.skeleton.model.Userx;
import at.qe.skeleton.services.AccessPointService;
import at.qe.skeleton.services.TemperaDeviceService;
import at.qe.skeleton.services.UserService;
import jakarta.faces.application.FacesMessage;
import jakarta.faces.context.FacesContext;
import lombok.Getter;
import lombok.Setter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.interceptor.TransactionAspectSupport;

import java.io.Serializable;

@Component
@Scope("view")
public class TemperaDeviceDetailController implements Serializable {

    @Autowired
    private TemperaDeviceListController temperaDeviceListController;

    @Autowired
    private transient TemperaDeviceService temperaDeviceService;

    @Autowired
    private transient UserService userService;

    @Autowired
    private transient AccessPointService accessPointService;

    @Getter
    private TemperaDevice temperaDevice;

    // Selected user in tempera edit dialog
    @Getter
    @Setter
    private Userx selectedUser;

    // Selected access point in tempera edit dialog
    @Getter
    @Setter
    private AccessPoint selectedAccessPoint;

    @PreAuthorize("hasAnyAuthority('ADMINISTRATOR')")
    public TemperaDevice createTemperaDevice(){
        TemperaDevice temperaDeviceRval = temperaDeviceService.save(temperaDeviceService.createTemperaDevice());
        temperaDeviceListController.update();
        FacesContext.getCurrentInstance().addMessage(null, new FacesMessage(FacesMessage.SEVERITY_INFO, "Info", "TemperaDevice created"));
        return temperaDeviceRval;
    }

    @PreAuthorize("hasAnyAuthority('ADMINISTRATOR')")
    public void deleteTemperaDevice(){
        try {
            temperaDeviceService.delete(temperaDevice);
            temperaDeviceListController.update();
            FacesContext.getCurrentInstance().addMessage(null, new FacesMessage(FacesMessage.SEVERITY_INFO, "Info", "TemperaDevice deleted"));
        } catch (EntityStillInUseException e) {
            FacesContext.getCurrentInstance().addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR, "Error", e.getMessage()));
        }
        doResetTemperaDevice();
    }

    public void doResetTemperaDevice(){
        this.temperaDevice = null;
        this.selectedUser = null;
        this.selectedAccessPoint = null;
        temperaDeviceListController.update();
    }

    public void setTemperaDevice(TemperaDevice temperaDevice){
           this.temperaDevice = temperaDevice;
           this.selectedAccessPoint = accessPointService.getAccessPointByTemperaDevice(temperaDevice);
           this.selectedUser = userService.getUserByTemperaDevice(temperaDevice);
    }

    @Transactional
    public void saveTemperaDevice() {
        try {
            temperaDevice.setAccessPoint(selectedAccessPoint);
            temperaDevice = temperaDeviceService.save(temperaDevice);
            userService.setTemperaDevice(selectedUser, temperaDevice);
            temperaDeviceListController.update();
            FacesContext.getCurrentInstance().addMessage(null, new FacesMessage(FacesMessage.SEVERITY_INFO, "Info", "TemperaDevice saved"));
        } catch (EntityValidationException | IllegalArgumentException e) {
            FacesContext.getCurrentInstance().addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR, "Error", e.getMessage()));
            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
        } finally {
            doResetTemperaDevice();
        }

    }

}
