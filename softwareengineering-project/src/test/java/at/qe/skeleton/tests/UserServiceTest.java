package at.qe.skeleton.tests;

import at.qe.skeleton.exceptions.EntityValidationException;
import at.qe.skeleton.model.*;
import at.qe.skeleton.repositories.ProjectRepository;
import at.qe.skeleton.repositories.TemperaDeviceRepository;
import at.qe.skeleton.repositories.UserxRepository;
import at.qe.skeleton.repositories.WorkGroupRepository;
import at.qe.skeleton.services.timeTracking.WorkGroupService;
import jakarta.transaction.Transactional;
import at.qe.skeleton.services.TemperaDeviceService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.web.WebAppConfiguration;

import at.qe.skeleton.services.UserService;

import java.util.*;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Some very basic tests for {@link UserService}.
 * <p>
 * This class is part of the skeleton project provided for students of the
 * course "Software Engineering" offered by the University of Innsbruck.
 */
@SpringBootTest
@WebAppConfiguration
public class UserServiceTest {


    @Autowired
    ProjectRepository projectRepository;

    @Autowired
    TemperaDeviceService temperaDeviceService;
    @Autowired
    private UserxRepository userxRepository;
    @Autowired
    private WorkGroupRepository workGroupRepository;
    @Autowired
    private WorkGroupService workGroupService;
    @Autowired
    private TemperaDeviceRepository temperaDeviceRepository;
    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private UserService userService;

    @DirtiesContext
    @Test
    @Transactional
    @WithMockUser(username = "admin", authorities = {"ADMINISTRATOR"})
    public void testDeleteUser() {
        String username = "user2";
        Userx adminUser = userService.loadUser("admin");
        Assertions.assertNotNull(adminUser, "Admin user could not be loaded from test data source");
        Userx toBeDeletedUser = userService.loadUser(username);
        Assertions.assertNotNull(toBeDeletedUser, "User \"" + username + "\" could not be loaded from test data source");
        int initialNumberOfUsers = userService.getAllUsers().size();

        TemperaDevice temperaDevice = toBeDeletedUser.getTemperaDevice();
        Assertions.assertNotNull(temperaDevice, "User \"" + username + "\" does not have a temperaDevice");
        Assertions.assertSame(DeviceStatus.ENABLED, temperaDevice.getStatus(), "User \"" + username + "\"'s temperaDevice is not enabled");
        userService.deleteUser(toBeDeletedUser);

        Assertions.assertEquals(initialNumberOfUsers, userService.getAllUsers().size(), "Number of users changed after calling UserService.deleteUser");
        Userx deletedUser = userService.loadUser(username);
        Assertions.assertFalse(deletedUser.isEnabled(), "Deleted User \"" + username + "\" is still enabled");
        Assertions.assertTrue(deletedUser.getRoles().isEmpty(), "Deleted User \"" + username + "\" still has roles");
        Assertions.assertTrue(deletedUser.getWorkGroups().isEmpty(), "Deleted User \"" + username + "\" still has workgroups");
        Assertions.assertNull(deletedUser.getEmail(), "Deleted User \"" + username + "\" still has email");
        Assertions.assertTrue(projectRepository.findByUsersContaining(deletedUser).isEmpty(), "Deleted User \"" + username + "\" still has projects");

        for (Userx remainingUser : userService.getAllEnabled()) {
            Assertions.assertNotEquals(toBeDeletedUser.getUsername(), remainingUser.getUsername(), "Deleted User \"" + username + "\" could still be loaded from test data source via UserService.getAllUsers");
        }

        TemperaDevice temperaDeviceAfterDeletion = temperaDeviceRepository.findTemperaDeviceById(temperaDevice.getId());
        Assertions.assertNull(deletedUser.getTemperaDevice(), "Deleted User still has a temperaDevice");
        Assertions.assertSame(DeviceStatus.DISABLED, temperaDeviceAfterDeletion.getStatus(), "Deleted User's temperaDevice is not disabled");
    }

