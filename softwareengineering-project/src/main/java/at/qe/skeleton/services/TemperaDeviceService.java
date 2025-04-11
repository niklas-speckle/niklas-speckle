package at.qe.skeleton.services;

import at.qe.skeleton.exceptions.EntityStillInUseException;
import at.qe.skeleton.model.*;
import at.qe.skeleton.repositories.*;
import at.qe.skeleton.exceptions.IdNotFoundException;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;


@Component
@Scope("application")
public class TemperaDeviceService {

    @Autowired
    private TemperaDeviceRepository temperaDeviceRepository;

    @Autowired
    private UserxRepository userxRepository;
    @Autowired
    private AccessPointRepository accessPointRepository;
    @Autowired
    private LogTemperaDeviceRepository logTemperaDeviceRepository;
    @Autowired
    private ClimateMeasurementRepository climateMeasurementRepository;

    /**
     * Retrieves a TemperaDevice from the database.
     * @param l the id of the TemperaDevice
     * @return the respective TemperaDevice
     */
    public TemperaDevice findTemperaDeviceById(long l) {
        return temperaDeviceRepository.findTemperaDeviceById(l);
    }

    /**
     * Retrieves the User that is connected to the given TemperaDevice from the database.
     * @param l the id of the TemperaDevice to which the User is connected
     * @return the User that is connected to the TemperaDevice
     */
    public Userx findUserOfTemperaDevice(Long l) {
        TemperaDevice temperaDevice = temperaDeviceRepository.findTemperaDeviceById(l);
        return userxRepository.findFirstByTemperaDevice(temperaDevice);
    }

    /**
     * Retrieves all TemperaDevices that have a connection to the given Room from the database.
     * @param room room object
     * @return a list of TemperaDevices that are connected to the given Room
     */
    public List<TemperaDevice> getTemperaDevicesByRoom(Room room) {

        List<AccessPoint> accessPoints = accessPointRepository.findAccessPointByRoom(room);

        if (accessPoints == null || accessPoints.isEmpty()) {
            return new ArrayList<>();
        }

        return accessPoints.stream()
                .flatMap(accessPoint -> Optional.ofNullable(accessPoint.getTemperaDevices()).orElse(Collections.emptyList()).stream())
                .collect(Collectors.toList());
    }

    /**
     * Deletes the given TemperaDevice from the database.
     * A TemperaDevice can only be deleted if it is not connected to a User. If it is still connected to an AccessPoint,
     * the respective TemperaDevice is deleted out of the AccessPoint's list of connected TemperaDevices and a log entry
     * (LogTemperaDevice) about the deletion is created.
     * @param temperaDevice the TemperaDevice to be deleted
     * @throws EntityStillInUseException if the TemperaDevice is still connected to a User
     */

    @Transactional
    @PreAuthorize("hasAnyAuthority('ADMINISTRATOR')")
    public void delete(TemperaDevice temperaDevice) throws EntityStillInUseException {
        if (userxRepository.findFirstByTemperaDevice(temperaDevice) != null) {
            throw new EntityStillInUseException("Tempera Device is still in use. Please remove the currently assigned user before deleting the device.");
        }
        AccessPoint connectedAccessPoint = accessPointRepository.findAccessPointByTemperaDevicesContains(temperaDevice);

        if (connectedAccessPoint != null) {
            connectedAccessPoint.getTemperaDevices().remove(temperaDevice);
            temperaDevice.setAccessPoint(null);
            temperaDeviceRepository.save(temperaDevice);
            accessPointRepository.save(connectedAccessPoint);
            addDeletionLog(temperaDevice, connectedAccessPoint);
        }

        List<Sensor> sensors = temperaDevice.getSensors();
        if (sensors != null && !sensors.isEmpty()) {
            for (Sensor sensor : sensors) {
                climateMeasurementRepository.deleteAllBySensor(sensor);
            }
        }

        temperaDeviceRepository.delete(findTemperaDeviceById(temperaDevice.getId()));
    }

