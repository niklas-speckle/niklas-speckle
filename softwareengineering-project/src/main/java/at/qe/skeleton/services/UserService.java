package at.qe.skeleton.services;

import at.qe.skeleton.configs.WebSecurityConfig;
import at.qe.skeleton.model.*;
import at.qe.skeleton.exceptions.EntityValidationException;
import at.qe.skeleton.repositories.*;

import java.util.*;
import java.util.stream.Collectors;

import at.qe.skeleton.services.climate.WarningService;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.interceptor.TransactionAspectSupport;

/**
 * Service for accessing and manipulating user data.
 * <p>
 * This class is part of the skeleton project provided for students of the
 * course "Software Engineering" offered by the University of Innsbruck.
 */
@Component
@Scope("application")
public class UserService {

    @Autowired
    private UserxRepository userRepository;

    @Autowired
    private ProjectRepository projectRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private EntityManager entityManager;
    @Autowired
    private WarningService warningService;
    @Autowired
    private ClimateMeasurementRepository climateMeasurementRepository;
    @Autowired
    private SensorRepository sensorRepository;

    /**
     * Returns a collection of all users.
     *
     * @return
     */
    @PreAuthorize("hasAuthority('ADMINISTRATOR') or hasAuthority('MANAGER') or hasAuthority('GROUP_LEADER')")
    public Collection<Userx> getAllUsers() {
        return userRepository.findAll();
    }

    /**
     * Returns a list of all coworkers from a users group if their workModeVisibility is not private.
     *
     * @return
     */
    public List<Userx> getWorkGroupCoworkers(Userx user) {
        // Set<> so coworkers are added only once.
        Set<Userx> users = new HashSet<>();
        for (WorkGroup group : user.getWorkGroups()) {
            for (Userx coworker : group.getUsers()) {
                if (coworker.getWorkModeVisibility() == null || coworker.getWorkModeVisibility() != WorkModeVisibility.HIDDEN) {
                    users.add(coworker);
                }
            }
        }
        users.remove(user);
        return new ArrayList<>(users);
    }


    /**
     * Returns a collection of enabled users.
     */
    @PreAuthorize("hasAuthority('ADMINISTRATOR') or hasAuthority('MANAGER') or hasAuthority('GROUP_LEADER')")
    public Collection<Userx> getAllEnabled() {
        return userRepository.findAll().stream().filter(Userx::isEnabled).collect(Collectors.toList());
    }

    /**
     * Loads a single user identified by its username.
     *
     * @param username the username to search for
     * @return the user with the given username
     */
    public Userx loadUser(String username) {
        return userRepository.findFirstByUsername(username);
    }


    /**
     * Get all users of a specific role.
     *
     * @param userRole the role to search for
     * @return a list of users with the given userRole
     */

    public List<Userx> getUsersByRole(UserxRole userRole) {
        return userRepository.findByRole(userRole);
    }

    /**
     * Saves the user.
     * @param user the user to save
     * @return the updated user
     */

    @Transactional
    @PreAuthorize("hasAnyAuthority('ADMINISTRATOR') or principal.username eq #user.username")
    public Userx saveUser(Userx user) throws EntityValidationException{

        // password and username are mandatory
        if ( user.getUsername() == null || user.getUsername().isEmpty() || user.getUsername().isBlank()) {
            throw new EntityValidationException("Username must not be empty.");
        }
        if ( user.getPassword() == null  || user.getPassword().isBlank() || user.getPassword().isEmpty()) {
            throw new EntityValidationException("Password must not be empty.");
        }

        if (user.isNew()) {
            if(userRepository.findFirstByUsername(user.getUsername()) != null){
                throw new EntityValidationException("A User with username: '" + user.getUsername() + "' already exists.");
            }
            user.setPassword(WebSecurityConfig.passwordEncoder().encode(user.getPassword()));
        }
        return userRepository.save(user);
    }

    /**
     * Sets the user to disabled and clears all personal information, roles, workgroups and projects.
     *
     * @param user the user to delete
     */
    @Transactional
    @PreAuthorize("hasAuthority('ADMINISTRATOR')")
    public void deleteUser(Userx user) {
        user = userRepository.findFirstByUsername(user.getUsername());
        if(user == null) {
            throw new IllegalArgumentException("User does not exist.");
        }
        user.getRoles().clear();
        user.getWorkGroups().clear();
        user.setUpdatedBy(null);
        user.setUpdateDate(null);
        user.setFirstName(null);
        user.setLastName(null);
        user.setEmail(null);
        user.setPhone(null);
        user.setWorkModeVisibility(WorkModeVisibility.HIDDEN);
        user.setDefaultProject(null);
        user.setEnabled(false);

        TemperaDevice temperaDevice = user.getTemperaDevice();

        if (temperaDevice != null) {
            try {
                removeTemperaDevice(user);
            } catch (EntityValidationException e) {
                throw new IllegalArgumentException("Error while removing tempera device: " + e.getMessage());
            }
            if (temperaDevice.getStatus() != DeviceStatus.NOT_REGISTERED) {
                temperaDevice.setStatus(DeviceStatus.DISABLED);
            }
        }

        user.setTemperaDevice(null);

        for (Project project : projectRepository.findByUsersContaining(user)) {
            project.getUsers().remove(user);
            projectRepository.save(project);
        }

        userRepository.save(user);
    }


    public Userx findUserByTemperaDevice(TemperaDevice temperaDevice) {
        return userRepository.findFirstByTemperaDevice(temperaDevice);
    }


    public Userx createUser() {
        Userx user = new Userx();
        user.setEnabled(true);
        user.setRoles(List.of(UserxRole.EMPLOYEE));
        user.setProjects(new HashSet<>());
        user.setWorkGroups(new HashSet<>());
        return user;
    }