    @DirtiesContext
    @Transactional
    @Test
    @WithMockUser(username = "admin", authorities = {"ADMINISTRATOR"})
    public void testUpdateUser() throws EntityValidationException {
        String username = "manager";
        Userx adminUser = userService.loadUser("admin");
        Assertions.assertNotNull(adminUser, "Admin user could not be loaded from test data source");
        Userx toBeSavedUser = userService.loadUser(username);
        Assertions.assertNotNull(toBeSavedUser, "User \"" + username + "\" could not be loaded from test data source");

        Assertions.assertNull(toBeSavedUser.getUpdatedBy(), "User \"" + username + "\" has a updateUser defined");
        Assertions.assertNull(toBeSavedUser.getUpdateDate(), "User \"" + username + "\" has a updateDate defined");

        toBeSavedUser.setEmail("changed-email@whatever.wherever");
        userService.saveUser(toBeSavedUser);

        Userx freshlyLoadedUser = userService.loadUser("manager");
        Assertions.assertNotNull(freshlyLoadedUser, "User \"" + username + "\" could not be loaded from test data source after being saved");
        Assertions.assertNotNull(freshlyLoadedUser.getUpdatedBy(), "User \"" + username + "\" does not have a updateUser defined after being saved");
        Assertions.assertEquals(adminUser.getUsername(), freshlyLoadedUser.getUpdatedBy(), "User \"" + username + "\" has wrong updateUser set");
        Assertions.assertNotNull(freshlyLoadedUser.getUpdateDate(), "User \"" + username + "\" does not have a updateDate defined after being saved");
        Assertions.assertEquals("changed-email@whatever.wherever", freshlyLoadedUser.getEmail(), "User \"" + username + "\" does not have a the correct email attribute stored being saved");
    }

    @DirtiesContext
    @Transactional
    @Test
    @WithMockUser(username = "admin", authorities = {"ADMINISTRATOR"})
    public void testCreateUser() throws EntityValidationException {
        Userx adminUser = userService.loadUser("admin");
        Assertions.assertNotNull(adminUser, "Admin user could not be loaded from test data source");

        String username = "newuser";
        String password = "passwd";
        String fName = "New";
        String lName = "User";
        String email = "new-email@whatever.wherever";
        String phone = "+12 345 67890";
        Userx toBeCreatedUser = new Userx();
        toBeCreatedUser.setUsername(username);
        toBeCreatedUser.setPassword(password);
        toBeCreatedUser.setEnabled(true);
        toBeCreatedUser.setFirstName(fName);
        toBeCreatedUser.setLastName(lName);
        toBeCreatedUser.setEmail(email);
        toBeCreatedUser.setPhone(phone);
        toBeCreatedUser.setRoles(List.of(UserxRole.EMPLOYEE, UserxRole.MANAGER));
        userService.saveUser(toBeCreatedUser);

        Userx freshlyCreatedUser = userService.loadUser(username);
        Assertions.assertNotNull(freshlyCreatedUser, "New user could not be loaded from test data source after being saved");
        Assertions.assertEquals(username, freshlyCreatedUser.getUsername(), "New user could not be loaded from test data source after being saved");
        Assertions.assertNotEquals(password, freshlyCreatedUser.getPassword(), "The password attribute is being saved unencrypted");
        Assertions.assertEquals(fName, freshlyCreatedUser.getFirstName(), "User \"" + username + "\" does not have a the correct firstName attribute stored being saved");
        Assertions.assertEquals(lName, freshlyCreatedUser.getLastName(), "User \"" + username + "\" does not have a the correct lastName attribute stored being saved");
        Assertions.assertEquals(email, freshlyCreatedUser.getEmail(), "User \"" + username + "\" does not have a the correct email attribute stored being saved");
        Assertions.assertEquals(phone, freshlyCreatedUser.getPhone(), "User \"" + username + "\" does not have a the correct phone attribute stored being saved");
        Assertions.assertTrue(freshlyCreatedUser.getRoles().contains(UserxRole.MANAGER), "User \"" + username + "\" does not have role MANAGER");
        Assertions.assertTrue(freshlyCreatedUser.getRoles().contains(UserxRole.EMPLOYEE), "User \"" + username + "\" does not have role EMPLOYEE");
        Assertions.assertNotNull(freshlyCreatedUser.getCreatedBy(), "User \"" + username + "\" does not have a createUser defined after being saved");
        Assertions.assertEquals(adminUser.getUsername(), freshlyCreatedUser.getCreatedBy(), "User \"" + username + "\" has wrong createUser set");
        Assertions.assertNotNull(freshlyCreatedUser.getCreateDate(), "User \"" + username + "\" does not have a createDate defined after being saved");
    }

