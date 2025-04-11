package at.qe.skeleton.repositories;

import at.qe.skeleton.model.*;

import java.util.List;

import at.qe.skeleton.model.WorkGroup;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * Repository for managing {@link Userx} entities.
 *
 * This class is part of the skeleton project provided for students of the
 * course "Software Engineering" offered by the University of Innsbruck.
 */
public interface UserxRepository extends AbstractRepository<Userx, String> {

    Userx findFirstByUsername(String username);

    Userx findFirstByTemperaDevice(TemperaDevice temperaDevice);

    List<Userx> findByUsernameContainingIgnoreCase(String username);

    List<Userx> findByUsernameContainingIgnoreCaseOrFirstNameContainingIgnoreCaseOrLastNameContainingIgnoreCase(String username, String firstName, String lastName);

    @Query("SELECT u FROM Userx u WHERE CONCAT(u.firstName, ' ', u.lastName) = :wholeName")
    List<Userx> findByWholeNameConcat(@Param("wholeName") String wholeName);

    @Query("SELECT u FROM Userx u WHERE :role MEMBER OF u.roles")
    List<Userx> findByRole(@Param("role") UserxRole role);

    /**
     * Retrieves a User entity by its Email.
     *
     * @param email must not be {@literal null}.
     * @return The user with the given email.
     * @throws IllegalArgumentException If email is {@literal null}.
     */
    Userx findFirstByEmail(String email);

    Integer count();
    @Query("SELECT u FROM Userx u JOIN u.workGroups wg WHERE wg IN :workGroups")
    List<Userx> findByWorkGroups(@Param("workGroups") List<WorkGroup> workGroups);

    @Query("SELECT DISTINCT u FROM Userx u JOIN u.projects p WHERE p IN :projects")
    List<Userx> findByProjects(@Param("projects") List<Project> projects);


    List<Userx> findByRoles(UserxRole userRole);

    Userx findByTemperaDevice(TemperaDevice temperaDevice);

}
