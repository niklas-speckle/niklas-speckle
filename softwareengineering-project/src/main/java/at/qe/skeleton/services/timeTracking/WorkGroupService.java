package at.qe.skeleton.services.timeTracking;

import at.qe.skeleton.model.Project;
import at.qe.skeleton.model.WorkGroup;
import at.qe.skeleton.model.Userx;
import at.qe.skeleton.repositories.WorkGroupRepository;
import org.hibernate.Hibernate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

/**
 * Service for managing work groups, providing CRUD operations and work group-specific business logic.
 */
@Service
@Scope("application")
public class WorkGroupService {

    @Autowired
    private WorkGroupRepository workGroupRepository;

    /**
     * Saves a work group. Only accessible by users with MANAGER, GROUP_LEADER, or ADMINISTRATOR roles.
     *
     * @param workGroup the work group to save
     * @return the saved work group entity
     * @throws DataIntegrityViolationException if a work group with the same name already exists
     */
    @Transactional
    @PreAuthorize("hasAnyAuthority('MANAGER', 'GROUP_LEADER', 'ADMINISTRATOR')")
    public WorkGroup save(WorkGroup workGroup) throws DataIntegrityViolationException {
        try {
            return workGroupRepository.save(workGroup);
        } catch (DataIntegrityViolationException e) {
            throw new DataIntegrityViolationException("Could not save work group because a work group with the name '" + workGroup.getName() + "' already exists.");
        }
    }

    /**
     * Deletes a work group by setting its disabled status to true. Only accessible by users with MANAGER or ADMINISTRATOR roles.
     *
     * @param id the ID of the work group to delete
     */
    @Transactional
    @PreAuthorize("hasAnyAuthority('MANAGER', 'ADMINISTRATOR')")
    public void delete(Long id) {
        Optional<WorkGroup> group = workGroupRepository.findById(id);
        if (group.isPresent()) {
            group.get().setDisabled(true);
            workGroupRepository.save(group.get());
        }
    }

    /**
     * Retrieves a work group by its ID, only if it is not disabled.
     *
     * @param id the ID of the work group
     * @return an optional containing the work group if found and not disabled, otherwise empty
     */
    public Optional<WorkGroup> getGroupById(Long id) {
        return workGroupRepository.findById(id).filter(value -> !value.isDisabled());
    }

    /**
     * Retrieves a work group by its name, only if it is not disabled.
     *
     * @param name the name of the work group
     * @return an optional containing the work group if found and not disabled, otherwise empty
     */
    public Optional<WorkGroup> getGroupByName(String name) {
        return workGroupRepository.findByName(name).stream().filter(value -> !value.isDisabled()).findFirst();
    }

    /**
     * Retrieves a list of enabled work groups led by a specific user.
     *
     * @param userx the group leader
     * @return a list of enabled work groups led by the user
     */
    public List<WorkGroup> getGroupsByGroupLeader(Userx userx) {
        List<WorkGroup> workGroups = workGroupRepository.findByGroupLeader(userx).stream().filter(value -> !value.isDisabled()).toList();
        workGroups.forEach(this::initializeAssociations);
        return workGroups;
    }

    /**
     * Retrieves a list of all work groups, initializing their associations.
     *
     * @return a list of all work groups
     */
    @Transactional
    public List<WorkGroup> getAll() {
        List<WorkGroup> workGroups = workGroupRepository.findAll();
        workGroups.forEach(this::initializeAssociations);
        return workGroups;
    }

    /**
     * Initializes the associations of a work group (users and projects).
     *
     * @param workGroup the work group to initialize
     */
    private void initializeAssociations(WorkGroup workGroup) {
        Hibernate.initialize(workGroup.getUsers());
        Hibernate.initialize(workGroup.getProjects());
    }

    /**
     * Retrieves a list of all enabled work groups.
     *
     * @return a list of all enabled work groups
     */
    public List<WorkGroup> getAllEnabled() {
        return workGroupRepository.findAll().stream().filter(value -> !value.isDisabled()).toList();
    }


    /**
     * Creates a new work group instance. Only accessible by users with MANAGER or ADMINISTRATOR roles.
     *
     * @return the new work group instance
     */
    @PreAuthorize("hasAnyAuthority('MANAGER', 'ADMINISTRATOR')")
    public WorkGroup createNewWorkGroup() {
        return new WorkGroup();
    }

    /**
     * Retrieves a list of enabled work groups a user is associated with.
     *
     * @param currentUser the user associated with the work groups
     * @return a list of enabled work groups the user is associated with
     */
    public List<WorkGroup> getWorkGroupsByUser(Userx currentUser) {
        return workGroupRepository.findAllByUsersContaining(currentUser).stream().filter(value -> !value.isDisabled()).toList();
    }

    /**
     * Retrieves a list of enabled work groups associated with specific projects.
     *
     * @param projects the projects associated with the work groups
     * @return a list of enabled work groups associated with the projects
     */
    @Transactional
    public List<WorkGroup> getWorkGroupsByProjects(List<Project> projects) {
        Set<WorkGroup> workGroupSet = new HashSet<>();
        for (Project project : projects) {
            workGroupSet.addAll(workGroupRepository.findAllByProjectsContaining(project).stream().filter(value -> !value.isDisabled()).toList());
        }
        return new ArrayList<>(workGroupSet);
    }

    /**
     * Finds selectable work groups for a project and user.
     *
     * @param project the project
     * @param user the user
     * @return a list of selectable work groups for the project and user
     */
    @Transactional
    public List<WorkGroup> findSelectableWorkGroupsForProjectAndUser(Project project, Userx user) {
        List<WorkGroup> workGroups = getWorkGroupsByProjects(List.of(project));
        workGroups.removeIf(group -> !group.getUsers().contains(user));
        return workGroups;
    }
}