    @Test
    @Transactional
    @WithMockUser(username = "admin", authorities = {"ADMINISTRATOR"})
    public void testExceptionForEmptyUsername() {
        Userx user = new Userx();
        user.setUsername(null);
        user.setPassword("password");

        EntityValidationException exception = assertThrows(EntityValidationException.class, () -> {
            userService.saveUser(user);
        }, "username = null should throw an error");
        assertEquals("Username must not be empty.", exception.getMessage());

        user.setUsername("");
        EntityValidationException exception2 = assertThrows(EntityValidationException.class, () -> {
            userService.saveUser(user);
        }, "username = empty string should throw an error");
        assertEquals("Username must not be empty.", exception2.getMessage());

        user.setUsername("      ");
        EntityValidationException exception3 = assertThrows(EntityValidationException.class, () -> {
            userService.saveUser(user);
        }, "username = blank string should throw an error");
        assertEquals("Username must not be empty.", exception3.getMessage());
    }

    @Test
    @Transactional
    @WithMockUser(username = "admin", authorities = {"ADMINISTRATOR"})
    public void testExceptionForEmptyPassword() {
        Userx user = new Userx();
        user.setUsername("username");
        user.setPassword(null);

        EntityValidationException exception = assertThrows(EntityValidationException.class, () -> {
            userService.saveUser(user);
        }, "password = null should throw an error");
        assertEquals("Password must not be empty.", exception.getMessage());

        user.setPassword("");
        EntityValidationException exception2 = assertThrows(EntityValidationException.class, () -> {
            userService.saveUser(user);
        }, "password = empty string should throw an error");
        assertEquals("Password must not be empty.", exception2.getMessage());

        user.setPassword("     ");
        EntityValidationException exception3 = assertThrows(EntityValidationException.class, () -> {
            userService.saveUser(user);
        }, "password = blank string should throw an error");
        assertEquals("Password must not be empty.", exception3.getMessage());
    }


    @Test
    @Transactional
    public void testUnauthenticateddLoadUsers() {
        assertThrows(org.springframework.security.authentication.AuthenticationCredentialsNotFoundException.class, () -> {
            for (Userx user : userService.getAllUsers()) {
                Assertions.fail("Call to userService.getAllUsers should not work without proper authorization");
            }
        });
    }

    @Test
    @Transactional
    @WithMockUser(username = "user", authorities = {"EMPLOYEE"})
    public void testUnauthorizedLoadUsers() {
        assertThrows(org.springframework.security.access.AccessDeniedException.class, () -> {
            for (Userx user : userService.getAllUsers()) {
                Assertions.fail("Call to userService.getAllUsers should not work without proper authorization");
            }
        });
    }

    @Test
    @Transactional
    @WithMockUser(username = "manager", authorities = {"EMPLOYEE"})
    public void testAuthorizedLoadUser() {
        String username = "manager";
        Userx user = userService.loadUser(username);
        Assertions.assertEquals(username, user.getUsername(), "Call to userService.loadUser returned wrong user");
    }

    @Test
    @Transactional
    @WithMockUser(username = "manager", authorities = {"EMPLOYEE"})
    public void testUnauthorizedSaveUser() {
        assertThrows(org.springframework.security.access.AccessDeniedException.class, () -> {
            String username = "manager";
            Userx user = userService.loadUser("admin");
            userService.saveUser(user);
        }, "Employee should not be able to save other users");
    }

    @Test
    @Transactional
    @WithMockUser(username = "manager", authorities = {"EMPLOYEE"})
    public void testAuthorizedSaveUser() {
        String username = "manager";
        Userx user = userService.loadUser(username);
        Assertions.assertDoesNotThrow(() -> {
            userService.saveUser(user);
        }, "Employee should be able to save themselves");
    }

    @Test
    @Transactional
    @WithMockUser(username = "manager", authorities = {"EMPLOYEE"})
    public void testUnauthorizedDeleteUser() {
        assertThrows(org.springframework.security.access.AccessDeniedException.class, () -> {
            Userx user = userService.loadUser("manager");
            Assertions.assertEquals("manager", user.getUsername(), "Call to userService.loadUser returned wrong user");
            userService.deleteUser(user);
        });
    }

    @Test
    @Transactional
    @WithMockUser(username = "admin", authorities = {"ADMINISTRATOR"})
    public void testGetUsersByRole() {

        for(UserxRole role : UserxRole.values()) {
            List<Userx> users = userService.getUsersByRole(role);

            // Check if all users in the returned list have the expected role
            for (Userx user : users) {
                Assertions.assertTrue(user.getRoles().contains(role), "User \"" + user.getUsername() + "\" does not have role " + role);
            }

            // Check if there are no users with the expected role missing in the returned list
            for (Userx user : userService.getAllUsers()) {
                if (user.getRoles().contains(role)) {
                    Assertions.assertTrue(users.contains(user), "User \"" + user.getUsername() + "\" with role " + role + " is missing in the returned list");
                }
            }
        }
    }

