package at.qe.skeleton.tests;


import at.qe.skeleton.exceptions.IdNotFoundException;
import at.qe.skeleton.model.Project;
import at.qe.skeleton.model.Userx;
import at.qe.skeleton.model.UserxRole;
import at.qe.skeleton.model.WorkGroup;
import at.qe.skeleton.repositories.ProjectRepository;
import at.qe.skeleton.repositories.UserxRepository;
import at.qe.skeleton.repositories.WorkGroupRepository;
import at.qe.skeleton.services.timeTracking.ProjectService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;



@SpringBootTest
@WebAppConfiguration
public class ProjectServiceTest {


    @Autowired
    private UserxRepository userxRepository;

    @Autowired
    private WorkGroupRepository workGroupRepository;

    @Autowired
    ProjectRepository projectRepository;

    @Autowired
    private ProjectService projectService;



    @Test
    @WithMockUser(username = "manager", authorities = {"MANAGER"})
    public void testGetAllProjects() {
        int totalProjects = projectRepository.count();
        List<Project> projects = projectService.getAll();
        assertEquals(totalProjects, projects.size());
    }

    @Test
    @Transactional
    @WithMockUser(username = "admin", authorities = {"ADMINISTRATOR"})
    public void testGetProjectByManager() {
        Userx testUser = new Userx();
        testUser.setRoles(List.of(UserxRole.MANAGER, UserxRole.EMPLOYEE));
        testUser.setUsername("testUser");
        testUser.setCreatedBy(userxRepository.findFirstByUsername("admin").getUsername());
        testUser.setCreateDate(LocalDateTime.now());
        testUser = userxRepository.save(testUser);

        Project testProject = new Project();
        testProject.setProjectManager(testUser);
        testProject.setName("testProject");
        testProject = projectRepository.save(testProject);

        List<Project> result = projectService.getProjectByManager(testUser);
        assertEquals(1, result.size());
        assertTrue(result.contains(testProject), "Result should contain testProject1,");
    }

    @Test
    @Transactional
    @WithMockUser(username = "manager", authorities = {"MANAGER"})
    public void testGetProjectByWorkGroups() {

        Project testProject1 = new Project();
        testProject1.setName("testProject1");
        testProject1 = projectRepository.save(testProject1);

        Project testProject2 = new Project();
        testProject2.setName("testProject2");
        testProject2 = projectRepository.save(testProject1);

        WorkGroup testWorkGroup1 = new WorkGroup();
        testWorkGroup1.setName("testGroup1");
        testWorkGroup1.setProjects(Set.of(testProject1));
        testWorkGroup1 = workGroupRepository.save(testWorkGroup1);

        WorkGroup testWorkGroup2 = new WorkGroup();
        testWorkGroup2.setName("testGroup2");
        testWorkGroup2.setProjects(Set.of(testProject2));
        testWorkGroup2 = workGroupRepository.save(testWorkGroup2);

        List<Project> result = projectService.getProjectByWorkGroups(List.of(testWorkGroup1, testWorkGroup2));
        assertTrue(result.contains(testProject1), "Should contain testProject1.");
        assertTrue(result.contains(testProject2), "Should contain testProject2.");
    }

    @Test
    @Transactional
    @WithMockUser(username = "manager", authorities = {"MANAGER"})
    public void testGetAllEnabledProjects() throws IdNotFoundException {
        List<Project> projects = projectService.getAllEnabled();
        assertEquals(3, projects.size());
        assertTrue(projects.stream().map(Project::getName).allMatch(List.of("Project 1", "Project 2", "Project 3")::contains));

        projectService.delete(1L);

        projects = projectService.getAllEnabled();
        assertEquals(2, projects.size());
        assertTrue(projects.stream().map(Project::getName).allMatch(List.of("Project 2", "Project 3")::contains));
    }


