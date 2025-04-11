package at.qe.skeleton.services.notifications;

import at.qe.skeleton.exceptions.EntityValidationException;
import at.qe.skeleton.model.*;
import at.qe.skeleton.model.notifications.*;
import at.qe.skeleton.repositories.NotificationRepository;
import at.qe.skeleton.repositories.UserxRepository;
import at.qe.skeleton.services.*;
import at.qe.skeleton.services.climate.WarningService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

@Component
@Scope("application")
public class NotificationService {

    @Autowired
    private NotificationRepository notificationRepository;

    @Autowired
    private AccessPointService accessPointService;

    @Autowired
    private TemperaDeviceService temperaDeviceService;

    @Autowired
    private UserService userService;

    @Autowired
    private TokenService tokenService;

    @Autowired
    private WarningService warningService;

    @Autowired
    private UserxRepository userxRepository;

    public List<Notification> getNotificationsByUser(Userx user) {
        return notificationRepository.findAllByUser(user);
    }

    /**
     * Sends notifications to all admins and user (for TemperaDevices) accordingly.
     * @param device AccessPoint or TemperaDevice
     * @param notification
     */
    public void addNotificationToBell(Device device, APINotification notification) throws EntityValidationException{
        addNotificationToAllAdmins(notification);

        if(device instanceof TemperaDevice temperaDevice){
            Userx user = userService.findUserByTemperaDevice(temperaDevice);
            if (user != null){
                notification.setUser(user);
                notification = notificationRepository.save(notification);
                if(user.getNotifications() == null){
                    user.setNotifications(new ArrayList<>(List.of(notification)));
                } else {
                    user.getNotifications().add(notification);
                }
                userxRepository.save(user);
            }
        }
    }

    /**
     * Adds the given WarningNotification to the NotificationList of the bell-symbol in the User's topbar.
     * @param warningNotification WarningNotification to be added
     */
    public void addNotificationToBell(WarningNotification warningNotification) {
        notificationRepository.save(warningNotification);
    }

    /**
     * Adds a copy of notification to each user with userRole ADMINISTRATOR
     * @param notification
     * @throws EntityValidationException
     */
    private void addNotificationToAllAdmins(APINotification notification) throws EntityValidationException{
        for(Userx admin : userService.getUsersByRole(UserxRole.ADMINISTRATOR)){
            APINotification notificationCopy = new APINotification(notification);
            notificationCopy.setUser(admin);
            Notification notificationSaved = notificationRepository.save(notificationCopy);
            if(admin.getNotifications() == null){
                admin.setNotifications(new ArrayList<>(List.of(notificationSaved)));
            } else {
                admin.getNotifications().add(notificationSaved);
            }
            userxRepository.save(admin);
        }
    }

    /**
     * Deletes the given Notification from the database. Users can only delete their own notifications
     * @param notification
     * @throws EntityValidationException if the Notification has no user
     */
    @PreAuthorize("principal.username eq #notification.user.username")
    public void deleteNotification(Notification notification) throws EntityValidationException {

        if(notification == null){
            throw new IllegalArgumentException("Notification is null");
        }

        Userx user = notification.getUser();

        user.getNotifications().remove(notification);
        userService.saveUser(user);
        notificationRepository.delete(notification);
    }

    /**
     * checks whether the given Notification is a WarningNotification. If so, the WarningStatus of the corresponding
     * Warning is set to CONFIRMED.
     * @param notification Notification to be confirmed
     */
    public void confirmNotification(Notification notification) {
        if (notification instanceof WarningNotification warningNotification) {
            Token token = warningNotification.getToken();

            if (tokenService.isTokenValid(warningNotification.getToken().getContent())) {
                Warning warning = tokenService.getWarningByToken(token.getContent());
                warningService.updateWarningStatus(warning.getId(), WarningStatus.CONFIRMED.ordinal());

            }
        }
    }

