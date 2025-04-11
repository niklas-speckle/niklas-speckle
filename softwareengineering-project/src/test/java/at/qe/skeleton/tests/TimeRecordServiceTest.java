package at.qe.skeleton.tests;

import at.qe.skeleton.model.*;
import at.qe.skeleton.repositories.ProjectRepository;
import at.qe.skeleton.repositories.TimeRecordRepository;
import at.qe.skeleton.repositories.UserxRepository;
import at.qe.skeleton.repositories.WorkGroupRepository;
import at.qe.skeleton.services.timeTracking.TimeRecordService;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.web.WebAppConfiguration;

import java.time.LocalDateTime;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@WebAppConfiguration
class TimeRecordServiceTest {

    @Autowired
    TimeRecordService timeRecordService;

    @Autowired
    TimeRecordRepository timeRecordRepository;

    @Autowired
    UserxRepository userxRepository;

    @Autowired
    WorkGroupRepository workGroupRepository;

    @Autowired
    ProjectRepository projectRepository;


    @Test
    void testLoadTimeRecord() {

        TimeRecord timeRecord = timeRecordRepository.findById(1L).orElseThrow();

        TimeRecord loadedTimeRecord = timeRecordService.loadTimeRecord(timeRecord);

        assertEquals(timeRecord, loadedTimeRecord);
    }

    @Test
    @Transactional
    @WithMockUser(username = "user", authorities = {"EMPLOYEE"})
    void testSaveTimeRecord() {
        int totalTimeRecords = timeRecordRepository.findAll().size();
        TimeRecord timeRecord = new TimeRecord();
        timeRecord.setWorkMode(WorkMode.AVAILABLE);

        timeRecord = timeRecordService.saveTimeRecord(timeRecord);
        TimeRecord reloadedTimeRecord = timeRecordService.loadTimeRecord(timeRecord);


        assertEquals(totalTimeRecords + 1, timeRecordRepository.findAll().size(), "After saving a new time record, there should be 1 more than before.");
        assertEquals(timeRecord, reloadedTimeRecord, "When a time record is saved, reloading it from the db should get the same time record.");
    }

    @Test
    @Transactional
    void testGetTimeRecordsForUser() {
        Userx admin = userxRepository.findFirstByUsername("admin");
        List<TimeRecord> timeRecordsForAdmin = timeRecordRepository.findAllByUser(admin);

        List<TimeRecord> timeRecordsForAdminViaService1 = timeRecordService.getTimeRecordsForUser(admin);

        assertTrue(timeRecordsForAdminViaService1.containsAll(timeRecordsForAdmin), "getTimeRecordsForUser(admin) should contain all admin's time records");

        TimeRecord newTimeRecord = new TimeRecord();
        newTimeRecord.setWorkMode(WorkMode.AVAILABLE);
        newTimeRecord.setUser(admin);
        newTimeRecord = timeRecordRepository.save(newTimeRecord);

        List<TimeRecord> timeRecordsForAdminViaService2 = timeRecordService.getTimeRecordsForUser(admin);
        assertTrue(timeRecordsForAdminViaService2.contains(newTimeRecord), "After saving a new time record for admin, getTimeRecordsForUser(admin) should contain the new time record.");
        assertEquals(timeRecordsForAdminViaService1.size() + 1, timeRecordsForAdminViaService2.size(), "After saving a new time record for admin, getTimeRecordsForUser(admin) should contain one more time record than before.");

    }

    @Test
    @Transactional
    @WithMockUser(username = "user", authorities = {"EMPLOYEE"})
    void testEditTimeRecord() {
        Project project = projectRepository.findById(1L).orElseThrow();
        WorkGroup workGroup = workGroupRepository.findById(1L).orElseThrow();
        LocalDateTime startTime = LocalDateTime.now().minusHours(1);
        LocalDateTime endTime = LocalDateTime.now();
        String description = "description";

        TimeRecord timeRecord = timeRecordRepository.findById(1L).orElseThrow();
        timeRecord.setStartTime(startTime);
        timeRecord.setEndTime(endTime);
        timeRecord.setWorkGroup(workGroup);
        timeRecord.setProject(project);
        timeRecord.setDescription(description);

        TimeRecord editedTimeRecord = timeRecordService.editTimeRecord(timeRecord);

        assertEquals(startTime, editedTimeRecord.getStartTime(), "Start time not as expected.");
        assertEquals(endTime, editedTimeRecord.getEndTime(), "End time not as expected.");
        assertEquals(workGroup, editedTimeRecord.getWorkGroup(), "Work group not as expected.");
        assertEquals(project, editedTimeRecord.getProject(), "Project not as expected.");
        assertEquals(description, editedTimeRecord.getDescription(), "Description not as expected.");
    }

