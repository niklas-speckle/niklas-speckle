package at.qe.skeleton.repositories;


import at.qe.skeleton.model.Warning;

/**
 * Repository for managing {@link Warning} entities.
 */
public interface WarningRepository extends AbstractRepository<Warning, Long> {


    Warning findWarningById(Long warningId);

    Warning findByTokenContent(String tokenContent);
}
