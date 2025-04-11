package at.qe.skeleton.tests;

import at.qe.skeleton.model.Token;
import at.qe.skeleton.model.Warning;
import at.qe.skeleton.model.WarningStatus;
import at.qe.skeleton.repositories.TokenRepository;
import at.qe.skeleton.repositories.WarningRepository;
import at.qe.skeleton.services.notifications.TokenService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@SpringBootTest
public class TokenServiceTest {
    @Mock
    private TokenRepository tokenRepository;
    @Mock
    private WarningRepository warningRepository;

    @InjectMocks
    private TokenService tokenService;

    @Test
    public void testGenerateToken() {
        //Given
        Warning warning = new Warning();
        warning.setTimestamp(LocalDateTime.now());

        //When
        Token token = tokenService.generateToken(warning);

        //Then
        Assertions.assertNotNull(token);
        Assertions.assertNotNull(token.getContent());
        Assertions.assertEquals(warning, token.getWarning());
        Assertions.assertFalse(token.isConsumed());
    }

    @Test
    public void consumedTokenShouldNotBeValid() {
        //Given
        Token token = new Token();
        token.setConsumed(true);
        when(tokenRepository.findTokenByContent(token.getContent())).thenReturn(token);

        //When
        boolean isValid = tokenService.isTokenValid(token.getContent());

        //Then
        Assertions.assertFalse(isValid);
    }

    @Test
    public void validTokenShouldBeValid() {
        //Given
        Token token = new Token();
        when(tokenRepository.findTokenByContent(token.getContent())).thenReturn(token);

        //When
        boolean isValid = tokenService.isTokenValid(token.getContent());

        //Then
        Assertions.assertTrue(isValid);
    }

    @Test
    public void testDisableToken() {
        //Given
        Token token = new Token();
        token.setConsumed(false);
        token.setWarning(new Warning());

        //When
        Token disabledToken = tokenService.disableToken(token);

        //Then
        Assertions.assertTrue(disabledToken.isConsumed());
        Assertions.assertNull(disabledToken.getWarning());
    }

    @Test
    @DisplayName("Warnings with no token should get a new token")
    public void noTokenCheckToken() {
        //Given
        Warning warning = new Warning();
        warning.setTimestamp(LocalDateTime.now());
        warning.setWarningStatus(WarningStatus.DRAFT);

        //When
        Token checkedToken = tokenService.checkToken(warning);

        //Then
        Assertions.assertEquals(warning.getToken(), checkedToken);
        Assertions.assertNotNull(warning.getToken());
    }

    @Test
    @DisplayName("Warnings with status CONFIRMED should get their token disabled")
    public void confirmedTokenCheckToken() {
        //Given
        Token token = new Token();
        token.setConsumed(false);

        Warning warning = new Warning();
        warning.setWarningStatus(WarningStatus.CONFIRMED);
        warning.setToken(token);
        token.setWarning(warning);

        //When
        Token checkedToken = tokenService.checkToken(warning);

        //Then
        Assertions.assertEquals(warning.getToken(), checkedToken);
        Assertions.assertTrue(token.isConsumed());
        Assertions.assertNull(token.getWarning());
    }

    @Test
    @DisplayName("Warnings with status IGNORED should get their token disabled")
    public void ignoredTokenCheckToken() {
        //Given
        Token token = new Token();
        token.setConsumed(false);

        Warning warning = new Warning();
        warning.setWarningStatus(WarningStatus.IGNORED);
        warning.setToken(token);
        token.setWarning(warning);

        //When
        Token checkedToken = tokenService.checkToken(warning);

        //Then
        Assertions.assertEquals(warning.getToken(), checkedToken);
        Assertions.assertTrue(token.isConsumed());
        Assertions.assertNull(token.getWarning());
    }

    @Test
    public void deleteOldDisabledTokens() {
        //Given
        List<Token> tokens = List.of(new Token(), new Token(), new Token());
        tokens.forEach(token -> token.setConsumed(true));

        when(tokenRepository.findAllByConsumedTrue()).thenReturn(tokens);
        doNothing().when(tokenRepository).delete(any(Token.class));

        //When
        tokenService.deleteOldDisabledTokens();

        //Then
        verify(tokenRepository, times(1)).findAllByConsumedTrue();
        verify(tokenRepository, times(tokens.size())).delete(any(Token.class));
    }
}