    /**
     * checks whether the given Notification is a WarningNotification. If so, the WarningStatus of the corresponding
     * Warning is set to IGNORED.
     * @param notification Notification to be ignored
     */
    public void ignoreNotification(Notification notification) {
        if (notification instanceof WarningNotification warningNotification) {
            Token token = warningNotification.getToken();

            if (tokenService.isTokenValid(warningNotification.getToken().getContent())) {
                Warning warning = tokenService.getWarningByToken(token.getContent());
                warningService.updateWarningStatus(warning.getId(), WarningStatus.IGNORED.ordinal());

            }
        }
    }

    /**
     * checks whether the given Token belongs to one of the user's Notifications. If so, the Notification is deleted.
     * @param user User to which the Notification belongs
     * @param token Token associated with the Notification
     */
    public void deleteNotification(Userx user, Token token) {
        List<Notification> notifications = notificationRepository.findAllByUser(user);
        String tokenValue = token.getContent();
        for(Notification notification : notifications){
            if (notification instanceof WarningNotification warningNotification) {
                if(warningNotification.getToken().getContent().equals(tokenValue)){
                    notificationRepository.delete(notification);
                    notifications.remove(notification);
                    user.setNotifications(notifications);
                    break;
                }
            }
        }
        userxRepository.save(user);
    }

    public void notificationFromAccessPoint(APINotificationDeviceBody apiNotification) throws EntityValidationException {

        Device device;
        if(apiNotification.getDeviceType().equals(DeviceType.TEMPERA_DEVICE)){
            device = temperaDeviceService.findTemperaDeviceById(apiNotification.getDeviceId());
        } else if (apiNotification.getDeviceType().equals(DeviceType.ACCESS_POINT)){
            device = accessPointService.getAccessPointById(apiNotification.getDeviceId());
        } else {
            throw new IllegalArgumentException("Device type not supported.");
        }

        addNotificationToBell(device, apiNotification);
    }


    /**
     * Returns the number of notifications for the given user
     * @param currentUser the user for which the number of notifications should be returned
     * @return the number of notifications
     */
    public long getNumberOfNotifications(Userx currentUser) {
        Userx user = userxRepository.findFirstByUsername(currentUser.getUsername());
        return user.getNotifications().stream().count();
    }


    /**
     * Method that automatically checks if enabled Access Points have checked with server in the last 2 minutes 
     * (delay can happen since normal interval is 1 min)
     *
    */
    @Scheduled(cron = "0 * * * * *") // every minute (s min h dayOM month dayOW)
    public void checkConnectionOfAccessPoints() throws EntityValidationException{
        List<AccessPoint> listAP = accessPointService.getAllAccessPoints();
        for(AccessPoint ap : listAP){
            if (ap.getStatus() == DeviceStatus.ENABLED && ap.isConnected() && ap.getLastConnection().isBefore(LocalDateTime.now().minusMinutes(2))) {
                // disconnected
                accessPointService.setDisconnected(ap);
                informAdminsAboutDisconnectedAP(ap);
            }
        }
        
    }

    public void informAdminsAboutDisconnectedAP(AccessPoint accessPoint) throws EntityValidationException{
            APINotification notification = new APINotification();
            notification.setDeviceType(DeviceType.SERVER);
            notification.setNotificationType(NotificationType.ERROR);
            notification.setMessage("AP %d has lost connection at ".formatted(accessPoint.getId()) + LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS).toString().replace("T", " "));
            addNotificationToAllAdmins(notification);
    }


    /**
     * Method to check if sender and APINotificationAPBody matches (e.g. AP is not allowed to send messages about other AP)
     */
    public boolean doAccessPointAndAPINotificationMatch(AccessPoint sender, APINotificationAPBody apiNotification){

        if(sender == null || apiNotification == null || sender.getId() == null || apiNotification.getDeviceId() == null){
            return false;
        }

        return sender.getId().equals(apiNotification.getDeviceId());
    }








}
