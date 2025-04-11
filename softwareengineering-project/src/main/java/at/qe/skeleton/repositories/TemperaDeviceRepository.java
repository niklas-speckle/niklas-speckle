package at.qe.skeleton.repositories;

import at.qe.skeleton.model.Sensor;
import at.qe.skeleton.model.TemperaDevice;
import at.qe.skeleton.model.Warning;

import java.util.List;

/**
 * Repository for managing {@link TemperaDevice} entities.
 */
public interface TemperaDeviceRepository extends AbstractRepository<TemperaDevice, Long> {
    TemperaDevice findTemperaDeviceById(Long id);

    List<TemperaDevice> findByStatusIs(String status);

    TemperaDevice findTemperaDeviceBySensorsContains(Sensor sensor);

    TemperaDevice findTemperaDeviceByWarningsContains(Warning warning);
}