    //TODO: more extensive testing if enough time.
    @Test
    @Transactional
    public void testFindUserByTemperaDevice(){
        Userx user = userService.findUserByTemperaDevice(temperaDeviceService.findTemperaDeviceById(1));
        Assertions.assertEquals("user2", user.getUsername());
    }

    @Test
    @WithMockUser(username = "admin" , authorities = {"ADMINISTRATOR"})
    public void testFindUserContainingDontSimpleSearch(){
        List<Userx> users = userService.findUserContaining("use", userService.loadUser("admin"));
        Assertions.assertEquals(1, users.size());
        Assertions.assertTrue(users.contains(userService.loadUser("user2")));
    }

    @Test
    @WithMockUser(username = "user2" , authorities = {"ADMINISTRATOR"})
    public void testFindUserContainingDontIncludeSearcher(){
        List<Userx> users = userService.findUserContaining("use", userService.loadUser("user2"));
        Assertions.assertEquals(0, users.size());
        Assertions.assertFalse(users.contains(userService.loadUser("user2")));
    }

    @Transactional
    @Test
    @WithMockUser(username = "admin", authorities = {"ADMINISTRATOR"})
    public void testFindUserContainingWorkgroup(){

        WorkGroup testWorkGroup = new WorkGroup();
        testWorkGroup.setName("testWorkGroup");
        testWorkGroup.setUsers(new HashSet<>());
        workGroupRepository.save(testWorkGroup);

        Userx testUser = new Userx();
        testUser.setUsername("testUser");
        testUser.setWorkModeVisibility(WorkModeVisibility.PRIVATE);
        testUser.setWorkGroups(new HashSet<>(Set.of(testWorkGroup)));
        testUser = userxRepository.save(testUser);
        System.out.println("testUser "+testUser.getWorkGroups());

        Userx testUser2 = new Userx();
        testUser2.setUsername("testUser2");
        testUser2.setWorkModeVisibility(WorkModeVisibility.PRIVATE);
        testUser2 = userxRepository.save(testUser2);
        System.out.println("testUser2 "+testUser2.getWorkGroups());

        Userx testUser3 = new Userx();
        testUser3.setUsername("testUser3");
        testUser3.setWorkModeVisibility(WorkModeVisibility.PUBLIC);
        testUser3.setWorkGroups(new HashSet<>(Set.of(testWorkGroup)));
        testUser3 = userxRepository.save(testUser3);
        System.out.println("testUser3 "+testUser3.getWorkGroups());

        Userx testUser4 = new Userx();
        testUser4.setUsername("testUser4");
        testUser4.setWorkModeVisibility(WorkModeVisibility.PUBLIC);
        testUser4.setWorkGroups(new HashSet<>(Set.of(testWorkGroup)));
        testUser4 = userxRepository.save(testUser4);
        System.out.println("testUser4 "+testUser4.getWorkGroups());

        List<Userx> users = userService.findUserContaining("test", userxRepository.findFirstByUsername("testUser"));

        Assertions.assertFalse(users.contains(testUser2), "should not contain user2");
        Assertions.assertTrue(users.contains(testUser3), "should contain user3");
        Assertions.assertTrue(users.contains(testUser4), "should contain user4");

        List<Userx> users2 = userService.findUserContaining("test", userxRepository.findFirstByUsername("testUser2"));
        Assertions.assertEquals(2, users2.size(), "testUser2");
        Assertions.assertFalse(users2.contains(testUser));
        Assertions.assertTrue(users2.contains(testUser3));
        Assertions.assertTrue(users2.contains(testUser4));

        List<Userx> users3 = userService.findUserContaining("test", userxRepository.findFirstByUsername("testUser3"));

        Assertions.assertTrue(users3.contains(testUser), "should contain testUser");
        Assertions.assertFalse(users3.contains(testUser2), "should not contain testUser2");
        Assertions.assertTrue(users3.contains(testUser4), "should contain testUser4");
    }

