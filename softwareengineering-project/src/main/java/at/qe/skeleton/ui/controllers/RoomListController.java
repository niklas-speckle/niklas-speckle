package at.qe.skeleton.ui.controllers;


import at.qe.skeleton.model.Room;
import at.qe.skeleton.services.room.RoomService;
import at.qe.skeleton.ui.converter.RoomConverter;
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
public class RoomListController implements Serializable {

    @Autowired
    private transient RoomService roomService;

    @Getter
    private List<Room> rooms;


    @PostConstruct
    public void init() {
        rooms = roomService.getAllRooms();
    }

    public List<SelectItem> getSelectItemsRoomList() {
        List<SelectItem> optionList = new ArrayList<>();
        optionList.add(new SelectItem(null, RoomConverter.NO_ROOM_STRING));
        optionList.addAll(roomService.getAllRooms().stream().map(r -> new SelectItem(r, r.getRoomNumber())).toList());
        return optionList;
    }

    /**
     * Updates the list of rooms. Method is called by RoomDetailController after editing, creating or deleting rooms in the ui.
     */
    public void update() {
        rooms = roomService.getAllRooms();
    }
}