    @Test
    @Transactional
    @WithMockUser(username = "user", authorities = {"EMPLOYEE"})
    void testSplitTimeRecord() {
        TimeRecord timeRecord = timeRecordRepository.findById(1L).orElseThrow();

        LocalDateTime start = timeRecord.getStartTime();
        LocalDateTime end = timeRecord.getEndTime();
        LocalDateTime splitAt = start.plusMinutes(10).isBefore(end) ? start.plusMinutes(1) : end.minusMinutes(1);

        List<TimeRecord> splitTimeRecords = timeRecordService.splitTimeRecord(timeRecord, splitAt);

        assertEquals(2, splitTimeRecords.size(), "splitTimeRecords must contain 2 time records.");
        assertEquals(start, splitTimeRecords.get(0).getStartTime(), "First split time record should start at original start.");
        assertEquals(splitAt, splitTimeRecords.get(0).getEndTime(), "First split time record should end at splitAt.");
        assertEquals(splitAt, splitTimeRecords.get(1).getStartTime(), "First split time record should start at splitAt.");
        assertEquals(end, splitTimeRecords.get(1).getEndTime(), "First split time record should end at original end.");
        assertEquals(splitTimeRecords.get(0).getWorkMode(), splitTimeRecords.get(1).getWorkMode(), "Split time records should have the same work mode.");
    }

    @Test
    @Transactional
    @WithMockUser(username = "user", authorities = {"GROUP_LEADER"})
    void testGetTimeRecordsForGroupLeader() {

        Userx groupLeader = userxRepository.findFirstByUsername("groupleader");
        List<WorkGroup> workGroups = workGroupRepository.findByGroupLeader(groupLeader);
        List<TimeRecord> timeRecords = new ArrayList<>();
        for (WorkGroup workGroup : workGroups) {
            timeRecords.addAll(timeRecordRepository.findAllByWorkGroup(workGroup));
        }

        List<TimeRecord> timeRecordsForGroupLeader = timeRecordService.getTimeRecordsForGroupLeader(groupLeader);

        assertTrue(timeRecordsForGroupLeader.containsAll(timeRecords), "All time records for group leader should be retrieved.");
    }

    @Test
    @Transactional
    @WithMockUser(username = "user", authorities = {"MANAGER"})
    void testGetTimeRecordsForManager() {
        Userx manager = userxRepository.findFirstByUsername("admin");
        List<Project> projects = projectRepository.findByProjectManager(manager);
        List<TimeRecord> timeRecords = new ArrayList<>();
        for (Project project : projects) {
            timeRecords.addAll(timeRecordRepository.findAllByProject(project));
        }

        List<TimeRecord> timeRecordsForManager = timeRecordService.getTimeRecordsForManager(manager);

        assertTrue(timeRecordsForManager.containsAll(timeRecords), "All time records for manager should be retrieved.");
    }

    @Test
    void testIsStartAfterEnd() {
        LocalDateTime startTime = LocalDateTime.of(2024, 4, 1, 0, 0);
        LocalDateTime endTime = LocalDateTime.of(2024, 4, 2, 0, 0);
        TimeRecord selectedTimeRecord = new TimeRecord();
        selectedTimeRecord.setStartTime(startTime);
        selectedTimeRecord.setEndTime(endTime);

        // Case 1: StartTime selected by user is after or equal to EndTime selected by user
        LocalDateTime newStartDateTime1 = LocalDateTime.of(2024, 4, 4, 0, 0);
        LocalDateTime newEndDateTime1 = LocalDateTime.of(2024, 4, 3, 0, 0);
        boolean result1 = timeRecordService.isStartAfterEnd(selectedTimeRecord, newStartDateTime1, newEndDateTime1);
        assertTrue(result1, "Should evaluate true if selected start time is after selected end time.");

        // Case 2: StartTime selected by user is after or equal to old EndTime
        LocalDateTime newStartDateTime2 = LocalDateTime.of(2024, 4, 3, 0, 0);
        LocalDateTime newEndDateTime2 = endTime; // Same as old end time
        boolean result2 = timeRecordService.isStartAfterEnd(selectedTimeRecord, newStartDateTime2, newEndDateTime2);
        assertTrue(result2, "Should evaluate true if selected start time is after old end time.");

        // Case 3: EndTime selected by user is before or equal to old StartTime
        LocalDateTime newStartDateTime3 = startTime; // Same as old start time
        LocalDateTime newEndDateTime3 = LocalDateTime.of(2024, 3, 20, 0, 0);
        boolean result3 = timeRecordService.isStartAfterEnd(selectedTimeRecord, newStartDateTime3, newEndDateTime3);
        assertTrue(result3, "Should evaluate true if selected end time is before old start time.");
    }

