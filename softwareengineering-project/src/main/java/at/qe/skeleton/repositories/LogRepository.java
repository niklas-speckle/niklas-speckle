package at.qe.skeleton.repositories;


import at.qe.skeleton.model.Action;
import at.qe.skeleton.model.AuditLog;

import java.util.Collection;

public interface LogRepository extends AbstractRepository<AuditLog, Long> {

    Collection<AuditLog> findAllByAction(Action action);

    Integer count();

}
