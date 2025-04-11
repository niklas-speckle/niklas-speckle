package at.qe.skeleton.tests;

import at.qe.skeleton.exceptions.EntityValidationException;
import at.qe.skeleton.model.notifications.*;
import at.qe.skeleton.services.notifications.NotificationService;
import at.qe.skeleton.services.notifications.NotificationButtonVisitor;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.web.WebAppConfiguration;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@SpringBootTest
@WebAppConfiguration
public class NotificationButtonVisitorTest {
    @Autowired
    NotificationButtonVisitor notificationButtonVisitor;

    @MockBean
    NotificationService notificationService;

    @Test
    public void testNotificationButtonVisitorConfirm() throws EntityValidationException {
        NotificationConfirmButton notificationConfirmButton = new NotificationConfirmButton();
        Notification notification = new WarningNotification();

        // Given
        doNothing().when(notificationService).confirmNotification(any());
        notificationButtonVisitor.visit(notificationConfirmButton, notification);

        // Then
        verify(notificationService, times(1)).confirmNotification(notification);
    }

    @Test
    public void testNotificationButtonVisitorIgnore() throws EntityValidationException {
        NotificationIgnoreButton notificationIgnoreButton = new NotificationIgnoreButton();
        Notification notification2 = new WarningNotification();

        // Given
        doNothing().when(notificationService).ignoreNotification(any());
        notificationButtonVisitor.visit(notificationIgnoreButton, notification2);

        // Then
        verify(notificationService, times(1)).ignoreNotification(notification2);
    }

    @Test
    public void testNotificationButtonVisitorDelete() throws EntityValidationException {
        NotificationDeleteButton notificationDeleteButton = new NotificationDeleteButton();
        Notification notification2 = new APINotification();

        // Given
        doNothing().when(notificationService).deleteNotification(any());
        notificationButtonVisitor.visit(notificationDeleteButton, notification2);

        // Then
        verify(notificationService, times(1)).deleteNotification(notification2);
    }
}
