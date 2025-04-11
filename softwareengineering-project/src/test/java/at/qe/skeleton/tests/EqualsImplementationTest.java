package at.qe.skeleton.tests;

import at.qe.skeleton.model.*;
import nl.jqno.equalsverifier.EqualsVerifier;
import nl.jqno.equalsverifier.Warning;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.Set;

/**
 * Tests to ensure that each entity's implementation of equals conforms to the
 * contract. See {@linkplain http://www.jqno.nl/equalsverifier/} for more
 * information.
 *
 * This class is part of the skeleton project provided for students of the
 * course "Software Engineering" offered by the University of Innsbruck.
 */
public class EqualsImplementationTest {

    @Test
    public void testUserEqualsContract() {
        Userx user1 = new Userx();
        user1.setUsername("user1");
        // Create dummy instances for recursive data structures
        WorkGroup workGroup1 = new WorkGroup();
        workGroup1.setName("groupName1");
        Set<WorkGroup> workGroupSet1 = Set.of(workGroup1);
        Project project1 = new Project();
        project1.setName("projectName1");
        Set<Project> projectSet1 = Set.of(project1);
        TemperaDevice temperaDevice1 = new TemperaDevice();
        temperaDevice1.setId(1L);
        Sensor sensor1 = new Sensor();
        sensor1.setId(1L);
        ClimateMeasurement climateMeasurement1 = new ClimateMeasurement();
        climateMeasurement1.setId(1L);

        Userx user2 = new Userx();
        user2.setUsername("user2");
        // Create dummy instances for recursive data structures
        WorkGroup workGroup2 = new WorkGroup();
        workGroup2.setName("groupName2");
        Set<WorkGroup> workGroupSet2 = Set.of(workGroup2);
        Project project2 = new Project();
        project2.setName("projectName2");
        Set<Project> projectSet2 = Set.of(project2);
        TemperaDevice temperaDevice2 = new TemperaDevice();
        temperaDevice2.setId(2L);
        Sensor sensor2 = new Sensor();
        sensor2.setId(2L);
        ClimateMeasurement climateMeasurement2 = new ClimateMeasurement();
        climateMeasurement2.setId(2L);

        EqualsVerifier.forClass(Userx.class)
                .withPrefabValues(Userx.class, user1, user2)
                .withPrefabValues(Set.class, workGroupSet1, workGroupSet2)
                .withPrefabValues(Set.class, projectSet1, projectSet2)
                .withPrefabValues(TemperaDevice.class, temperaDevice1, temperaDevice2)
                .withPrefabValues(Sensor.class, sensor1, sensor2)
                .withPrefabValues(ClimateMeasurement.class, climateMeasurement1, climateMeasurement2)
                .suppress(Warning.STRICT_INHERITANCE, Warning.ALL_FIELDS_SHOULD_BE_USED)
                .verify();
    }


    @Test
    public void testUserRoleEqualsContract() {
        EqualsVerifier.forClass(UserxRole.class).verify();
    }

    @Test
    public void testRoomEqualsContract() {
        Room room1 = new Room();
        room1.setRoomNumber("room1");
        room1.setCreateDate(LocalDateTime.of(2024, 1, 1, 0, 0, 0));
        room1.setFloor("2");

        Room room2 = new Room();
        room2.setRoomNumber("room2");
        room2.setCreateDate(LocalDateTime.of(2021, 1, 1, 0, 0, 0));
        room2.setFloor("1");

        EqualsVerifier.forClass(Room.class).withPrefabValues(Room.class, room1, room2).suppress(Warning.STRICT_INHERITANCE, Warning.ALL_FIELDS_SHOULD_BE_USED).verify();
    }

}