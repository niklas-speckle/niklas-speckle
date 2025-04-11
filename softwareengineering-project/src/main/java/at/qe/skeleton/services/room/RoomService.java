package at.qe.skeleton.services.room;

import at.qe.skeleton.exceptions.EntityValidationException;
import at.qe.skeleton.exceptions.IdAlreadyExistsException;
import at.qe.skeleton.exceptions.EntityStillInUseException;
import at.qe.skeleton.model.AccessPoint;
import at.qe.skeleton.model.Room;
import at.qe.skeleton.repositories.AccessPointRepository;
import at.qe.skeleton.repositories.RoomRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Component
@Scope("application")
public class RoomService {

    @Autowired
    private RoomRepository roomRepository;

    @Autowired
    private LimitService limitService;

    @Autowired
    private AccessPointRepository accessPointRepository;


    public List<Room> getAllRooms() {
        return roomRepository.findAll();
    }


    /**
     * Saves a room to the database.
     * @param room room object
     * @return the saved room
     * @throws IdAlreadyExistsException If a new room is saved with an already existing room number.
     * @throws EntityValidationException If the room number is null or the limits are not valid (upperlimit > lowerlimit).
     */


    @PreAuthorize("hasAnyAuthority('ADMINISTRATOR')")
    public Room saveRoom(Room room) throws IdAlreadyExistsException, EntityValidationException {

        if( room.getRoomNumber() == null || room.getRoomNumber().isEmpty() || room.getRoomNumber().isBlank()){
            throw new EntityValidationException("Room number must not be null.");
        }


        if(!limitService.areLimitsValid(room.getLimitsList())){
            throw new EntityValidationException("Lower limit must not be greater than upper limit.");
        }

        if(room.isNew() && (roomRepository.findByRoomNumber(room.getRoomNumber()) != null)){
                throw new IdAlreadyExistsException("Room with the room number '" + room.getRoomNumber() + "' already exists.");


        }
        return roomRepository.save(room);
    }

    /**
     * deletes the given room, if the room is not attached to an AccessPoint.
     * @param room room object to be deleted
     * @throws EntityStillInUseException If the room is still in use by an access point.
     */
    @Transactional
    @PreAuthorize("hasAnyAuthority('ADMINISTRATOR')")
    public void deleteRoom(Room room) throws EntityStillInUseException {
        List <AccessPoint> connectedAccessPoint = accessPointRepository.findAccessPointByRoom(room);

        if(connectedAccessPoint != null && !connectedAccessPoint.isEmpty()) {
            throw new EntityStillInUseException("Room is still in use by an access point.");
        }
        roomRepository.delete(room);
    }


    @PreAuthorize("hasAnyAuthority('ADMINISTRATOR')")
    public Room createRoom(){
        // creates a room with default limits
        return new Room();
    }

    /**
     * Finds room with roomNumber in Repo.
     * @param roomNumber id of room entity
     */
    public Room getRoomByRoomNumber(String roomNumber){
        return roomRepository.findByRoomNumber(roomNumber);
    }
}