    public Userx getUserByTemperaDevice(TemperaDevice temperaDevice) {
        return userRepository.findByTemperaDevice(temperaDevice);
    }

    /**
     * Sets a user to the temperaDevice. Because the user ones the device in our model we have to inversely set the tempera device to the user.
     * The temperaDevice must not be null as the function acts like a setter for the user to a TD.
     * Thus, when the user is null it should withdraw the TemperaDevice from the old user.
     * Else, it should set the TemperaDevice to the new user only if the temperaDevice does not already belong to another user.
     * @param user
     * @param temperaDevice
     * @return
     * @throws EntityValidationException if the user already has another temperaDevice
     * @throws IllegalArgumentException temperaDevice must not be null
     */
    @Transactional
    @PreAuthorize("hasAnyAuthority('ADMINISTRATOR')")
    public Userx setTemperaDevice(Userx user, TemperaDevice temperaDevice) throws EntityValidationException, IllegalArgumentException{

        try{
            if(temperaDevice == null){
                throw new IllegalArgumentException("TemperaDevice must not be null.");
            }

            Userx oldUser = getUserByTemperaDevice(temperaDevice);

            if(user == null){
                if(oldUser == null){
                    return null;
                }
                // need to flush to avoid unique constraint violation
                removeTemperaDevice(oldUser);
                entityManager.flush();
                return null;
            } else {
                boolean userHasDevice = user.getTemperaDevice() != null;
                if(userHasDevice && !user.getTemperaDevice().getId().equals(temperaDevice.getId())){
                    throw new EntityValidationException("User '" + user.getUsername() + "' already has a device with 'id = " + user.getTemperaDevice().getId() + "'. You first have to remove this device before adding a new one.");
                }

                if(oldUser != null){
                    // need to flush to avoid unique constraint violation
                    removeTemperaDevice(oldUser);
                    entityManager.flush();
                }

                user.setTemperaDevice(temperaDevice);
                return saveUser(user);
            }
        } catch (RuntimeException e){
            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
            throw new EntityValidationException("Error while setting tempera device: " + e.getMessage());
        }



    }


    /**
     * Removes the temperaDevice from the user and clears the Warnings and ClimateMeasurements of the device, because
     * these are seen as personal data of the user and as he has no connection to the TemperaDevice anymore, all the
     * User's data still connected to the TemperaDevice should be removed.
     * @param user
     * @return
     * @throws EntityValidationException if user cannot be persisted
     */
    @Transactional
    public Userx removeTemperaDevice(Userx user) throws EntityValidationException {
        TemperaDevice oldTemperaDevice = user.getTemperaDevice();

        if (oldTemperaDevice != null)
        {
            if (oldTemperaDevice.getWarnings() != null){
                for (Warning warning : oldTemperaDevice.getWarnings()) {
                    warningService.deleteWarning(warning, oldTemperaDevice);
                }
            }
            for (Sensor sensor : oldTemperaDevice.getSensors()) {
                if (sensor.getClimateMeasurements() != null && !sensor.getClimateMeasurements().isEmpty()){
                    climateMeasurementRepository.deleteAllBySensor(sensor);
                    sensor.getClimateMeasurements().clear();
                    sensorRepository.save(sensor);
                }
            }
        }

        user.setTemperaDevice(null);
        return saveUser(user);
    }

    /**
     * takes a partial name and returns a list of users containing the partial name in their username, first name or
     * last name. The list is sorted alphabetically and filtered to exclude the requesting user; furthermore the set
     * workModeVisibility is respected: users with workModeVisibility PRIVATE are excluded, users with
     * workModeVisibility HIDDEN are also excluded, unless they are in the same workGroup as the requesting user.
     * @param partialName the String to search for in the username, first name or last name
     * @param requestingUser the user requesting the search
     * @return a list of users containing the partialName
     */
    public List<Userx> findUserContaining(String partialName, Userx requestingUser) {

        List<Userx> suggestedUsers = userRepository.findByUsernameContainingIgnoreCaseOrFirstNameContainingIgnoreCaseOrLastNameContainingIgnoreCase(partialName, partialName, partialName);

        suggestedUsers.removeIf(user ->
                user.equals(requestingUser) ||
                        (user.getWorkModeVisibility() != null && (user.getWorkModeVisibility() == WorkModeVisibility.HIDDEN ||
                                (user.getWorkModeVisibility() == WorkModeVisibility.PRIVATE &&
                                        !areUsersInSameWorkGroup(user, requestingUser))))
        );


        suggestedUsers.sort((o1, o2) -> o1.getUsername().compareToIgnoreCase(o2.getUsername()));
        return suggestedUsers;
    }

    private boolean areUsersInSameWorkGroup(Userx user, Userx requestingUser) {
        Set<WorkGroup> requestingUserWorkGroups = requestingUser.getWorkGroups();
        if (requestingUserWorkGroups == null || requestingUserWorkGroups.isEmpty()) {
            return false;
        }
        Set<WorkGroup> userWorkGroups = user.getWorkGroups();
        if (userWorkGroups == null || userWorkGroups.isEmpty()) {
            return false;
        }

        for (WorkGroup workGroup : requestingUserWorkGroups) {
            if (userWorkGroups.contains(workGroup)) {
                return true;
            }
        }
        return false;
    }

    public void changePassword(Userx user, String newPassword) throws EntityValidationException {
        if (newPassword != null) {
            user.setPassword(passwordEncoder.encode(newPassword));
            saveUser(user);
        } else {throw new EntityValidationException("New password must not be empty.");}
    }
}
