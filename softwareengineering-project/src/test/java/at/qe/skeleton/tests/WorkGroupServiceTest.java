package at.qe.skeleton.tests;

import at.qe.skeleton.exceptions.EntityValidationException;
import at.qe.skeleton.model.Project;
import at.qe.skeleton.model.Userx;
import at.qe.skeleton.model.UserxRole;
import at.qe.skeleton.model.WorkGroup;
import at.qe.skeleton.repositories.UserxRepository;
import at.qe.skeleton.repositories.WorkGroupRepository;
import at.qe.skeleton.services.UserService;
import at.qe.skeleton.services.timeTracking.ProjectService;
import at.qe.skeleton.services.timeTracking.WorkGroupService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@WebAppConfiguration
public class WorkGroupServiceTest {

    @Autowired
    WorkGroupService workGroupService;

    @Autowired
    UserxRepository userxRepository;

    @Autowired
    WorkGroupRepository workGroupRepository;

    @Autowired
    ProjectService projectService;

    @Autowired
    private UserService userService;


    @Test
    @Transactional
    @WithMockUser(username = "admin", authorities = {"ADMINISTRATOR"})
    public void integrationTestSave() {

        WorkGroup workGroupToBeSaved = WorkGroup.builder().name("TestGroup").build();

        workGroupToBeSaved = workGroupService.save(workGroupToBeSaved);

        Assertions.assertEquals(workGroupToBeSaved, workGroupService.getGroupById(workGroupToBeSaved.getId()).get());

        // Save same entity a second time sould not throw any error
        Assertions.assertEquals(workGroupToBeSaved, workGroupService.save(workGroupToBeSaved));
    }

    @Test
    @Transactional
    @WithMockUser(username = "unauthorized", authorities = {"EMPLOYEE"})
    public void testSaveUnauthorized() {
        WorkGroup workGroupToBeSaved = WorkGroup.builder().name("TestGroup").build();

        assertThrows(AccessDeniedException.class, () -> {
            workGroupService.save(workGroupToBeSaved);
        });
    }


    @Test
    @WithMockUser(username = "manager", authorities = {"MANAGER"})
    public void testGetAllWorkGroups() {
        List<WorkGroup> workGroupsFromDB = workGroupRepository.findAll();
        int totalNumberOfWorkgroups = workGroupsFromDB.size();

        List<WorkGroup> workGroupsViaService = workGroupService.getAllEnabled();
        assertEquals(totalNumberOfWorkgroups, workGroupsViaService.size());
        assertTrue(workGroupsViaService.containsAll(workGroupsFromDB));
    }

    @Test
    @WithMockUser(username = "manager", authorities = {"MANAGER"})
    public void testGetGroupById() {
        WorkGroup workGroup = workGroupService.getGroupById(1L).orElseThrow();
        assertEquals("Group 1", workGroup.getName());
    }

    @Test
    @WithMockUser(username = "manager", authorities = {"MANAGER"})
    public void testGetGroupsByName() {
        WorkGroup workGroup = workGroupService.getGroupByName("Group 1").orElseThrow();
        assertEquals("Group 1", workGroup.getName());
    }


    @Test
    @Transactional
    @WithMockUser(username = "admin", authorities = {"ADMINISTRATOR"})
    public void testGetGroupByGroupLeader() {
        Userx testUser = new Userx();
        testUser.setRoles(List.of(UserxRole.GROUP_LEADER, UserxRole.EMPLOYEE));
        testUser.setUsername("testUser");
        testUser.setCreatedBy(userxRepository.findFirstByUsername("admin").getUsername());
        testUser.setCreateDate(LocalDateTime.now());
        testUser = userxRepository.save(testUser);

        WorkGroup testGroup = new WorkGroup();
        testGroup.setGroupLeader(testUser);
        testGroup.setName("testGroup");
        testGroup = workGroupRepository.save(testGroup);

        List<WorkGroup> result = workGroupService.getGroupsByGroupLeader(testUser);
        assertEquals(1, result.size());
        assertTrue(result.contains(testGroup), "Result should contain testProject1,");
    }

