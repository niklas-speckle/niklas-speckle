package at.qe.skeleton.services;

import at.qe.skeleton.exceptions.EntityStillInUseException;
import at.qe.skeleton.model.AccessPoint;
import at.qe.skeleton.model.DeviceStatus;
import at.qe.skeleton.model.LogTemperaDevice;
import at.qe.skeleton.model.TemperaDevice;
import at.qe.skeleton.repositories.AccessPointRepository;
import at.qe.skeleton.repositories.LogTemperaDeviceRepository;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

@Service
public class AccessPointService {
    private static final String NETWORK_PREFIX = "G4T2-AP-";

    @Autowired
    private AccessPointRepository accessPointRepository;


    @Autowired
    private LogTemperaDeviceRepository logTemperaDeviceRepository;


    /**
     * checks whether the given AccessPoint is enabled.
     * @param accessPoint the AccessPoint to be checked
     * @return true if the AccessPoint is enabled, false otherwise
     */
    public boolean isAccessPointEnabled(AccessPoint accessPoint) {
        if (accessPoint == null) {
            return false;
        }

        return accessPoint.getStatus().equals(DeviceStatus.ENABLED);
    }

    public AccessPoint getAccessPointById(Long id) {
        return accessPointRepository.findAccessPointById(id);
    }

    /**
     * checks whether the given String is a valid AccessPointId: the prefix "G4T2-AP-" signals, that the given ID
     * belongs to an AccessPoint of our system. The remaining part of the ID should be a valid long number, that
     * resembles the ID of the AccessPoint, saved in our database. The found AccessPoint is only returned, if it is
     * enabled.
     *
     * @param accessPointIdString the ID of the AccessPoint
     * @return the active AccessPoint
     * @throws IllegalArgumentException if the given ID is not a valid AccessPointId, the ID could not be found in
     *                                  the database or the found AccessPoint is not enabled
     */

    public AccessPoint getAccessPointById(String accessPointIdString) {

        if (!accessPointIdString.startsWith(NETWORK_PREFIX)) {
            throw new IllegalArgumentException("AccessPointId must start with " + NETWORK_PREFIX);
        }
        long accessPointId = Long.parseLong(accessPointIdString.substring(8));
        AccessPoint accessPoint = accessPointRepository.findAccessPointById(accessPointId);

        if (accessPoint == null || !accessPoint.getStatus().equals(DeviceStatus.ENABLED)) {
            throw new IllegalArgumentException("No active AccessPoint with id '" + accessPointIdString + "' found");
        }

        return accessPoint;
    }

    /**
     * deletes the latest LogTemperaDevice of the given AccessPoint
     *
     * @param accessPoint the AccessPoint, whose latest LogTemperaDevice should be deleted
     */
    @Transactional
    public void resetLogTemperaDevice(AccessPoint accessPoint) {
        LogTemperaDevice latestLog = accessPoint.getLogTemperaDevices().remove(0);
        logTemperaDeviceRepository.delete(latestLog);
    }

    /**
     * Finds the AccessPoint that a TemperaDevice is assigned to. This is needed in the tempera View as the TemperaDevice is assigned to an AccessPoint.
     * The TemperaDevice itself does not know to which AccessPoint it is assigned.
     *
     * @param temperaDevice
     * @return
     */
    public AccessPoint getAccessPointByTemperaDevice(TemperaDevice temperaDevice) {
        if (temperaDevice == null || temperaDevice.getId() == null) {
            return null;
        }
        return accessPointRepository.findAccessPointByTemperaDevices(temperaDevice);
    }

    public List<AccessPoint> getAllAccessPoints() {
        return accessPointRepository.findAll();
    }

    @PreAuthorize("hasAnyAuthority('ADMINISTRATOR')")
    public AccessPoint save(AccessPoint accessPoint) {
        return accessPointRepository.save(accessPoint);
    }

    /**
     * deletes the given AccessPoint from the database, if no TemperaDevice is still connected to it
     *
     * @param accessPoint the AccessPoint to be deleted
     * @throws EntityStillInUseException if the AccessPoint is still connected to a TemperaDevice
     */
    @PreAuthorize("hasAnyAuthority('ADMINISTRATOR')")
    public void delete(AccessPoint accessPoint) throws EntityStillInUseException {
        List<TemperaDevice> connectedTemperaDevices = accessPoint.getTemperaDevices();

        if (connectedTemperaDevices != null && !connectedTemperaDevices.isEmpty()) {
            throw new EntityStillInUseException("A Tempera Device is still connected to this Access Point. Please remove the currently assigned Tempera Device in the 'Tempera Devices'-Menu before deleting the device.");
        }
        accessPointRepository.delete(accessPoint);
    }