    @Test
    @Transactional
    @WithMockUser(username = "manager", authorities = {"MANAGER"})
    public void testCreateProject() {

        int numberOfProjectsBefore = projectService.getAll().size();

        Project testProject = projectService.createNewProject();
        testProject.setName("Test Project");
        testProject.setDescription("This is a test project");

        projectService.save(testProject);

        int numberOfProjectsAfter = projectService.getAll().size();

        Assertions.assertEquals(numberOfProjectsBefore + 1, numberOfProjectsAfter);

        // Assert content of saved project
        Project savedProject = projectService.getProjectByName("Test Project").orElseThrow();
        Assertions.assertEquals("Test Project", savedProject.getName());
        Assertions.assertEquals("This is a test project", savedProject.getDescription());
    }

    @Test
    @Transactional
    @WithMockUser(username = "unauthorized", authorities = {"EMPLOYEE"})
    public void testCreateProjectUnauthorized() {

        int numberOfProjectsBefore = projectService.getAll().size();

        assertThrows(org.springframework.security.access.AccessDeniedException.class, () -> {
            Project testProject = projectService.createNewProject();
        });

        Project testProject = new Project();
        testProject.setName("Test Project");
        testProject.setDescription("This is a test project");

        assertThrows(org.springframework.security.access.AccessDeniedException.class, () -> {
            projectService.save(testProject);
        });

        int numberOfProjectsAfter = projectService.getAll().size();
        assertEquals(numberOfProjectsBefore, numberOfProjectsAfter);
    }

    @Test
    @Transactional
    @WithMockUser(username = "admin", authorities = {"ADMINISTRATOR"})
    public void testDeleteProject() throws IdNotFoundException {

        int numberOfProjectsBefore = projectService.getAll().size();

        List<Project> projects = projectService.getAll();

        Optional<Project> projectToDelete = projectService.getProjectByName("Project 1");
        assertTrue(projectToDelete.isPresent());

        Userx oldManager = projectToDelete.get().getProjectManager();
        assertTrue(oldManager.getRoles().contains(UserxRole.MANAGER));

        projectService.delete(projectToDelete.get().getId());

        int numberOfProjectsAfter = projectService.getAllEnabled().size();

        assertEquals(numberOfProjectsBefore - 1, numberOfProjectsAfter);
        assertFalse(projectService.getProjectByName("Project 1").isPresent());
    }

    @Test
    @WithMockUser(username = "unauthorized", authorities = {"EMPLOYEE"})
    public void testDeleteProjectUnauthorized() {
        int numberOfProjectsBefore = projectService.getAll().size();

        Project projectToDelete = projectService.getProjectById(1L).orElseThrow();

        assertThrows(org.springframework.security.access.AccessDeniedException.class, () -> {
            projectService.delete(projectToDelete.getId());
        });

        int numberOfProjectsAfter = projectService.getAll().size();

        assertEquals(numberOfProjectsBefore, numberOfProjectsAfter);
    }

    @Test
    @Transactional
    @WithMockUser(username = "manager", authorities = {"MANAGER"})
    public void testEditProject() {
        Project projectToEdit = projectService.getProjectByName("Project 1").orElseThrow();
        projectToEdit.setName("Edited Project 1");
        projectToEdit.setDescription("Edited Description for Project 1");

        projectService.save(projectToEdit);

        Project editedProject = projectService.getProjectByName("Edited Project 1").orElseThrow();
        assertEquals("Edited Project 1", editedProject.getName());
        assertEquals("Edited Description for Project 1", editedProject.getDescription());
    }


    @Test
    @Transactional
    @WithMockUser(username = "groupleader", authorities = {"GROUP_LEADER"})
    public void testSetUsers() {
        Project project = projectService.getProjectByName("Project 1").orElseThrow();

        List<Userx> newUsers = List.of(userxRepository.findFirstByUsername("admin"), userxRepository.findFirstByUsername("manager"));

        projectService.setUsers(project, new ArrayList<>(newUsers));

        project = projectService.getProjectByName("Project 1").orElseThrow();
        assertEquals(2, project.getUsers().size());
        assertTrue(project.getUsers().stream().map(Userx::getUsername).allMatch(List.of("admin", "manager")::contains));
    }



}
