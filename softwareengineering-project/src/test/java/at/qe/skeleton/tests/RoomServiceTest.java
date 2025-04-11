package at.qe.skeleton.tests;

import at.qe.skeleton.exceptions.EntityStillInUseException;
import at.qe.skeleton.model.*;
import at.qe.skeleton.repositories.AccessPointRepository;
import at.qe.skeleton.repositories.LimitsRepository;
import at.qe.skeleton.repositories.RoomRepository;
import at.qe.skeleton.repositories.TemperaDeviceRepository;
import at.qe.skeleton.services.AccessPointService;
import at.qe.skeleton.services.TemperaDeviceService;
import at.qe.skeleton.services.room.RoomService;
import at.qe.skeleton.exceptions.EntityValidationException;
import at.qe.skeleton.exceptions.IdAlreadyExistsException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@WebAppConfiguration
public class RoomServiceTest {
    @Autowired
    TemperaDeviceRepository temperaDeviceRepository;
    @Autowired
    RoomRepository roomRepository;
    @Autowired
    AccessPointRepository accessPointRepository;
    @Autowired
    LimitsRepository limitsRepository;
    @Autowired
    private RoomService roomService;
    @Autowired
    private TemperaDeviceService temperaDeviceService;
    @Autowired
    private AccessPointService accessPointService;


    @Test
    @Transactional
    @WithMockUser(username = "admin", authorities = {"ADMINISTRATOR"})
    public void testDeleteRoom() throws EntityStillInUseException {
        Limits limits = Limits.builder()
                .upperLimit(30)
                .lowerLimit(20)
                .sensorType(SensorType.AIR_TEMPERATURE)
                .build();
        limits = limitsRepository.save(limits);

        Room room = Room.builder()
                .roomNumber("123")
                .limitsList(new ArrayList<>(List.of(limits)))
                .floor("1")
                .build();

        Room savedRoom = roomRepository.save(room);
        limits.setRoom(room);
        limits = limitsRepository.save(limits);

        assertEquals(room, roomRepository.findByRoomNumber("123"));

        AccessPoint accessPoint = AccessPoint.builder()
                .room(room)
                .build();

        accessPoint = accessPointRepository.save(accessPoint);

        TemperaDevice temperaDevice = TemperaDevice.builder()
                .accessPoint(accessPoint)
                .status(DeviceStatus.DISABLED)
                .build();
        temperaDeviceRepository.save(temperaDevice);

        accessPoint.setTemperaDevices(new ArrayList<>(List.of(temperaDevice)));
        accessPoint = accessPointRepository.save(accessPoint);


        List<TemperaDevice> connectedTemperaDevices = temperaDeviceService.getTemperaDevicesByRoom(room);

        assertEquals(temperaDeviceRepository.findTemperaDeviceById(temperaDevice.getId()), connectedTemperaDevices.get(0));

        Assertions.assertThrows(EntityStillInUseException.class, () -> {
            roomService.deleteRoom(savedRoom);
        });

        temperaDeviceService.delete(connectedTemperaDevices.get(0));
        accessPointService.delete(accessPoint);

        Assertions.assertDoesNotThrow(() -> {
            roomService.deleteRoom(savedRoom);
        });
    }


    private Room createTestRoom(String roomNumber, String floor, LocalDateTime createDate, double lowerLimit, double upperLimit) {
        Room room = new Room();
        room.setRoomNumber(roomNumber);
        room.setFloor(floor);
        room.setCreateDate(createDate);
        Limits limits = new Limits();
        limits.setLowerLimit(lowerLimit);
        limits.setUpperLimit(upperLimit);
        room.setLimitsList(List.of(limits));
        return room;
    }

    @Test
    @Transactional
    @WithMockUser(username = "admin", authorities = {"ADMINISTRATOR"})
    public void testSaveNewRoom() {
        Room room = createTestRoom("TestRoom", "TestFloor", LocalDateTime.now(), 1.0, 100.0);

        try {
            Room savedRoom = roomService.saveRoom(room);
            assertNotNull(savedRoom);
            assertEquals(room.getRoomNumber(), savedRoom.getRoomNumber());
            assertEquals(room.getFloor(), savedRoom.getFloor());
            assertEquals(LocalDate.now(), savedRoom.getCreateDate().toLocalDate());
        } catch (IdAlreadyExistsException | EntityValidationException e) {
            fail("Unexpected exception thrown while saving the room: " + e.getMessage());
        }
    }

