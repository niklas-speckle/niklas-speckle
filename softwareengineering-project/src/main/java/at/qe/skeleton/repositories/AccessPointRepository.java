package at.qe.skeleton.repositories;

import at.qe.skeleton.model.*;

import java.util.List;

/**
 * Repository for managing {@link AccessPoint} entities.
 */
public interface AccessPointRepository extends AbstractRepository<AccessPoint, Long> {
    AccessPoint findAccessPointById(Long id);

    List<AccessPoint> findAllByStatusIs(String status);

    AccessPoint findAccessPointByTemperaDevicesContains(TemperaDevice temperaDevice);

    /**
     * Finds the accesspoint for a given TemperaDevice.
     * @param temperaDevice
     * @return AccessPoint
     */
    AccessPoint findAccessPointByTemperaDevices(TemperaDevice temperaDevice);

    List<AccessPoint> findAccessPointByRoom(Room room);

}