    private void addDeletionLog(TemperaDevice temperaDevice, AccessPoint connectedAccessPoint) {
        connectedAccessPoint.getTemperaDevices().remove(temperaDevice);

        LogTemperaDevice logTemperaDevice = LogTemperaDevice.builder()
                .timestamp(LocalDateTime.now())
                .temperaDeviceId(temperaDevice.getId())
                .logStatus(LogStatus.DELETED)
                .build();

        saveLogEntry(connectedAccessPoint, logTemperaDevice);
    }


    /**
     * Logs a status change of a TemperaDevice in the corresponding AccessPoint.
     * @param temperaDevice the TemperaDevice that has changed its status
     */
    public void logStatusChange(TemperaDevice temperaDevice){
        AccessPoint accessPoint = accessPointRepository.findAccessPointByTemperaDevicesContains(temperaDevice);

        if (accessPoint != null) {

            LogTemperaDevice logTemperaDevice = LogTemperaDevice.builder()
                    .timestamp(LocalDateTime.now())
                    .temperaDeviceId(temperaDevice.getId())
                    .logStatus(LogStatus.UPDATED)
                    .newStatus(temperaDevice.getStatus())
                    .build();

            saveLogEntry(accessPoint, logTemperaDevice);
        }
    }

    /**
     * Logs an access point change of a TemperaDevice in the corresponding AccessPoints.
     * In the new AccessPoint a log entry is saved with the status CREATED.
     * In the old AccessPoint a log entry is saved with the status DELETED.
     */
    @Transactional
    public void logAccessPointChange(TemperaDevice temperaDevice) {
        AccessPoint oldAccessPoint = accessPointRepository.findAccessPointByTemperaDevicesContains(temperaDevice);
        AccessPoint newAccessPoint = temperaDevice.getAccessPoint();

        if (oldAccessPoint != null) {
            LogTemperaDevice logTemperaDeviceOld = LogTemperaDevice.builder()
                    .timestamp(LocalDateTime.now())
                    .temperaDeviceId(temperaDevice.getId())
                    .logStatus(LogStatus.DELETED)
                    .newStatus(temperaDevice.getStatus())
                    .build();

            saveLogEntry(oldAccessPoint, logTemperaDeviceOld);
        }

        if (newAccessPoint != null) {
            LogTemperaDevice logTemperaDeviceNew = LogTemperaDevice.builder()
                    .timestamp(LocalDateTime.now())
                    .temperaDeviceId(temperaDevice.getId())
                    .logStatus(LogStatus.CREATED)
                    .newStatus(temperaDevice.getStatus())
                    .build();

            saveLogEntry(newAccessPoint, logTemperaDeviceNew);
        }
    }

    /**
     * Saves a log entry in the corresponding AccessPoint.
     * @param accessPoint the AccessPoint to save the log entry in
     * @param logTemperaDevice the log entry which contains the information about tempera changes
     */
    private void saveLogEntry(AccessPoint accessPoint, LogTemperaDevice logTemperaDevice) {
        logTemperaDeviceRepository.save(logTemperaDevice);

        List<LogTemperaDevice> logTemperaDevices = accessPoint.getLogTemperaDevices();

        if (logTemperaDevices == null || logTemperaDevices.isEmpty()) {
            accessPoint.setLogTemperaDevices(new ArrayList<>(List.of(logTemperaDevice)));
        } else {
            logTemperaDevices.add(logTemperaDevice);
            accessPoint.setLogTemperaDevices(logTemperaDevices);
        }

        accessPointRepository.save(accessPoint);
    }

    /**
     * Finds the TemperaDevice the given Sensor is connected with.
     * @param sensor the Sensor to find the TemperaDevice for
     */
    public TemperaDevice findTemperaDeviceBySensor(Sensor sensor) {
        return temperaDeviceRepository.findTemperaDeviceBySensorsContains(sensor);
    }

