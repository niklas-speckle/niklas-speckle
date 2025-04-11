package at.qe.skeleton.services.climate;

import at.qe.skeleton.services.notifications.NotificationEvent;
import at.qe.skeleton.model.*;
import at.qe.skeleton.repositories.*;
import at.qe.skeleton.services.TemperaDeviceService;
import at.qe.skeleton.services.notifications.TokenService;
import at.qe.skeleton.services.timeTracking.TimeRecordService;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class WarningService {
    static final int TIME_DESTROY = 120;
    static final int TIME_DRAFT = 5;
    static final int TIME_UNSEEN = 30;
    static final int TIME_IGNORED = 60;
    static final int TIME_CONFIRMED = 15;

    @Autowired
    private TemperaDeviceService temperaDeviceService;

    @Autowired
    private WarningRepository warningRepository;

    @Autowired
    private TemperaDeviceRepository temperaDeviceRepository;

    @Autowired
    private TimeRecordService timeRecordService;

    @Autowired
    private ApplicationEventPublisher applicationEventPublisher;

    @Autowired
    private TokenService tokenService;

    /**
     * is triggered, whenever a new climateMeasurement comes in and its value violates the given limits.
     * The life cycle of the warning is as follows: if a transgression happens, first of all a DRAFT-warning is
     * created. If the transgression lasts more than 5 minutes the warning is set to UNSEEN and an event is published,
     * to inform all Listener about the new warning. The user can now react to the warning: he can set it to 'IGNORED'
     * or 'CONFIRMED' or simply do nothing. Depending on the user's reaction the warning will be renewed after a given
     * time. Also, the WorkMode of the User is taken into account, when he doesn't react to a Warning (a User out of
     * office or in a meeting cannot react to a Warning). To get old warnings (older than 120 min) out of the system,
     * they are deleted, when a new Warning comes in.
     * @param climateMeasurement the climateMeasurement that should be checked.
     * @return the warning that was created or updated.
     */
    @Transactional
    public Warning checkWarning(ClimateMeasurement climateMeasurement) {
        TemperaDevice temperaDevice = temperaDeviceService.findTemperaDeviceBySensor(climateMeasurement.getSensor());
        SensorType sensorType = climateMeasurement.getSensor().getSensorType();
        Warning warning = temperaDeviceService.getActiveWarning(temperaDevice, sensorType);

        if (warning == null) {
            return createDraftWarning(climateMeasurement, temperaDevice);
        }

        LocalDateTime timeStampMeasurement = climateMeasurement.getTimeStamp();
        LocalDateTime timeStampWarning = warning.getTimestamp();
        int minutesPassed = (int) Duration.between(timeStampWarning, timeStampMeasurement).toMinutes();

        WarningStatus warningStatus = warning.getWarningStatus();

        if (shallWarningBeRenewed(minutesPassed, warningStatus)) {
            return renewWarning(temperaDevice, warning, climateMeasurement);
        }

        if(warningStatus == WarningStatus.DRAFT && minutesPassed > TIME_DRAFT){
            warning.setMeasuredValue(climateMeasurement.getMeasuredValue());
            warning.setTimestamp(climateMeasurement.getTimeStamp());
            updateWarningStatus(warning, WarningStatus.UNSEEN, temperaDevice);
            return warning;
        }

        if(warningStatus == WarningStatus.UNSEEN && minutesPassed > TIME_UNSEEN){
            return checkUnseenWarning(temperaDevice, warning, climateMeasurement);
        }
        return null;
    }

    private Warning checkUnseenWarning(TemperaDevice temperaDevice, Warning warning, ClimateMeasurement climateMeasurement) {
        Userx user = temperaDeviceService.findUserOfTemperaDevice(temperaDevice.getId());
        WorkMode currentWorkMode = timeRecordService.getCurrentWorkModeOfUser(user);

        if(currentWorkMode.isInRoom()){
            return renewWarning(temperaDevice, warning, climateMeasurement);
        }

        warning.setTimestamp(climateMeasurement.getTimeStamp());
        warning.setMeasuredValue(climateMeasurement.getMeasuredValue());
        warningRepository.save(warning);
        return warning;
    }

    private Warning renewWarning(TemperaDevice temperaDevice, Warning warning, ClimateMeasurement climateMeasurement) {
        deleteWarning(warning, temperaDevice);
        return createDraftWarning(climateMeasurement, temperaDevice);
    }

    /**
     * deletes the given warning. Before deleting the warning from the database, it is removed from the TemperaDevice's
     * list of warnings. If the warning has a token, the token is disabled. If the warning is not a DRAFT-warning, the
     * WarningStatus is set to DELETED, so that a NotificationEvent is published. So all Listeners are informed about
     * the deletion of the warning.
     * @param warning Warning that should be deleted.
     * @param temperaDevice to which the warning belongs.
     */
    public void deleteWarning(Warning warning, TemperaDevice temperaDevice) {
        List<Warning> warnings = new ArrayList<>(temperaDevice.getWarnings());
        warnings.remove(warning);
        temperaDevice.setWarnings(warnings);

        temperaDeviceRepository.save(temperaDevice);
        Token token = warning.getToken();

        if (token != null) {
            tokenService.disableToken(token);
        }

        if (warning.getWarningStatus() != WarningStatus.DRAFT) {
            warning.setWarningStatus(WarningStatus.DELETED);
            applicationEventPublisher.publishEvent(new NotificationEvent(this, temperaDevice, warning));
        }

        warningRepository.delete(warning);

    }

    private Warning createDraftWarning(ClimateMeasurement climateMeasurement, TemperaDevice temperaDevice) {
        Warning warning = Warning.builder()
                .timestamp(climateMeasurement.getTimeStamp())
                .measuredValue(climateMeasurement.getMeasuredValue())
                .sensorType(climateMeasurement.getSensor().getSensorType())
                .warningStatus(WarningStatus.DRAFT)
                .build();
        warningRepository.save(warning);

        if(temperaDevice.getWarnings() == null) {
            temperaDevice.setWarnings(new ArrayList<>());
        }
        temperaDevice.getWarnings().add(warning);
        temperaDeviceRepository.save(temperaDevice);

        return warning;
    }

    private Boolean shallWarningBeRenewed(int minutesPassed, WarningStatus warningStatus) {
        if(minutesPassed > TIME_DESTROY){
            return true;
        }
        boolean isOldIgnoredWarning = warningStatus == WarningStatus.IGNORED && minutesPassed > TIME_IGNORED;
        boolean isOldConfirmedWarning = warningStatus == WarningStatus.CONFIRMED && minutesPassed > TIME_CONFIRMED;

        return (isOldConfirmedWarning || isOldIgnoredWarning);
    }

    /**
     * finds the Warning belonging to the warningId, the WarningStatus as well as the User's TemperaDevice and pass it
     * on tho updateWarningStatus(Warning warning, WarningStatus warningStatus, TemperaDevice temperaDevice).
     * @param warningId id of the warningId that should be updated.
     * @param status ordinal value of the WarningStatus that should be set.
     */
    @Transactional
    public void updateWarningStatus(Long warningId, Integer status) {
        Warning warning = warningRepository.findWarningById(warningId);
        WarningStatus warningStatus = WarningStatus.values()[status];
        TemperaDevice temperaDevice = temperaDeviceRepository.findTemperaDeviceByWarningsContains(warning);
        warning.setTimestamp(LocalDateTime.now());
        updateWarningStatus(warning, warningStatus, temperaDevice);
    }

    /**
     * sets the WarningStatus of the warning to the given WarningStatus and saves the warning.
     * If necessary a token is generated (WarningStatus has been DRAFT) or disabled (WarningStatus CONFIRMED/IGNORED)
     * by TokenService's checkToken().
     * The Status update is published as a NotificationEvent so that the user can be informed accordingly.
     * @param warning Warning that should be updated.
     * @param warningStatus WarningStatus that should be set.
     * @param temperaDevice to which the warning belongs.
     */
    @Transactional
    public void updateWarningStatus(Warning warning, WarningStatus warningStatus, TemperaDevice temperaDevice) {
        warning.setWarningStatus(warningStatus);
        warningRepository.save(warning);

        tokenService.checkToken(warning);

        applicationEventPublisher.publishEvent(new NotificationEvent(this, temperaDevice, warning));
    }


}