    /**
     * Creates a new AccessPoint which is by default enabled. Only Admins are allowed to create AccessPoints.
     *
     * @return
     */
    @PreAuthorize("hasAnyAuthority('ADMINISTRATOR')")
    public AccessPoint createAccessPoint() throws EntityStillInUseException {
        AccessPoint accessPoint = new AccessPoint();
        accessPoint.setStatus(DeviceStatus.NOT_REGISTERED);
        accessPoint.setConnected(false);
        return accessPoint;
    }

    public AccessPoint getActiveAccessPointById(String accessPointId) {
        return getAccessPointById(accessPointId);
    }

    /**
     * Sets the current time as Last Connection (is called when an Access Point connects itself with the Server)
     * 
     * @param accessPoint The Access Point that connected itself.
     */
    public void updateLastConnection(AccessPoint accessPoint){
        accessPoint.setLastConnection(LocalDateTime.now());
        accessPointRepository.save(accessPoint);     
    }

    /**
     * Sets an Access Point to connected (called when connection between AP and Server is established).
     *
     * @param accessPoint The Access Point that is supposed to set to disconnected.
     */
    public void setConnected(AccessPoint accessPoint){
        accessPoint.setConnected(true);
        accessPointRepository.save(accessPoint);
    }

    /**
     * Sets an Access Point to disconnected (called when connection between AP and Server is lost).
     * 
     * @param accessPoint The Access Point that is supposed to set to disconnected.
     */
    public void setDisconnected(AccessPoint accessPoint){
        accessPoint.setConnected(false);
        accessPointRepository.save(accessPoint);
    }


    /**
     * This method is used to register an AccessPoint after first rest contact with server
     * (only NOT_REGISTERED devices will have a ststus change)
     * 
     * @param accessPoint AccessPoint to be registered
     * @throws EntityStillInUseException
     */
    public void register(AccessPoint accessPoint) throws EntityStillInUseException{
        if (accessPoint.getStatus() == DeviceStatus.NOT_REGISTERED){
            accessPoint.setStatus(DeviceStatus.DISABLED);
            accessPointRepository.save(accessPoint);
        }
    }


    /**
     * This method gets the AP from the repository if the device status is not_registered, otherwise exceptions will be thrown
     * 
     * @param accessPointIdString AP id as string that has to be found in database
     * @return AccessPoint if found in Database
     * @throws IllegalArgumentException if id is not of a not_registered device or of no device at all
     */
    public AccessPoint getNotRegisteredAccessPointById(String accessPointIdString) {

        if (!accessPointIdString.startsWith(NETWORK_PREFIX)) {
            throw new IllegalArgumentException("AccessPointId must start with " + NETWORK_PREFIX);
        }
        long accessPointId = Long.parseLong(accessPointIdString.substring(8));
        AccessPoint accessPoint = accessPointRepository.findAccessPointById(accessPointId);
        if (accessPoint == null || !accessPoint.getStatus().equals(DeviceStatus.NOT_REGISTERED)) {
            throw new IllegalArgumentException("No not registered AccessPoint with id '" + accessPointIdString + "' found");
        }

        return accessPoint;
    }

    /**
     * @return boolean if AccessPoint exists
     */
    public boolean doesAccessPointIDExist(String accessPointIdString){
        long accessPointId = Long.parseLong(accessPointIdString.substring(8));
        AccessPoint accessPoint = accessPointRepository.findAccessPointById(accessPointId);
        return accessPoint != null;
    }


    /**
     * @return boolean if String is a valid AccessPoint ID
     */
    public boolean isValidAccessPointIdString(String accessPointIdString) {

        List<String> splittedString = Arrays.stream(accessPointIdString.split("-")).toList();

        if(!accessPointIdString.startsWith(NETWORK_PREFIX) || splittedString.size() != 3){
            return false;
        }
        try {
            Long.parseLong(splittedString.get(2));
        } catch (NumberFormatException e) {
            return false;
        }
        return true;
    }


    /**
     * @return boolean if TemperaDevice is connected to AccessPoint
     */
    public boolean isTemperaDeviceConnectedToAccessPoint(TemperaDevice temperaDevice, AccessPoint accessPoint) {
        if(temperaDevice == null || accessPoint == null){
            return false;
        }
        return accessPoint.getTemperaDevices().contains(temperaDevice);
    }

}