    /**
     * Finds the first Warning of the given TemperaDevice and SensorType, if it exists.
     * @param temperaDevice the TemperaDevice for which the existing Warning should be found
     * @param sensorType the SensorType of the Warning
     * @return the searched Warning or null if it does not exist
     */
    public Warning getActiveWarning(TemperaDevice temperaDevice, SensorType sensorType){
        List<Warning> existingWarnings = temperaDevice.getWarnings();

        return existingWarnings.stream()
                .filter(warning -> warning.getSensorType().equals(sensorType))
                .findFirst()
                .orElse(null);
    }

    @PreAuthorize("hasAnyAuthority('ADMINISTRATOR')")
    public List<TemperaDevice> getAllTemperaDevices() {
        return temperaDeviceRepository.findAll();
    }


    /**
     * @return a new TemperaDevice with all SensorTypes initialized
     */
    @PreAuthorize("hasAnyAuthority('ADMINISTRATOR')")
    public TemperaDevice createTemperaDevice() {
        TemperaDevice newTemperaDevice = new TemperaDevice();
        newTemperaDevice.setSensors(new ArrayList<>());

        for (SensorType sensorType : SensorType.values()) {
            Sensor sensor = new Sensor();
            sensor.setSensorType(sensorType);
            newTemperaDevice.getSensors().add(sensor);
        }

        newTemperaDevice.setStatus(DeviceStatus.NOT_REGISTERED);

        return newTemperaDevice;
    }


    /**
     * Saves a TemperaDevice to the database. Also logs the change in the corresponding AccessPoint.
     * @param temperaDevice
     * @return
     */
    @PreAuthorize("hasAnyAuthority('ADMINISTRATOR')")
    public TemperaDevice save(TemperaDevice temperaDevice) {

        TemperaDevice temperaDeviceBeforeSave = temperaDeviceRepository.findTemperaDeviceById(temperaDevice.getId());

        // if tempera device is new no need for logging in AccessPoint
        if (temperaDevice.isNew()) {
            return temperaDeviceRepository.save(temperaDevice);
        }

        boolean hasStatusChanged = temperaDeviceBeforeSave.getStatus() != temperaDevice.getStatus();
        boolean hasAccessPointChanged = temperaDeviceBeforeSave.getAccessPoint() != temperaDevice.getAccessPoint();
        if(hasStatusChanged){
            logStatusChange(temperaDevice);
        }
        if(hasAccessPointChanged){
            logAccessPointChange(temperaDevice);
        }
        return temperaDeviceRepository.save(temperaDevice);
    }

    /**
     * Finds a TemperaDevice by its ID.
     * @param temperaDeviceId The ID of the device in format G4T2-TD-<ID>.
     * @throws IllegalArgumentException if the format of id is wrong
     * @throws IdNotFoundException if no active TemperaDevice is found in DB.
     * @return The TemperaDevice.
     */
    public TemperaDevice findTemperaDeviceById(String temperaDeviceId) throws IdNotFoundException {
        if(!temperaDeviceId.startsWith("G4T2-TD-")){
            throw new IllegalArgumentException("TemperaDeviceId must start with G4T2-TD-");
        }
        long temperaDeviceIDLong = Long.parseLong(temperaDeviceId.substring(8));
        TemperaDevice temperaDevice = findTemperaDeviceById(temperaDeviceIDLong);

        if(temperaDevice == null || !temperaDevice.getStatus().equals(DeviceStatus.ENABLED)) {
            throw new IdNotFoundException("No active TemperaDevice with id '" + temperaDeviceId + "' found");
        }
        return temperaDevice;
    }


    /**
     * This method is used to register a TemperaDevice after first rest contact with server
     * (only NOT_REGISTERED devices will have a status change)
     * 
     * @param temperaDevice TemperaDevice to be registered
     */
    public void register(TemperaDevice temperaDevice){
        if (temperaDevice.getStatus() == DeviceStatus.NOT_REGISTERED){
            temperaDevice.setStatus(DeviceStatus.DISABLED);
            temperaDeviceRepository.save(temperaDevice);
            logStatusChange(temperaDevice);
        }
    }
}
