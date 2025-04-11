package at.qe.skeleton.repositories;

import at.qe.skeleton.model.Sensor;

/**
 * Repository for managing {@link Sensor} entities.
 */
public interface SensorRepository extends AbstractRepository<Sensor, Long> {

    Sensor findSensorById(Long id);



}
