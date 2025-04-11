package at.qe.skeleton.repositories;


import at.qe.skeleton.model.Token;
import at.qe.skeleton.model.Warning;

import java.util.List;

/**
 * Repository for managing {@link Warning} entities.
 */
public interface TokenRepository extends AbstractRepository<Token, String> {


    Token findTokenByContent(String tokenContent);

    List<Token> findAllByConsumedTrue();
}
