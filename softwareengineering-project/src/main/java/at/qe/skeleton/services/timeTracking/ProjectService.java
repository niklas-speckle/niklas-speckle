package at.qe.skeleton.services.timeTracking;

import at.qe.skeleton.exceptions.IdNotFoundException;
import at.qe.skeleton.model.WorkGroup;
import at.qe.skeleton.model.Project;
import at.qe.skeleton.model.Userx;
import at.qe.skeleton.repositories.ProjectRepository;
import org.hibernate.Hibernate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Service for managing projects, providing CRUD operations and project-specific business logic.
 */
@Service
@Scope("application")
public class ProjectService {

    @Autowired
    private ProjectRepository projectRepository;

    /**
     * Saves a project. Only accessible by users with MANAGER, ADMINISTRATOR, or GROUP_LEADER roles.
     *
     * @param project the project to save
     * @return the saved project entity
     * @throws DataIntegrityViolationException if a project with the same name already exists
     */
    @Transactional
    @PreAuthorize("hasAnyAuthority('MANAGER', 'ADMINISTRATOR', 'GROUP_LEADER')")
    public Project save(Project project) throws DataIntegrityViolationException {
        try {
            removeUsersIfNecessary(project);
            return projectRepository.save(project);
        } catch (DataIntegrityViolationException e) {
            throw new DataIntegrityViolationException("Could not save project because a project with the name '" + project.getName() + "' already exists.");
        }
    }

    private void removeUsersIfNecessary(Project project) {
        if (project.getUsers() != null) {
        // Iterate through the users
        Iterator<Userx> userIterator = project.getUsers().iterator();
        while (userIterator.hasNext()) {
            Userx user = userIterator.next();
            boolean userInWorkGroup = false;
            // Check if the user is part of any of the projects work groups
            for (WorkGroup workGroup : project.getWorkGroups()) {
                if (workGroup.getUsers().contains(user)) {
                    userInWorkGroup = true;
                    break;
                }
            }
            // If the user is not part of any work group, remove them from the project
            if (!userInWorkGroup) {
                userIterator.remove();
            }
        }
        }
    }

    /**
     * Deletes a project by setting its disabled status to true. Only accessible by users with MANAGER or ADMINISTRATOR roles.
     *
     * @param id the ID of the project to delete
     * @throws IdNotFoundException if no project with the given ID exists
     */
    @Transactional
    @PreAuthorize("hasAnyAuthority('MANAGER', 'ADMINISTRATOR')")
    public void delete(Long id) throws IdNotFoundException {
        Optional<Project> project = projectRepository.findById(id);
        if (project.isPresent()) {
            project.get().setDisabled(true);
            projectRepository.save(project.get());
        } else {
            throw new IdNotFoundException("Project with id " + id + " does not exist.");
        }
    }

    /**
     * Retrieves a project by its ID, only if it is not disabled.
     *
     * @param id the ID of the project
     * @return an optional containing the project if found and not disabled, otherwise empty
     */
    public Optional<Project> getProjectById(Long id) {
        return projectRepository.findById(id).filter(value -> !value.isDisabled());
    }

    /**
     * Retrieves a project by its name, only if it is not disabled.
     *
     * @param name the name of the project
     * @return an optional containing the project if found and not disabled, otherwise empty
     */
    public Optional<Project> getProjectByName(String name) {
        return projectRepository.findByName(name).stream().filter(value -> !value.isDisabled()).findFirst();
    }

    /**
     * Retrieves a list of enabled projects managed by a specific user. Only accessible by users with MANAGER or ADMINISTRATOR roles.
     *
     * @param user the manager of the projects
     * @return a list of enabled projects managed by the user
     */
    @PreAuthorize("hasAnyAuthority('MANAGER', 'ADMINISTRATOR')")
    public List<Project> getProjectByManager(Userx user) {
        return projectRepository.findByProjectManager(user).stream().filter(value -> !value.isDisabled()).toList();
    }

    /**
     * Retrieves a list of enabled projects associated with specific work groups.
     *
     * @param workGroups the work groups associated with the projects
     * @return a list of enabled projects associated with the work groups
     */
    public List<Project> getProjectByWorkGroups(List<WorkGroup> workGroups) {
        Set<Project> projectSet = new HashSet<>();
        for (WorkGroup workGroup : workGroups) {
            projectSet.addAll(projectRepository.findByWorkGroupsContaining(workGroup).stream().filter(value -> !value.isDisabled()).toList());
        }
        return new ArrayList<>(projectSet);
    }

    /**
     * Retrieves a list of all projects, initializing their associations.
     *
     * @return a list of all projects
     */
    @Transactional
    public List<Project> getAll() {
        List<Project> projects = projectRepository.findAll();
        projects.forEach(this::initializeAssociations);
        return projects;
    }

    /**
     * Initializes the associations of a project (users and work groups).
     *
     * @param project the project to initialize
     */
    private void initializeAssociations(Project project) {
        Hibernate.initialize(project.getUsers());
        Hibernate.initialize(project.getWorkGroups());
    }

    /**
     * Retrieves a list of all enabled projects.
     *
     * @return a list of all enabled projects
     */
    public List<Project> getAllEnabled() {
        return projectRepository.findAll().stream().filter(value -> !value.isDisabled()).collect(Collectors.toCollection(ArrayList::new));
    }

    /**
     * Sets the users for a project. Only accessible by users with GROUP_LEADER or ADMINISTRATOR roles.
     *
     * @param project the project to update
     * @param users   the list of users to set for the project
     */
    @Transactional
    @PreAuthorize("hasAnyAuthority('GROUP_LEADER', 'ADMINISTRATOR')")
    public void setUsers(Project project, List<Userx> users) {
        project.setUsers(users);
        projectRepository.save(project);
    }

    /**
     * Creates a new project instance with empty user and work group lists.
     * Only accessible by users with MANAGER or ADMINISTRATOR roles.
     *
     * @return the new project instance
     */
    @PreAuthorize("hasAnyAuthority('MANAGER', 'ADMINISTRATOR')")
    public Project createNewProject() {
        Project newProject = new Project();
        newProject.setWorkGroups(new ArrayList<>());
        newProject.setUsers(new ArrayList<>());
        return newProject;
    }

    /**
     * Retrieves a list of enabled projects a user is associated with.
     *
     * @param currentUser the user associated with the projects
     * @return a list of enabled projects the user is associated with
     */
    public List<Project> getProjectsByUser(Userx currentUser) {
        return projectRepository.findByUsersContaining(currentUser).stream().filter(value -> !value.isDisabled()).toList();
    }
}
