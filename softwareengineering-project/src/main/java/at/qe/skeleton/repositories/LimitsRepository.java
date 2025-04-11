package at.qe.skeleton.repositories;


import at.qe.skeleton.model.Limits;
import at.qe.skeleton.model.Room;
import at.qe.skeleton.model.SensorType;

import java.util.Optional;

/**
 * Repository for managing {@link Limits} entities.
 */
public interface LimitsRepository extends AbstractRepository<Limits, Long> {

    Optional<Limits> findFirstByRoomAndSensorTypeIs(Room room, SensorType sensorType);


}
