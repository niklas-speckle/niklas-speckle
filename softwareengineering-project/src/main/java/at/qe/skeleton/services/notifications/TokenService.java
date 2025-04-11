package at.qe.skeleton.services.notifications;

import at.qe.skeleton.model.Token;
import at.qe.skeleton.model.Warning;
import at.qe.skeleton.model.WarningStatus;
import at.qe.skeleton.repositories.TokenRepository;
import at.qe.skeleton.repositories.WarningRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class TokenService {
    @Autowired
    private WarningRepository warningRepository;
    @Autowired
    private TokenRepository tokenRepository;

    /**
     * generates a token for a warning to be mailed to the respective user within a link.
     * @param warning the Warning for which the token is generated
     * @return the generated token
     */
    public Token generateToken(Warning warning) {
        String tokenValue = UUID.randomUUID().toString();
        Token token = new Token(tokenValue, warning);
        warning.setToken(token);
        warningRepository.save(warning);
        return token;
    }

    /**
     * searches for a Warning by the token value.
     * @param tokenValue the token value
     * @return the searched Warning
     */
    public Warning getWarningByToken(String tokenValue) {
        return warningRepository.findByTokenContent(tokenValue);
    }

    /**
     * checks if a token is valid.
     * @param tokenValue the token value to be checked
     * @return true if the token is valid, false otherwise
     */
    public boolean isTokenValid(String tokenValue){
        Token token = tokenRepository.findTokenByContent(tokenValue);
        return token != null && !token.isConsumed();
    }

    /**
     * disables the given token and sets its Warning to null.
     * @param token the token to be disabled
     * @return the disabled token
     */
    public Token disableToken(Token token) {
        token.setWarning(null);
        token.setConsumed(true);
        tokenRepository.save(token);
        return token;
    }

    /**
     * takes a given Warning and checks, if a token is needed or a token should be disabled.
     * @param warning the Warning which should be checked
     * @return the token of the warning
     */
    public Token checkToken(Warning warning) {
        Token token = warning.getToken();

        if (token == null) {
            return generateToken(warning);
        }

        if (warning.getWarningStatus() == WarningStatus.CONFIRMED || warning.getWarningStatus() == WarningStatus.IGNORED) {
            return disableToken(token);
        }

        return token;
    }

    /**
     * deletes all disabled tokens every sunday at midnight.
     */
    @Scheduled(cron = "0 0 0 * * SUN") //every sunday at midnight
    public void deleteOldDisabledTokens() {
        tokenRepository.findAllByConsumedTrue().forEach(tokenRepository::delete);
    }
}