    @Test
    void testDetectOverlap() {

        TimeRecord selectedTimeRecord = new TimeRecord();
        LocalDateTime startTime1 = LocalDateTime.of(2024, 4, 30, 3, 0);
        LocalDateTime endTime1 = LocalDateTime.of(2024, 4, 30, 4, 0);
        selectedTimeRecord.setStartTime(startTime1);
        selectedTimeRecord.setEndTime(endTime1);


        TimeRecord otherTimeRecordBefore = new TimeRecord();
        LocalDateTime startTime2 = LocalDateTime.of(2024, 4, 30, 1, 0);
        LocalDateTime endTime2 = LocalDateTime.of(2024, 4, 30, 2, 0);
        otherTimeRecordBefore.setStartTime(startTime2);
        otherTimeRecordBefore.setEndTime(endTime2);

        TimeRecord otherTimeRecordAfter = new TimeRecord();
        LocalDateTime startTime3 = LocalDateTime.of(2024, 4, 30, 5, 0);
        LocalDateTime endTime3 = LocalDateTime.of(2024, 4, 30, 6, 0);
        otherTimeRecordAfter.setStartTime(startTime3);
        otherTimeRecordAfter.setEndTime(endTime3);

        // Case 1: New start time causes overlap (start before other time record's end time)
        LocalDateTime newStartDateTime1 = LocalDateTime.of(2024, 4, 30, 1, 30); // Before other time record's end time
        boolean result1 = timeRecordService.detectOverlap(selectedTimeRecord, otherTimeRecordBefore, newStartDateTime1, endTime1);
        assertTrue(result1, "Overlap detected when new start time is before selected time record's start time.");

        // Case 2: New end time causes overlap (end after other time record's start time)
        LocalDateTime newEndDateTime2 = LocalDateTime.of(2024, 4, 30, 5, 30); // After other time record's start time
        boolean result2 = timeRecordService.detectOverlap(selectedTimeRecord, otherTimeRecordAfter, startTime1, newEndDateTime2);
        assertTrue(result2, "Overlap detected when new end time is after selected time record's end time.");

        // Case 3: No overlap
        LocalDateTime newStartDateTime3 = LocalDateTime.of(2024, 4, 30, 2, 30);
        LocalDateTime newEndDateTime3 = LocalDateTime.of(2024, 4, 30, 5, 0);
        boolean result3 = timeRecordService.detectOverlap(selectedTimeRecord, otherTimeRecordBefore, newStartDateTime3, newEndDateTime3);
        assertFalse(result3, "No overlap should be detected.");
    }

    @Test
    @Transactional
    @WithMockUser(username = "user", authorities = {"EMPLOYEE"})
    public void testSaveNewAndCloseOldTimeRecord() {
        // Given
        Userx user = userxRepository.findFirstByUsername("elvis");

        TimeRecord openTimeRecord = new TimeRecord();
        openTimeRecord.setWorkMode(WorkMode.AVAILABLE);
        openTimeRecord.setUser(user);
        openTimeRecord.setStartTime(LocalDateTime.of(2024, 4, 1, 8, 0));
        openTimeRecord = timeRecordRepository.save(openTimeRecord);

        LocalDateTime newStartTime = LocalDateTime.of(2024, 4, 1, 9, 0);

        TimeRecord newTimeRecord = new TimeRecord();
        newTimeRecord.setWorkMode(WorkMode.DEEP_WORK);
        newTimeRecord.setUser(user);
        newTimeRecord.setStartTime(newStartTime);

        // When
        timeRecordService.saveNewAndCloseOldTimeRecord(newTimeRecord);


        // Then
        openTimeRecord = timeRecordRepository.findById(openTimeRecord.getId()).orElseThrow();

        assertNotNull(openTimeRecord, "Old time record should not be null.");
        assertEquals(newStartTime, openTimeRecord.getEndTime(), "End time of the old time record should be equal to the start time of the new time record.");
        assertEquals(newStartTime, timeRecordRepository.findFirstByUserAndEndTimeIsNull(user).getStartTime(), "New time record should have been saved.");
    }