    @Test
    @Transactional
    @WithMockUser(username = "admin", authorities = {"ADMINISTRATOR"})
    public void integrationsTestGetWorkGroupByUser() throws EntityValidationException {

        Userx user1 = userService.saveUser(Userx.builder().username("user100").password("p").workGroups(new HashSet<>()).build());
        Userx user2 = userService.saveUser(Userx.builder().username("user200").password("p").workGroups(new HashSet<>()).build());
        Userx user3 = userService.saveUser(Userx.builder().username("user300").password("p").workGroups(new HashSet<>()).build());
        Userx user4 = userService.saveUser(Userx.builder().username("user400").password("p").workGroups(new HashSet<>()).build());

        // Create workgroups with empty user sets first
        WorkGroup workGroup1 = workGroupService.save(WorkGroup.builder().name("TestGroup1").users(Set.of(user1, user2)).disabled(false).build());
        WorkGroup workGroup2 = workGroupService.save(WorkGroup.builder().name("TestGroup2").users(Set.of(user3, user4)).disabled(false).build());
        WorkGroup workGroup3 = workGroupService.save(WorkGroup.builder().name("TestGroup3").users(Set.of(user1, user3)).disabled(false).build());
        WorkGroup workGroup4 = workGroupService.save(WorkGroup.builder().name("TestGroup4").users(Set.of(user2, user4)).disabled(false).build());


        List<WorkGroup> result1 = workGroupService.getWorkGroupsByUser(user1);
        List<WorkGroup> result2 = workGroupService.getWorkGroupsByUser(user2);
        List<WorkGroup> result3 = workGroupService.getWorkGroupsByUser(user3);
        List<WorkGroup> result4 = workGroupService.getWorkGroupsByUser(user4);

        assertTrue(result1.containsAll(List.of(workGroup1, workGroup3)));
        assertTrue(result2.containsAll(List.of(workGroup1, workGroup4)));
        assertTrue(result3.containsAll(List.of(workGroup2, workGroup3)));
        assertTrue(result4.containsAll(List.of(workGroup2, workGroup4)));
    }


    @Test
    @Transactional
    @WithMockUser(username = "admin", authorities = {"ADMINISTRATOR"})
    public void testGetWorkGroupsByProjects(){
        Project project1 = projectService.save(Project.builder().name("TestProject1").disabled(false).build());
        Project project2 = projectService.save(Project.builder().name("TestProject2").disabled(false).build());
        Project project3 = projectService.save(Project.builder().name("TestProject3").disabled(false).build());
        Project project4 = projectService.save(Project.builder().name("TestProject4").disabled(false).build());

        WorkGroup workGroup1 = workGroupService.save(WorkGroup.builder().name("TestGroup1").projects(new HashSet<>(Set.of(project1, project2))).disabled(false).build());
        WorkGroup workGroup2 = workGroupService.save(WorkGroup.builder().name("TestGroup2").projects(new HashSet<>(Set.of(project3, project4))).disabled(false).build());
        WorkGroup workGroup3 = workGroupService.save(WorkGroup.builder().name("TestGroup3").projects(new HashSet<>(Set.of(project1, project3))).disabled(false).build());
        WorkGroup workGroup4 = workGroupService.save(WorkGroup.builder().name("TestGroup4").projects(new HashSet<>(Set.of(project2, project4))).disabled(false).build());

        List<WorkGroup> result1 = workGroupService.getWorkGroupsByProjects(List.of(project1));
        List<WorkGroup> result2 = workGroupService.getWorkGroupsByProjects(List.of(project2));
        List<WorkGroup> result3 = workGroupService.getWorkGroupsByProjects(List.of(project3));
        List<WorkGroup> result4 = workGroupService.getWorkGroupsByProjects(List.of(project4));

        List<WorkGroup> result12 = workGroupService.getWorkGroupsByProjects(List.of(project1, project2));
        List<WorkGroup> result34 = workGroupService.getWorkGroupsByProjects(List.of(project3, project4));

        assertEquals(List.of(workGroup1, workGroup3), result1);
        assertEquals(List.of(workGroup1, workGroup4), result2);
        assertEquals(List.of(workGroup2, workGroup3), result3);
        assertEquals(List.of(workGroup2, workGroup4), result4);
        assertEquals(List.of(workGroup1, workGroup3, workGroup4), result12);
        assertEquals(List.of(workGroup2, workGroup3, workGroup4), result34);
    }

    @Test
    @Transactional
    @WithMockUser(username = "groupLeader", authorities = {"MANAGER"})
    public void testCreateWorkGroup() {

        int numberOfWorkGroupsBefore = workGroupService.getAllEnabled().size();

        WorkGroup testWorkGroup = workGroupService.createNewWorkGroup();
        testWorkGroup.setName("Test WorkGroup");

        workGroupService.save(testWorkGroup);

        int numberOfWorkGroupsAfter = workGroupService.getAllEnabled().size();

        assertEquals(numberOfWorkGroupsBefore + 1, numberOfWorkGroupsAfter);

        // Assert content of saved workgroup
        WorkGroup savedWorkGroup = workGroupService.getGroupByName("Test WorkGroup").orElseThrow();
        assertEquals("Test WorkGroup", savedWorkGroup.getName());
    }

    @Test
    @WithMockUser(username = "unauthorized", authorities = {"EMPLOYEE"})
    public void testCreateWorkGroupUnauthorized() {

        int numberOfWorkGroupsBefore = workGroupService.getAllEnabled().size();

        WorkGroup testWorkGroup = new WorkGroup();
        testWorkGroup.setName("Test WorkGroup");

        assertThrows(org.springframework.security.access.AccessDeniedException.class, () -> {
            workGroupService.save(testWorkGroup);
        });

        int numberOfWorkGroupsAfter = workGroupService.getAllEnabled().size();
        assertEquals(numberOfWorkGroupsBefore, numberOfWorkGroupsAfter);
    }

