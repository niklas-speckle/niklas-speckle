package at.qe.skeleton.rest.controllers;


import at.qe.skeleton.model.*;
import at.qe.skeleton.exceptions.EntityStillInUseException;
import at.qe.skeleton.exceptions.EntityValidationException;
import at.qe.skeleton.model.notifications.*;
import at.qe.skeleton.model.notifications.visitorpattern.APINotificationValidationVisitorInterface;
import at.qe.skeleton.rest.dto.LogTemperaDeviceDTO;
import at.qe.skeleton.rest.dto.MeasurementDTO;
import at.qe.skeleton.rest.dto.APINotificationDTO;
import at.qe.skeleton.rest.dto.TimeRecordDTO;
import at.qe.skeleton.rest.mapper.LogTemperaDeviceDTOMapper;
import at.qe.skeleton.rest.mapper.MeasurementMapper;
import at.qe.skeleton.rest.mapper.APINotificationDeviceBodyDTOMapper;
import at.qe.skeleton.rest.mapper.TimeRecordMapper;
import at.qe.skeleton.services.*;

import at.qe.skeleton.services.climate.ClimateMeasurementService;
import at.qe.skeleton.services.climate.WarningService;
import at.qe.skeleton.services.notifications.NotificationService;
import at.qe.skeleton.services.notifications.TokenService;
import at.qe.skeleton.services.timeTracking.TimeRecordService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;

@org.springframework.web.bind.annotation.RestController
public class RestController implements APINotificationValidationVisitorInterface {

    @Autowired
    private ClimateMeasurementService climateMeasurementService;

    @Autowired
    private AccessPointService accessPointService;

    @Autowired
    private TemperaDeviceService temperaDeviceService;

    @Autowired
    private MeasurementMapper measurementMapper;

    @Autowired
    private TimeRecordMapper timeRecordMapper;

    @Autowired
    private LogTemperaDeviceDTOMapper logTemperaDeviceDTOMapper;


    @Autowired
    private TimeRecordService timeRecordService;

    @Autowired
    private WarningService warningService;

    @Autowired
    private TokenService tokenService;

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private APINotificationDeviceBodyDTOMapper apiNotificationDeviceBodyDTOMapper;

    /**
     * provides a Post-Method accessible under /api/timerecords/{accessPointId} to transmit a TimeRecord.
     * The TimeRecordDTO is converted to a new TimeRecord and saved in the database. Furthermore the former, open
     * TimeRecord is closed and saved as well.
     *
     * @param timeRecord    The TimeRecord to be saved.
     * @param accessPointId The id of the AccessPoint, that transmit the data.
     * @return ResponseEntity<TimeRecordDTO> The TimeRecord that was sent. StatusCodes: 201 if successful, 403 if
     * AccessPoint is not active/not found/not valid, 500 if an error occurred.
     */

    @PostMapping("/api/timerecords/{accessPointId}")
    public ResponseEntity<TimeRecordDTO> createTimeRecord(@RequestBody TimeRecordDTO timeRecord, @PathVariable String accessPointId) throws EntityValidationException {
        try {
            AccessPoint accessPoint = accessPointService.getActiveAccessPointById(accessPointId);
            updateConnection(accessPoint);

            TimeRecord newTimeRecord = timeRecordMapper.mapFrom(timeRecord);

            timeRecordService.saveNewAndCloseOldTimeRecord(newTimeRecord);

        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        } catch (DataAccessException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }

        return ResponseEntity.status(201).body(timeRecord);
    }