    @Transactional
    @Test
    @WithMockUser(username = "admin", authorities = {"ADMINISTRATOR"})
    public void testFindUserHidden(){
        WorkGroup testWorkGroup2 = new WorkGroup();
        testWorkGroup2.setName("testWorkGroup2");
        testWorkGroup2.setUsers(new HashSet<>());
        workGroupRepository.save(testWorkGroup2);

        Userx testUser4 = new Userx();
        testUser4.setUsername("testUser4");
        testUser4.setWorkModeVisibility(WorkModeVisibility.HIDDEN);
        testUser4.setWorkGroups(new HashSet<>(Set.of(testWorkGroup2)));
        testUser4 = userxRepository.save(testUser4);

        Userx testUser5 = new Userx();
        testUser5.setUsername("testUser5");
        testUser5.setWorkModeVisibility(WorkModeVisibility.HIDDEN);
        testUser5 = userxRepository.save(testUser5);

        Userx testUser6 = new Userx();
        testUser6.setUsername("testUser6");
        testUser6.setWorkModeVisibility(WorkModeVisibility.PUBLIC);
        testUser6.setWorkGroups(new HashSet<>(Set.of(testWorkGroup2)));
        testUser6 = userxRepository.save(testUser6);

        Userx testUser7 = new Userx();
        testUser7.setUsername("testUser7");
        testUser7.setWorkModeVisibility(WorkModeVisibility.PUBLIC);
        testUser7.setWorkGroups(new HashSet<>(Set.of(testWorkGroup2)));
        testUser7 = userxRepository.save(testUser7);

        List<Userx> users = userService.findUserContaining("test", testUser4);
        Assertions.assertEquals(2, users.size(), "Size of users is not 2");
        Assertions.assertFalse(users.contains(testUser5), "User 5 is in the list");
        Assertions.assertTrue(users.contains(testUser6), "User 6 is not in the list");
        Assertions.assertFalse(users.contains(testUser4), "User 4 is not in the list");

        List<Userx> users2 = userService.findUserContaining("test", testUser6);
        Assertions.assertEquals(1, users2.size(), "Size of users2 is not 1");
        Assertions.assertFalse(users2.contains(testUser4), "User 4 is in the list");
        Assertions.assertFalse(users2.contains(testUser5), "User 5 is in the list");
        Assertions.assertTrue(users2.contains(testUser7), "User 7 is not in the list");
    }


    @Test
    @Transactional

    @WithMockUser(username = "admin", authorities = {"ADMINISTRATOR"})
    public void integrationTestSetTemperaDevice() throws EntityValidationException {

        TemperaDevice temperaDevice = temperaDeviceService.save(TemperaDevice.builder()
                .status(DeviceStatus.ENABLED)
                .sensors(new ArrayList<>(List.of(Sensor.builder().sensorType(SensorType.AIR_TEMPERATURE).build())))
                .build());

        TemperaDevice temperaDeviceUnassigned = temperaDeviceService.save(TemperaDevice.builder().status(DeviceStatus.ENABLED).build());


        Userx oldUser = userService.saveUser(
                Userx.builder().username("oldUser").password("password").enabled(true).temperaDevice(temperaDevice).build());

        Userx newUser = userService.saveUser(
                Userx.builder().username("newUser").password("password").enabled(true).build());

        // should switch temperaDevices
        newUser = userService.setTemperaDevice(newUser, temperaDevice);
        assertEquals(temperaDevice, userService.loadUser(newUser.getUsername()).getTemperaDevice(),  "User does not have the correct temperaDevice");
        Assertions.assertNull(userService.loadUser(oldUser.getUsername()).getTemperaDevice(), "Old user still has a temperaDevice");
        Assertions.assertEquals(newUser, userService.getUserByTemperaDevice(temperaDevice));

        // should have no side effect
        userService.setTemperaDevice(null, temperaDeviceUnassigned);
        assertEquals(temperaDevice, userService.loadUser(newUser.getUsername()).getTemperaDevice(),  "User does not have the correct temperaDevice");
        Assertions.assertNull(userService.loadUser(oldUser.getUsername()).getTemperaDevice(), "Old user still has a temperaDevice");
        Assertions.assertNull(userService.getUserByTemperaDevice(temperaDeviceUnassigned));

        // first assign of TD
        userService.setTemperaDevice(oldUser, temperaDeviceUnassigned);
        assertEquals(temperaDeviceUnassigned, userService.loadUser(oldUser.getUsername()).getTemperaDevice(),  "User does not have the correct temperaDevice");
        Assertions.assertEquals(newUser, userService.getUserByTemperaDevice(temperaDevice));
        Assertions.assertEquals(oldUser, userService.getUserByTemperaDevice(temperaDeviceUnassigned));
    }

