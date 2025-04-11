package at.qe.skeleton.repositories;

import at.qe.skeleton.model.ClimateMeasurement;
import at.qe.skeleton.model.Sensor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;


/**
 * Repository for managing {@link ClimateMeasurementRepository} entities.
 */
public interface ClimateMeasurementRepository extends AbstractRepository<ClimateMeasurement, Long> {

    Optional<ClimateMeasurement> findById(Long id);

    List<ClimateMeasurement> findAllBySensor(Sensor sensor);

    List<ClimateMeasurement> findAllBySensorAndTimeStampAfterAndTimeStampBefore(Sensor sensor, LocalDateTime after, LocalDateTime before);

    @Query(nativeQuery = true, value = "SELECT * FROM climate_measurement cm WHERE cm.sensor_id = :sensorId AND cm.time_stamp >= :minTimestamp ORDER BY cm.time_stamp DESC LIMIT 1")
    Optional<ClimateMeasurement> findNewestBySensorAndNotOlderThan(@Param("sensorId") Long sensorId, @Param("minTimestamp") LocalDateTime minTimestamp);

    void deleteAllBySensor(Sensor sensor);
}