    /**
     * provides a Post-Method accessible under /api/measurements/{accessPointId} to transmit a ClimateMeasurements.
     * The ClimateMeasurementsDTO is mapped to new ClimateMeasurements and saved in the database.
     *
     * @param measurementDTO The ClimateMeasurements to be saved.
     * @param accessPointId  The id of the AccessPoint, that transmit the data.
     * @return ResponseEntity<MeasurementDTO> The MeasurementDTO that was sent. StatusCodes: 201 if successful, 403 if
     * AccessPoint is not active/not found/not valid, 500 if an error occurred.
     */
    @PostMapping("/api/measurements/{accessPointId}")
    public ResponseEntity<MeasurementDTO> createMeasurement(@RequestBody MeasurementDTO measurementDTO, @PathVariable String accessPointId) throws EntityValidationException {
        try {
            //Exception will be thrown if accessPointId is not valid/active
            AccessPoint accessPoint = accessPointService.getActiveAccessPointById(accessPointId);
            updateConnection(accessPoint);
            List<ClimateMeasurement> climateMeasurements = measurementMapper.mapFrom(measurementDTO);

            climateMeasurementService.saveAll(climateMeasurements);
            climateMeasurementService.checkLimits(climateMeasurements);

            return ResponseEntity.status(201).body(measurementDTO);


        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        } catch (DataAccessException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }

    }

    /**
     * provides a Get-Method accessible under /api/temperaDevices/{accessPointId} to get Updates about TemperaDevices
     * of a given AccessPoint.
     * Each call of this method will return the latest LogTemperaDevice of the given AccessPoint.
     *
     * @param accessPointId The id of the AccessPoint, that request the data.
     * @return ResponseEntity<LogTemperaDeviceDTO> The LogTemperaDeviceDTO that was requested. StatusCodes: 200 if
     * successful, 204 if no LogTemperaDevice was found, 403 if AccessPoint is not active/not found/not valid, 500 if an
     * error occurred.
     */

    @GetMapping("/api/temperaDevices/{accessPointId}")
    public ResponseEntity<LogTemperaDeviceDTO> getLogTemperaDeviceByAccessPointId(@PathVariable String accessPointId) throws EntityValidationException {
        AccessPoint accessPoint;

        try {
            accessPoint = accessPointService.getActiveAccessPointById(accessPointId);
            updateConnection(accessPoint);

            LogTemperaDevice logTemperaDevice = accessPoint.getLogTemperaDevices().get(0);

            return ResponseEntity.ok(logTemperaDeviceDTOMapper.mapTo(logTemperaDevice));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        } catch (IndexOutOfBoundsException e) {
            return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
        } catch (DataAccessException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }

    }

    /**
     * provides a Delete-Method accessible under /api/temperaDevices/{accessPointId} to get Updates about TemperaDevices
     * of a given AccessPoint.
     * Each call of this method will delete the latest LogTemperaDevice of the given AccessPoint.
     *
     * @param accessPointId The id of the AccessPoint, that request the data.
     * @return ResponseEntity<String> The message that the latest LogTemperaDevice was deleted. StatusCodes: 200 if
     * successful, 403 if AccessPoint is not active/not found/not valid, 500 if an error occurred.
     */
    @DeleteMapping("/api/temperaDevices/{accessPointId}")
    public ResponseEntity<String> deleteLogTemperaDeviceByAccessPointId(@PathVariable String accessPointId) throws EntityValidationException{
        AccessPoint accessPoint;

        try {
            accessPoint = accessPointService.getActiveAccessPointById(accessPointId);
            updateConnection(accessPoint);
            accessPointService.resetLogTemperaDevice(accessPoint);

        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        } catch (DataAccessException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }

        return ResponseEntity.ok("Deleted latest LogTemperaDevice for AccessPoint with id: " + accessPointId);
    }