    @Test
    @Transactional
    @WithMockUser(username = "allExceptAdmin", authorities = {"MANAGER", "EMPLOYEE", "GROUP_LEADER"})
    public void testUnauthorizedSetTemperaDevice() throws EntityValidationException {

        Userx newUser = new Userx();
        TemperaDevice newTemperaDevice = new TemperaDevice();


        Assertions.assertThrows(
                AccessDeniedException.class,
                () -> {
                    userService.setTemperaDevice(newUser, newTemperaDevice);});
    }

    @Test
    @Transactional
    @WithMockUser(username = "admin", authorities = {"ADMINISTRATOR"})
    public void testGetAllEnabled() throws EntityValidationException {
        int totalBefore = userxRepository.count();
        Userx user1 = new Userx();
        user1.setUsername("enabledUser1");
        user1.setPassword("password1");
        user1.setEnabled(true);
        userService.saveUser(user1);

        Userx user2 = new Userx();
        user2.setUsername("disabledUser2");
        user2.setPassword("password2");
        user2.setEnabled(false);
        userService.saveUser(user2);

        Userx user3 = new Userx();
        user3.setUsername("enabledUser3");
        user3.setPassword("password3");
        user3.setEnabled(true);
        userService.saveUser(user3);

        Collection<Userx> enabledUsers = userService.getAllEnabled();
        Assertions.assertEquals(totalBefore + 2, enabledUsers.size(), "The number of enabled users is incorrect");
        Assertions.assertTrue(enabledUsers.contains(user1), "Enabled user1 is not in the enabled users list");
        Assertions.assertTrue(enabledUsers.contains(user3), "Enabled user3 is not in the enabled users list");
        Assertions.assertFalse(enabledUsers.contains(user2), "Disabled user2 should not be in the enabled users list");
    }

    @Test
    @Transactional
    @WithMockUser(username = "admin", authorities = {"ADMINISTRATOR"})
    public void testGetWorkGroupCoworkers() throws EntityValidationException {
        WorkGroup workGroup = new WorkGroup();
        workGroup.setName("TestGroup");
        workGroup = workGroupRepository.save(workGroup);

        Userx user1 = new Userx();
        user1.setUsername("testUser1");
        user1.setPassword("password1");
        user1.setEnabled(true);
        user1.setWorkGroups(Set.of(workGroup));
        user1 = userService.saveUser(user1);

        Userx user2 = new Userx();
        user2.setUsername("testUser2");
        user2.setPassword("password2");
        user2.setEnabled(true);
        user2.setWorkGroups(Set.of(workGroup));
        user2.setWorkModeVisibility(WorkModeVisibility.PRIVATE);
        user2 = userService.saveUser(user2);

        Userx user3 = new Userx();
        user3.setUsername("testUser3");
        user3.setPassword("password3");
        user3.setEnabled(true);
        user3.setWorkGroups(Set.of(workGroup));
        user3.setWorkModeVisibility(WorkModeVisibility.PUBLIC);
        user3 = userService.saveUser(user3);

        workGroup.setUsers(Set.of(user1, user2, user3));
        workGroup = workGroupRepository.save(workGroup);

        List<Userx> coworkers = userService.getWorkGroupCoworkers(user1);
        Assertions.assertTrue(coworkers.contains(user2), "Coworker user2 is not in the list");
        Assertions.assertTrue(coworkers.contains(user3), "Coworker user3 is not in the list");
        Assertions.assertFalse(coworkers.contains(user1), "The user itself should not be in the coworkers list");
    }

    @Test
    @Transactional
    @WithMockUser(username = "admin", authorities = {"ADMINISTRATOR"})
    public void testChangePassword() throws EntityValidationException {
        Userx user = new Userx();
        user.setUsername("testUser");
        user.setPassword("oldPassword");
        user.setEnabled(true);
        user = userService.saveUser(user);

        String newPassword = "newPassword";
        userService.changePassword(user, newPassword);

        Userx updatedUser = userService.loadUser("testUser");
        Assertions.assertNotEquals("oldPassword", updatedUser.getPassword(), "The password was not updated");
        Assertions.assertTrue(passwordEncoder.matches(newPassword, updatedUser.getPassword()), "The new password does not match the encoded password");
    }



}