    @Test
    @Transactional
    @WithMockUser(username = "admin", authorities = {"ADMINISTRATOR"})
    public void testDeleteWorkGroup() {

        int numberOfWorkGroupsBefore = workGroupService.getAllEnabled().size();

        Optional<WorkGroup> workGroupToDelete = workGroupService.getGroupByName("Group 1");
        assertTrue(workGroupToDelete.isPresent());

        Userx oldGroupLeader = workGroupToDelete.get().getGroupLeader();
        assertTrue(oldGroupLeader.getRoles().contains(UserxRole.GROUP_LEADER));

        workGroupService.delete(workGroupToDelete.get().getId());

        int numberOfWorkGroupsAfter = workGroupService.getAllEnabled().size();

        assertEquals(numberOfWorkGroupsBefore - 1, numberOfWorkGroupsAfter);
        assertFalse(workGroupService.getGroupByName("Work Group 1").isPresent());
    }

    @Test
    @WithMockUser(username = "unauthorized", authorities = {"EMPLOYEE"})
    public void testDeleteWorkGroupUnauthorized() {
        int numberOfWorkGroupsBefore = workGroupService.getAllEnabled().size();

        WorkGroup workGroupToDelete = workGroupService.getGroupById(1L).orElseThrow();

        assertThrows(org.springframework.security.access.AccessDeniedException.class, () -> {
            workGroupService.delete(workGroupToDelete.getId());
        });

        int numberOfWorkGroupsAfter = workGroupService.getAllEnabled().size();

        assertEquals(numberOfWorkGroupsBefore, numberOfWorkGroupsAfter);
    }

    @Test
    @Transactional
    @WithMockUser(username = "groupLeader", authorities = {"GROUP_LEADER"})
    public void testEditWorkGroup() {
        WorkGroup workGroupToEdit = workGroupService.getGroupByName("Group 1").orElseThrow();
        workGroupToEdit.setName("Edited Work Group 1");
        workGroupToEdit.setDescription("Edited Description");

        workGroupService.save(workGroupToEdit);

        WorkGroup editedWorkGroup = workGroupService.getGroupByName("Edited Work Group 1").orElseThrow();
        assertEquals("Edited Work Group 1", editedWorkGroup.getName());
        assertEquals("Edited Description", editedWorkGroup.getDescription());
    }

    @Test
    @Transactional
    @WithMockUser(username = "admin", authorities = {"ADMINISTRATOR"})
    public void testFindSelectableWorkGroupsForProjectAndUser() throws EntityValidationException {
        Userx user = userxRepository.save(Userx.builder().username("testUser").password("password").workGroups(new HashSet<>()).build());
        Project project = projectService.save(Project.builder().name("TestProject").disabled(false).build());

        WorkGroup workGroup1 = workGroupService.save(WorkGroup.builder().name("TestGroup1").projects(new HashSet<>(Set.of(project))).users(new HashSet<>(Set.of(user))).disabled(false).build());
        WorkGroup workGroup2 = workGroupService.save(WorkGroup.builder().name("TestGroup2").projects(new HashSet<>(Set.of(project))).users(new HashSet<>()).disabled(false).build());
        WorkGroup workGroup3 = workGroupService.save(WorkGroup.builder().name("TestGroup3").projects(new HashSet<>(Set.of(project))).users(new HashSet<>(Set.of(user))).disabled(true).build());

        List<WorkGroup> selectableWorkGroups = workGroupService.findSelectableWorkGroupsForProjectAndUser(project, user);

        assertEquals(1, selectableWorkGroups.size(), "Selectable work groups size should be 1.");
        assertTrue(selectableWorkGroups.contains(workGroup1), "Selectable work groups should contain workGroup1.");
    }

    @Test
    @Transactional
    @WithMockUser(username = "admin", authorities = {"ADMINISTRATOR"})
    public void testGetAllEnabled() {
        int totalBefore = workGroupRepository.findAll().size();
        WorkGroup workGroup1 = workGroupService.save(WorkGroup.builder().name("TestGroup1").disabled(false).build());
        WorkGroup workGroup2 = workGroupService.save(WorkGroup.builder().name("TestGroup2").disabled(false).build());
        WorkGroup workGroup3 = workGroupService.save(WorkGroup.builder().name("TestGroup3").disabled(true).build());

        List<WorkGroup> allWorkGroups = workGroupService.getAllEnabled();

        assertTrue(allWorkGroups.contains(workGroup1), "All work groups should contain workGroup1.");
        assertTrue(allWorkGroups.contains(workGroup2), "All work groups should contain workGroup2.");
        assertFalse(allWorkGroups.contains(workGroup3), "All work groups should not contain workGroup3.");
        assertEquals(totalBefore + 2, allWorkGroups.size(), "All work groups size should be 3.");
    }



}

