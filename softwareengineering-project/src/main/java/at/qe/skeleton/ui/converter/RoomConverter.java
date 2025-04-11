package at.qe.skeleton.ui.converter;


import at.qe.skeleton.model.Room;
import at.qe.skeleton.services.room.RoomService;
import jakarta.faces.component.UIComponent;
import jakarta.faces.context.FacesContext;
import jakarta.faces.convert.Converter;
import jakarta.faces.convert.ConverterException;
import jakarta.faces.convert.FacesConverter;
import org.springframework.stereotype.Component;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.jsf.FacesContextUtils;

@Component
@FacesConverter(value = "roomConverter")
public class RoomConverter implements Converter<Room> {

    // string which is shown when no room is selected
    public static final String NO_ROOM_STRING = "No Room";

    private RoomService roomService;

    public RoomService getRoomService() {
        if(roomService == null){
            WebApplicationContext context = FacesContextUtils.getWebApplicationContext(FacesContext.getCurrentInstance());

            if(context == null){
                throw new IllegalStateException("FacesContext not found");
            }

            roomService = context.getBean(RoomService.class);
        }
        return roomService;
    }

    public String getAsString(FacesContext context, UIComponent component, Room room) {
        if (room == null) {
            return NO_ROOM_STRING;
        } else{
            return room.getRoomNumber();
        }
    }

    public Room getAsObject(FacesContext context, UIComponent component, String submittedValue) {
        if (submittedValue == null || submittedValue.isEmpty() || submittedValue.isBlank() || submittedValue.equals(NO_ROOM_STRING)) {
            return null;
        }

        try {
            return getRoomService().getRoomByRoomNumber(submittedValue);
        } catch (NumberFormatException e) {
            throw new ConverterException(submittedValue + " is not a valid Room ID");
        }
    }

}
