package at.qe.skeleton.repositories;

import at.qe.skeleton.model.WorkGroup;
import at.qe.skeleton.model.Project;
import at.qe.skeleton.model.Userx;

import java.util.List;
import java.util.Optional;

/**
 * Repository for managing {@link Project} entities.
 */
public interface ProjectRepository extends AbstractRepository<Project, Long> {

    void delete(Project project);

    Optional<Project> findByName(String name);

    List<Project> findByProjectManager(Userx userx);

    List<Project> findByUsersContaining(Userx user);

    List<Project> findByUsers(List<Userx> users);

    List<Project> findByWorkGroupsContaining(WorkGroup workGroup);

    int count();
}