    @Test
    @Transactional
    @WithMockUser(username = "admin", authorities = {"ADMINISTRATOR"})
    void testCloseOpenTimeRecords() {
        Userx user = userxRepository.findFirstByUsername("admin");

        TimeRecord openTimeRecord = new TimeRecord();
        openTimeRecord.setWorkMode(WorkMode.AVAILABLE);
        openTimeRecord.setUser(user);
        openTimeRecord.setStartTime(LocalDate.now().minusDays(1).atTime(9, 0));
        openTimeRecord = timeRecordRepository.save(openTimeRecord);

        timeRecordService.closeOpenTimeRecords();

        openTimeRecord = timeRecordRepository.findById(openTimeRecord.getId()).orElseThrow();
        assertNotNull(openTimeRecord.getEndTime(), "Open time record should have an end time after closeOpenTimeRecords() is called.");
        assertEquals(LocalDate.now().minusDays(1).atTime(23, 59, 59), openTimeRecord.getEndTime(), "End time should be set to the end of the previous day.");

        TimeRecord newTimeRecord = timeRecordRepository.findFirstByUserAndEndTimeIsNull(user);
        assertNotNull(newTimeRecord, "A new time record should be created for the new day.");
        assertEquals(LocalDate.now().atStartOfDay(), newTimeRecord.getStartTime(), "New time record should start at the beginning of the new day.");
    }

    @Test
    @Transactional
    @WithMockUser(username = "user", authorities = {"EMPLOYEE"})
    void testGetCurrentWorkModeOfUser() {
        Userx user = userxRepository.findFirstByUsername("user");

        TimeRecord openTimeRecord = new TimeRecord();
        openTimeRecord.setWorkMode(WorkMode.DEEP_WORK);
        openTimeRecord.setUser(user);
        openTimeRecord.setStartTime(LocalDateTime.now().minusHours(1));
        timeRecordRepository.save(openTimeRecord);

        WorkMode currentWorkMode = timeRecordService.getCurrentWorkModeOfUser(user);
        assertEquals(WorkMode.DEEP_WORK, currentWorkMode, "Current work mode should be DEEP_WORK.");

        openTimeRecord.setEndTime(LocalDateTime.now());
        timeRecordRepository.save(openTimeRecord);

        currentWorkMode = timeRecordService.getCurrentWorkModeOfUser(user);
        assertEquals(WorkMode.OUT_OF_OFFICE, currentWorkMode, "Current work mode should be OUT_OF_OFFICE if no open time record exists.");
    }

    @Test
    @Transactional
    @WithMockUser(username = "user", authorities = {"EMPLOYEE"})
    void testGetTodaysTimeRecords() {
        Userx user = userxRepository.findFirstByUsername("user");

        TimeRecord timeRecord1 = new TimeRecord();
        timeRecord1.setWorkMode(WorkMode.AVAILABLE);
        timeRecord1.setUser(user);
        timeRecord1.setStartTime(LocalDate.now().atTime(9, 0));
        timeRecordRepository.save(timeRecord1);

        TimeRecord timeRecord2 = new TimeRecord();
        timeRecord2.setWorkMode(WorkMode.DEEP_WORK);
        timeRecord2.setUser(user);
        timeRecord2.setStartTime(LocalDate.now().atTime(11, 0));
        timeRecordRepository.save(timeRecord2);

        List<TimeRecord> todaysTimeRecords = timeRecordService.getTodaysTimeRecords(user);
        assertEquals(2, todaysTimeRecords.size(), "There should be 2 time records for today.");
        assertTrue(todaysTimeRecords.contains(timeRecord1), "Time record 1 should be included in today's time records.");
        assertTrue(todaysTimeRecords.contains(timeRecord2), "Time record 2 should be included in today's time records.");
    }

}
