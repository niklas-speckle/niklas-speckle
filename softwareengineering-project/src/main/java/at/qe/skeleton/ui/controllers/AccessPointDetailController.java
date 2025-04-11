package at.qe.skeleton.ui.controllers;

import at.qe.skeleton.exceptions.EntityStillInUseException;
import at.qe.skeleton.model.AccessPoint;
import at.qe.skeleton.model.DeviceStatus;
import at.qe.skeleton.services.AccessPointService;
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
import java.util.ArrayList;

@Component
@Scope("view")
public class AccessPointDetailController implements Serializable {

    /**
     * To trigger update of the accessPointList after editing, deleting or creating an accessPoint.
     */
    @Autowired
    private AccessPointListController accessPointListController;

    @Autowired
    private transient AccessPointService accessPointService;

    @Getter
    private AccessPoint accessPoint;

    @Getter
    @Setter
    private DeviceStatus deviceStatus;


    @PreAuthorize("hasAnyAuthority('ADMINISTRATOR')")
    public AccessPoint createAccessPoint() throws EntityStillInUseException {
        AccessPoint accessPointRval = accessPointService.save(accessPointService.createAccessPoint());
        accessPointListController.update();
        FacesContext.getCurrentInstance().addMessage(null, new FacesMessage(FacesMessage.SEVERITY_INFO, "Info", "AccessPoint created"));
        return accessPointRval;
    }

    @PreAuthorize("hasAnyAuthority('ADMINISTRATOR')")
    public void deleteAccessPoint() {
        try {
            accessPointService.delete(accessPoint);
            accessPointListController.update();
            FacesContext.getCurrentInstance().addMessage(null, new FacesMessage(FacesMessage.SEVERITY_INFO, "Info", "AccessPoint deleted"));
        } catch (EntityStillInUseException e) {
            FacesContext.getCurrentInstance().addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR, "Error", e.getMessage()));
        }
        doResetAccessPoint();
    }

    public void setAccessPoint(AccessPoint accessPoint){
        this.accessPoint = accessPoint;
        this.deviceStatus = accessPoint.getStatus();
        if(accessPoint.getTemperaDevices() == null){
            accessPoint.setTemperaDevices(new ArrayList<>());
        }
    }

    public void doResetAccessPoint(){
        this.accessPoint = null;
        this.deviceStatus = null;
        accessPointListController.update();
    }

    @Transactional
    public void saveAccessPoint(){
        try {
            accessPoint.setStatus(deviceStatus);
            accessPointService.save(accessPoint);
            accessPointListController.update();
            FacesContext.getCurrentInstance().addMessage(null, new FacesMessage(FacesMessage.SEVERITY_INFO, "Info", "AccessPoint saved"));
        } catch (Exception e) {
            FacesContext.getCurrentInstance().addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR, "Error", e.getMessage()));
            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
        } finally {
            doResetAccessPoint();
        }
    }


}