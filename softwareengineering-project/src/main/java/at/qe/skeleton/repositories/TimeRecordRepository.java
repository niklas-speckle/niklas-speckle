package at.qe.skeleton.repositories;

import at.qe.skeleton.model.Project;
import at.qe.skeleton.model.TimeRecord;
import at.qe.skeleton.model.Userx;
import at.qe.skeleton.model.WorkGroup;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Repository for managing {@link TimeRecord} entities.
 */
public interface TimeRecordRepository extends AbstractRepository<TimeRecord, Long> {

    TimeRecord findFirstByUserAndEndTimeIsNull(Userx user);
    List<TimeRecord> findAllByUser(Userx user);

    List<TimeRecord> findAllByUserAndStartTimeAfter(Userx user, LocalDateTime startTime);

    List<TimeRecord> findAllByProject(Project project);

    List<TimeRecord> findAllByWorkGroup(WorkGroup workGroup);

    List<TimeRecord> findAllByEndTimeIsNull();
}
