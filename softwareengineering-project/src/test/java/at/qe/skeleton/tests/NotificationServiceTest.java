package at.qe.skeleton.tests;

import at.qe.skeleton.model.Token;
import at.qe.skeleton.model.Warning;
import at.qe.skeleton.model.WarningStatus;
import at.qe.skeleton.model.notifications.APINotification;
import at.qe.skeleton.model.notifications.WarningNotification;
import at.qe.skeleton.services.notifications.NotificationService;
import at.qe.skeleton.services.notifications.TokenService;
import at.qe.skeleton.services.climate.WarningService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

import static org.mockito.Mockito.*;

@SpringBootTest
public class NotificationServiceTest {
    @Autowired
    private NotificationService notificationService;

    @MockBean
    private TokenService tokenService;

    @MockBean
    private WarningService  warningService;

    @Test
    public void testConfirmNotification() {
        // Given
        Token token = new Token();
        token.setContent("testToken");

        Warning warning = new Warning();
        warning.setToken(token);
        warning.setId(101L);
        warning.setWarningStatus(WarningStatus.UNSEEN);

        WarningNotification warningNotification = new WarningNotification();
        warningNotification.setToken(token);

        when(tokenService.isTokenValid("testToken")).thenReturn(true);
        when(tokenService.getWarningByToken("testToken")).thenReturn(warning);
        doNothing().when(warningService).updateWarningStatus(101L, WarningStatus.CONFIRMED.ordinal());

        // When
        notificationService.confirmNotification(warningNotification);

        // Then
        verify(tokenService, times(1)).isTokenValid("testToken");
        verify(tokenService, times(1)).getWarningByToken("testToken");
        verify(warningService, times(1)).updateWarningStatus(101L, WarningStatus.CONFIRMED.ordinal());
    }

    @Test
    public void testIgnoreNotification() {
        // Given
        Token token2 = new Token();
        token2.setContent("testToken2");

        Warning warning2 = new Warning();
        warning2.setToken(token2);
        warning2.setId(102L);
        warning2.setWarningStatus(WarningStatus.UNSEEN);

        WarningNotification warningNotification = new WarningNotification();
        warningNotification.setToken(token2);

        when(tokenService.isTokenValid("testToken2")).thenReturn(true);
        when(tokenService.getWarningByToken("testToken2")).thenReturn(warning2);
        doNothing().when(warningService).updateWarningStatus(102L, WarningStatus.IGNORED.ordinal());

        // When
        notificationService.ignoreNotification(warningNotification);

        // Then
        verify(tokenService, times(1)).isTokenValid("testToken2");
        verify(tokenService, times(1)).getWarningByToken("testToken2");
        verify(warningService, times(1)).updateWarningStatus(102L, WarningStatus.IGNORED.ordinal());
    }

    @Test
    public void testIgnoreNotificationWithAPINotification() {
        // Given
        APINotification notification = new APINotification();

        // When
        notificationService.ignoreNotification(notification);

        // Then
        verify(tokenService, never()).isTokenValid(any());
        verify(tokenService, never()).getWarningByToken(any());
        verify(warningService, never()).updateWarningStatus(any(), any());
    }

    @Test
    public void testConfirmNotificationWithAPINotification() {
        // Given
        APINotification notification2 = new APINotification();

        // When
        notificationService.confirmNotification(notification2);

        // Then
        verify(tokenService, never()).isTokenValid(any());
        verify(tokenService, never()).getWarningByToken(any());
        verify(warningService, never()).updateWarningStatus(any(), any());
    }


}
