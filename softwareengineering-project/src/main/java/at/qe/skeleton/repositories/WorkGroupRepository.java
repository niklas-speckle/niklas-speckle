package at.qe.skeleton.repositories;

import at.qe.skeleton.model.Project;
import at.qe.skeleton.model.WorkGroup;
import at.qe.skeleton.model.Userx;

import java.util.List;
import java.util.Optional;

/**
 * Repository for managing {@link WorkGroup} entities.
 */
public interface WorkGroupRepository extends AbstractRepository<WorkGroup, Long> {

    void delete(WorkGroup workGroup);

    Optional<WorkGroup> findByName(String name);

    List<WorkGroup> findByGroupLeader(Userx userx);

    List<WorkGroup> findAllByUsersContaining(Userx users);

    List<WorkGroup> findAllByProjectsContaining(Project project);

}
