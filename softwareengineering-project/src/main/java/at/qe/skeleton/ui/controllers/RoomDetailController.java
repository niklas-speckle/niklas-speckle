package at.qe.skeleton.ui.controllers;

import at.qe.skeleton.exceptions.EntityValidationException;
import at.qe.skeleton.exceptions.IdAlreadyExistsException;
import at.qe.skeleton.exceptions.EntityStillInUseException;
import at.qe.skeleton.model.Limits;
import at.qe.skeleton.model.Room;
import at.qe.skeleton.services.room.LimitService;
import at.qe.skeleton.services.room.RoomService;
import jakarta.faces.application.FacesMessage;
import jakarta.faces.context.FacesContext;
import lombok.Getter;
import lombok.Setter;
import org.primefaces.PrimeFaces;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.io.Serializable;
import java.util.Comparator;

@Getter
@Setter
@Component
@Scope("view")
public class RoomDetailController implements Serializable {

    @Autowired
    private transient RoomService roomService;

    @Autowired
    private transient LimitService limitService;

    @Autowired
    private RoomListController roomListController;

    /**
     * Currently displayed room.
     */
    private Room room;

    private Boolean temperatureLimitsChanged = false;
    private Boolean humidityLimitsChanged = false;
    private Boolean airQualityLimitsChanged = false;
    private Boolean lightLimitsChanged = false;

    /**
     * Sets the current room and ensures that the sensor list is always displayed in the same order.
     *
     * @param room the room to set
     */
    public void setRoom(Room room) {
        this.room = room;
        // ensure that sensor list is always in the same order when displayed
        room.getLimitsList().sort(Comparator.comparingInt(l -> l.getSensorType().ordinal()));
    }
    /**
     * Saves the current room. Validates that reasons for limit changes are provided.
     * Updates the room list and resets the current room after saving.
     */
    public void doSaveRoom(){
        for (Limits limit : room.getLimitsList()) {
            if (isLimitChanged(limit) && (limit.getReasonForChange() == null || limit.getReasonForChange().isEmpty())) {
                // Validation failed, prevent hiding the dialog
                FacesContext.getCurrentInstance().validationFailed();
                FacesContext.getCurrentInstance().addMessage("dialogMsg",
                        new FacesMessage(FacesMessage.SEVERITY_ERROR,
                                "Error",
                                "Please provide a reason for the change of limits for " + limit.getSensorType()));
                return;
            }
        }
        try {
            room = roomService.saveRoom(room);
            for (Limits limit : room.getLimitsList()) {
                // limits get saved when room gets saved, so they already have an id, check for updateDate instead to find out if they are new.
                if (limit.getCreateDate() == limit.getUpdateDate()) {
                    limit.setRoom(room);
                    limitService.saveLimit(limit);
                } else if (isLimitChanged(limit)) {
                    limitService.saveLimit(limit);
                }
            }
            roomListController.update();
            FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_INFO, "Success", "Room saved."));
            PrimeFaces.current().ajax().update("roomForm");
            doResetRoom();
        } catch (IdAlreadyExistsException | EntityValidationException e) {
            FacesContext.getCurrentInstance().validationFailed();
            FacesContext.getCurrentInstance().addMessage(null, new FacesMessage(
                    FacesMessage.SEVERITY_ERROR, "Error", e.getMessage()
            ));
        }
    }

    /**
     * Deletes the current room. Handles any exceptions if the room is still in use.
     * Updates the room list and resets the current room after deletion.
     */
    public void doDeleteRoom() {
        try {
            roomService.deleteRoom(room);
            roomListController.update();
        } catch (EntityStillInUseException e) {
            FacesContext.getCurrentInstance().addMessage(null, new FacesMessage(
                    FacesMessage.SEVERITY_ERROR, "Error deleting room", e.getMessage()
            ));
        } finally {
            doResetRoom();
        }
    }

    /**
     * Resets the current room to null and updates the room list.
     */
    public void doResetRoom() {
        this.room = null;
        resetLimitChangedFlags();
        roomListController.init();
    }

    /**
     * Resets the flags indicating changes in the room limits.
     */
    private void resetLimitChangedFlags() {
        temperatureLimitsChanged = false;
        humidityLimitsChanged = false;
        airQualityLimitsChanged = false;
        lightLimitsChanged = false;
    }

    /**
     * Initializes a new room and sorts its limits list.
     */
    public void doCreateRoom() {
        this.room = roomService.createRoom();
        room.getLimitsList().sort(Comparator.comparingInt(l -> l.getSensorType().ordinal()));
    }

    /**
     * Marks the limit as changed based on its sensor type and resets the reason for change.
     *
     * @param limit the limit that has been changed
     */
    public void onLimitChange(Limits limit) {
        switch (limit.getSensorType()) {
            case AIR_TEMPERATURE -> {
                temperatureLimitsChanged = true;
                room.getLimitsList().get(0).setReasonForChange(null);
            }
            case AIR_HUMIDITY -> {
                humidityLimitsChanged = true;
                room.getLimitsList().get(1).setReasonForChange(null);
            }
            case AIR_QUALITY -> {
                airQualityLimitsChanged = true;
                room.getLimitsList().get(2).setReasonForChange(null);
            }
            case LIGHT_INTENSITY -> {
                lightLimitsChanged = true;
                room.getLimitsList().get(3).setReasonForChange(null);
            }
        }
        // Ensure the limits list is displayed in the same order after a change
        room.getLimitsList().sort(Comparator.comparingInt(l -> l.getSensorType().ordinal()));
    }

    /**
     * Checks if a limit has been changed based on its sensor type.
     *
     * @param limit the limit to check
     * @return true if the limit has been changed, false otherwise
     */
    public Boolean isLimitChanged(Limits limit) {
        return switch (limit.getSensorType()) {
            case AIR_TEMPERATURE -> temperatureLimitsChanged;
            case AIR_HUMIDITY -> humidityLimitsChanged;
            case AIR_QUALITY -> airQualityLimitsChanged;
            case LIGHT_INTENSITY -> lightLimitsChanged;
        };
    }


}