    /**
     * provides a Get-Method accessible under /api/warnings to set the status of a specific warning.
     *
     * @param token  The token of the warning, that should be updated.
     * @param status The new status of the warning resembled by its Ordinal-value. (2 = IGNORED, 3 = CONFIRMED)
     * @return ResponseEntity<String> A String, that informs the User whether the update was successful.
     */
    @GetMapping("/api/warnings")
    public ResponseEntity<String> updateWarningStatus(@RequestParam("token") String token,
                                                      @RequestParam("status") Integer status) {

        if (status != WarningStatus.CONFIRMED.ordinal() && status != WarningStatus.IGNORED.ordinal()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Warning can only be confirmed or ignored.");
        }

        try {
            if (tokenService.isTokenValid(token)) {
                Warning warning = tokenService.getWarningByToken(token);
                warningService.updateWarningStatus(warning.getId(), status);
                String response = "The violation of your room climate limits regarding "
                        + warning.getSensorType().getName() + " was "
                        + WarningStatus.values()[status].toString() + " by you.";

                return ResponseEntity.ok(response);
            } else {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Token is not valid");
            }
        } catch (NullPointerException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Warning not found");
        } catch (DataAccessException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error while updating Warning");
        }
    }

    /**
     * Recieves messages from accessPoints and transmits them to the according services (given by id in URL)
     * if message was caused by temperaDevice or accessPoint is given in the body
     * accessPoint messages will be transmitted to all admins
     * temperaDevice messages will be transmitted to the according user and all admins
     * 
     * http-status unauthorized marks requests that are not allowed due to wrong devices or status - these messages will be deleted on the AP
     *
     * @param apiNotificationDTO The message object to be transmitted
     * @param deviceID           The ID of the device in format G4T2-AP-<ID>
     * @return
     */
    @PostMapping("/api/messages/{deviceID}")
    public ResponseEntity<String> createMessage(@RequestBody APINotificationDTO apiNotificationDTO, @PathVariable String deviceID) throws EntityValidationException, EntityStillInUseException {

        Optional<ResponseEntity<String>> validationResponseEntity = validateAccessPointID(deviceID);

        if(validationResponseEntity.isPresent()){return validationResponseEntity.get();}

        try {
            AccessPoint sender = accessPointService.getAccessPointById(deviceID);

            if(!accessPointService.isAccessPointEnabled(sender)){
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("AccessPoint is not allowed to send messages because it is not enabled.");
            }

            updateConnection(sender);

            APINotificationDeviceBody apiNotification = apiNotificationDeviceBodyDTOMapper.mapFrom(apiNotificationDTO);

            validationResponseEntity = apiNotification.accept(this, sender);
            if(validationResponseEntity.isPresent()){return validationResponseEntity.get();}

            notificationService.notificationFromAccessPoint(apiNotification);

            return ResponseEntity.ok("Message sent successfully");

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
        
    }


    /**
     * This method is used to update the LastConnection timestamp as well as check if a notification about the Connection establishment between Server
     * and Access Point has to be sent.
     * 
     * @param accessPoint The Access Point that established a connection/ needs to be updated
     * @throws EntityValidationException
     */
    public void updateConnection(AccessPoint accessPoint) throws EntityValidationException{
        accessPointService.updateLastConnection(accessPoint);
        if (!accessPoint.isConnected()){
            accessPointService.setConnected(accessPoint);
            APINotification notification = new APINotification();
            notification.setDeviceType(DeviceType.SERVER);
            notification.setNotificationType(NotificationType.INFO);
            notification.setMessage("Connection to AP %d has been successfully established at ".formatted(accessPoint.getId()) + LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS).toString().replace("T", " "));
            notificationService.addNotificationToBell(accessPoint, notification);
        }
    }

    
    /**
     * This method checks if the given deviceId of an AP is not-registered. If that is the case it will register device and send a
     * notification to all admins about the successful registration.
     * 
     * @param deviceId String Id of AP sent via http request
     * @return boolean indicating if the given device was not_registered or not
     * @throws EntityStillInUseException
     * @throws EntityValidationException
     */
    private boolean checkIfAPNotRegistered(String deviceId) throws EntityStillInUseException, EntityValidationException {
        try{
            AccessPoint accessPoint = accessPointService.getNotRegisteredAccessPointById(deviceId);
            if (accessPoint == null){
                return false;
            }
            accessPointService.register(accessPoint);
            APINotification notification = new APINotification();
            notification.setDeviceType(DeviceType.SERVER);
            notification.setNotificationType(NotificationType.INFO);
            notification.setMessage("AP %d has been successfully registered".formatted(accessPoint.getId()));
            notificationService.addNotificationToBell(accessPoint, notification);
            return true;

        } catch (IllegalArgumentException e){
            return false;
        }    
    } 