    @Test
    @Transactional
    @WithMockUser(username = "admin", authorities = {"ADMINISTRATOR"})
    public void testSaveRoomWithoutNumber() {

        Room room = createTestRoom(null, "TestFloor", LocalDateTime.now(), 0.0, 100.0);

        assertThrows(EntityValidationException.class, () -> roomService.saveRoom(room), "roomNumber = null should throw an exception.");

        room.setRoomNumber("");
        assertThrows(EntityValidationException.class, () -> roomService.saveRoom(room), "roomNumber = empty string should throw an exception.");

        room.setRoomNumber("    ");
        assertThrows(EntityValidationException.class, () -> roomService.saveRoom(room), "roomNumber = blank string should throw an exception.");

    }

    @Test
    @Transactional
    @WithMockUser(username = "admin", authorities = {"ADMINISTRATOR"})
    public void testSaveRoomWithExistingNumber() {

        roomRepository.save(createTestRoom("TestRoom", "TestFloor", LocalDateTime.now(), 0.0, 100.0));

        Room roomToBeSaved = createTestRoom("TestRoom", "AnotherFloor", null, 10.0, 50.0);

        assertThrows(IdAlreadyExistsException.class, () -> roomService.saveRoom(roomToBeSaved));
    }

    @Test
    @Transactional
    @WithMockUser(username = "admin", authorities = {"ADMINISTRATOR"})
    public void testUpdateRoom() {

        Room originalRoom = createTestRoom("TestRoom", "TestFloor", LocalDateTime.now(), 0.0, 100.0);
        roomRepository.save(originalRoom);

        Room roomToUpdate = roomRepository.findByRoomNumber("TestRoom");
        assertNotNull(roomToUpdate);
        roomToUpdate.setFloor("UpdatedFloor");

        try {
            Room updatedRoom = roomService.saveRoom(roomToUpdate);
            assertEquals("UpdatedFloor", updatedRoom.getFloor());
        } catch (IdAlreadyExistsException | EntityValidationException e) {
            fail("Exception thrown during room updating: " + e.getMessage());
        }
    }



    @Test
    @Transactional
    @WithMockUser(username = "admin", authorities = {"ADMINISTRATOR"})
    public void testSaveRoomWithInvalidLimit() {
        Room room = createTestRoom("TestRoom", "TestFloor", LocalDateTime.now(), 100.0, 50.0);

        Assertions.assertThrows(EntityValidationException.class, () -> roomService.saveRoom(room), "Expected EntityValidationException for invalid limit");
    }


    /**
     * Only Admin is allowed to save, delete and create rooms.
     */
    @Test
    @WithMockUser(username = "employee", authorities = {"EMPLOYEE", "MANAGER", "GROUP_LEADER"})
    public void testUnauthorizedSaveRoom() {
        Room room = new Room();
        Assertions.assertThrows(org.springframework.security.access.AccessDeniedException.class, () -> roomService.saveRoom(room));
    }

    /**
     * Only Admin is allowed to save, delete and create rooms.
     */
    @Test
    @WithMockUser(username = "employee", authorities = {"EMPLOYEE", "MANAGER", "GROUP_LEADER"})
    public void testUnauthorizedDeleteRoom() {
        Room room = new Room();
        Assertions.assertThrows(org.springframework.security.access.AccessDeniedException.class, () -> roomService.deleteRoom(room));
    }

    /**
     * Only Admin is allowed to save, delete and create rooms.
     */
    @Test
    @WithMockUser(username = "employee", authorities = {"EMPLOYEE", "MANAGER", "GROUP_LEADER"})
    public void testUnauthorizedCreateRoom() {
        Assertions.assertThrows(org.springframework.security.access.AccessDeniedException.class, () -> roomService.createRoom());
    }
}
