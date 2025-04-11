package at.qe.skeleton.services.room;


import at.qe.skeleton.model.Limits;
import at.qe.skeleton.model.Room;
import at.qe.skeleton.model.SensorType;
import at.qe.skeleton.model.Userx;
import at.qe.skeleton.repositories.LimitsRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.NoSuchElementException;

@Component
@Scope("application")
public class LimitService {

    @Autowired
    LimitsRepository limitsRepository;


    /**
     * Checks if all limits of a list of limits are valid (upperlimit > lowerlimit).
     * @param limits list of limits
     * @return true if all limits are valid, false otherwise
     */
    public boolean areLimitsValid(List<Limits> limits){
        for(Limits limit : limits){
            if(!limit.isValid()) {
                return false;
            }
        }
        return true;
    }

    /**
     * Retrieves the temperature limits configured for the specified user and sensor type.
     *
     * @param user The user for whom to retrieve the limits.
     * @param sensorType The type of sensor for which to retrieve the limits.
     * @return The temperature limits for the specified user and sensor type.
     * @throws NoSuchElementException If no TemperaDevice is found for the user or if no temperature limits are configured for the room.
     */
    public Limits getLimitsForUserAndSensorType(Userx user, SensorType sensorType) throws NoSuchElementException {
        if (user.getTemperaDevice() == null) {
            throw new NoSuchElementException("No TemperaDevice found for user "+user.getUsername());
        } else {
            return getLimitsForRoomAndSensorType(user.getTemperaDevice().getAccessPoint().getRoom(), sensorType);
        }
    }

    /**
     * Retrieves the limits configured for the specified room and sensor type.
     *
     * @param room The room for which to retrieve the limits.
     * @param sensorType The type of sensor for which to retrieve the limits.
     * @return The limits for the specified room and sensor type.
     * @throws NoSuchElementException If no limits are configured for the room.
     */
    public Limits getLimitsForRoomAndSensorType(Room room, SensorType sensorType) throws NoSuchElementException {
        return limitsRepository.findFirstByRoomAndSensorTypeIs(room, sensorType).orElseThrow(() -> new NoSuchElementException("No limits for"+sensorType+"found for room "+room.getRoomNumber()+"."));
    }

    public Limits findLimitBySensorType(List<Limits> limits, SensorType sensorType) {
        if (limits == null) {
            return null;
        }

        for (Limits limit : limits) {
            if (limit.getSensorType().equals(sensorType)) {
                return limit;
            }
        }
        return null;
    }


    public Limits saveLimit(Limits limits) {
        return limitsRepository.save(limits);
    }
}