    /**
     * This method checks if the given TemperaDevice is not-registered. If that is the case it will register the device and send a notification
     * to all admins (and to the user linked to the device if one was already linked) about the successful registration.
     * 
     * @param temperaDevice Tempera Device to ckeck if not-registered
     * @return boolean indicating if device was not-registered or not
     * @throws EntityStillInUseException
     * @throws EntityValidationException
     */
    private boolean checkIfTDNotRegistered(TemperaDevice temperaDevice) throws EntityValidationException {
        try{
            if (temperaDevice == null){
                return false;
            }
            if (temperaDevice.getStatus().equals(DeviceStatus.NOT_REGISTERED)){
                temperaDeviceService.register(temperaDevice);
                APINotification notification = new APINotification();
                notification.setDeviceType(DeviceType.SERVER);
                notification.setNotificationType(NotificationType.INFO);
                notification.setMessage("TD %d has been successfully registered".formatted(temperaDevice.getId()));
                notificationService.addNotificationToBell(temperaDevice, notification);
                return true;
            }
            return false;
        } catch (IllegalArgumentException e){
            return false;
        }    
    }


    /**
     * checks AccessPoint deviceID for createMessage.
     * - string must have form G4T2-AP-<id>
     * - device must exist
     * - device must be registered
     * @return ResponseEntity<String> if deviceID is invalid. Else returns null
     */
    public Optional<ResponseEntity<String>> validateAccessPointID(String deviceID) throws EntityValidationException, EntityStillInUseException {
        if (!accessPointService.isValidAccessPointIdString(deviceID)) {
            return Optional.of(ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid deviceID. ID must have the form: G4T2-AP-<id>."));
        }

        if(!accessPointService.doesAccessPointIDExist(deviceID)){
            return Optional.of(ResponseEntity.status(HttpStatus.BAD_REQUEST).body("AccessPoint not found"));
        }
        // check for not registered
        if (checkIfAPNotRegistered(deviceID)){
            return Optional.of(ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("AccessPoint has been registered, but is not yet enabled"));
        }

        return Optional.empty();
    }



    /**
     * validates APINotificationAPBody
     */
    public Optional<ResponseEntity<String>> visit(APINotificationAPBody apiNotificationAPBody, AccessPoint sender){
        if(!notificationService.doAccessPointAndAPINotificationMatch(sender, apiNotificationAPBody)){
            return Optional.of(ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("AccessPoint is only allowed to send AP Messages about itself"));
        }

        return Optional.empty();
    }

    /**
     * validates APINotificationTDBody
     */
    public Optional<ResponseEntity<String>> visit(APINotificationTDBody apiNotificationTDBody, AccessPoint sender) throws EntityValidationException, EntityStillInUseException {
        TemperaDevice temperaDevice = temperaDeviceService.findTemperaDeviceById(apiNotificationTDBody.getDeviceId());
        // validate tempera device
        if (!accessPointService.isTemperaDeviceConnectedToAccessPoint(temperaDevice, sender)) {
            return Optional.of(ResponseEntity.status(HttpStatus.BAD_REQUEST).body("TemperaDevice is not connected to this AccessPoint"));
        }

        if (checkIfTDNotRegistered(temperaDevice)){
            return Optional.of(ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("TemperaDevice has been registered, but is not yet enabled"));
        }

        if(!temperaDevice.isEnabled()){
            return Optional.of(ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("TemperaDevice is not enabled"));
        }

        return Optional.empty();
    }




}