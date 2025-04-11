package at.qe.skeleton.repositories;

import at.qe.skeleton.model.Room;

/**
 * Repository for managing {@link Room} entities.
 */
public interface RoomRepository extends AbstractRepository<Room, Long> {
    Room findByRoomNumber(String roomNumber);


}
