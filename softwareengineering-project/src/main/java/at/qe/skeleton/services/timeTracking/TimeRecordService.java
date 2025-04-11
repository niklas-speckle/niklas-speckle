package at.qe.skeleton.services.timeTracking;

import at.qe.skeleton.model.*;
import at.qe.skeleton.repositories.TimeRecordRepository;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Service for accessing and manipulating TimeRecords.
 */
@Component
@Scope("application")
public class TimeRecordService {

    @Autowired
    TimeRecordRepository timeRecordRepository;

    @Autowired
    WorkGroupService workGroupService;

    @Autowired
    ProjectService projectService;

    /**
     * Loads a TimeRecord by its ID.
     *
     * @param timeRecord The TimeRecord to be loaded.
     * @return The loaded TimeRecord.
     * @throws IllegalArgumentException if the TimeRecord is not found.
     */
    public TimeRecord loadTimeRecord(TimeRecord timeRecord) {
        return timeRecordRepository.findById(timeRecord.getId()).orElseThrow(() -> new IllegalArgumentException("Time record not found for id: " + timeRecord.getId()));
    }

    /**
     * Saves the given TimeRecord.
     *
     * @param timeRecord The TimeRecord to be saved.
     * @return The saved TimeRecord.
     */
    @Transactional
    public TimeRecord saveTimeRecord(TimeRecord timeRecord) {
        return timeRecordRepository.save(timeRecord);
    }

    /**
     * Gets all TimeRecords for a specific user.
     *
     * @param user The user whose TimeRecords are to be retrieved.
     * @return A list of TimeRecords for the user.
     */
    public List<TimeRecord> getTimeRecordsForUser(Userx user) {
        return timeRecordRepository.findAllByUser(user);
    }

    /**
     * Gets the current TimeRecord for a user, where the end time is null.
     *
     * @param user The user whose current TimeRecord is to be retrieved.
     * @return The current TimeRecord for the user.
     */
    public TimeRecord getCurrentTimeRecordForUser(Userx user) {
        return timeRecordRepository.findFirstByUserAndEndTimeIsNull(user);
    }

    /**
     * Edits the given TimeRecord.
     *
     * @param timeRecord The TimeRecord to be edited.
     * @return The edited TimeRecord.
     */
    @Transactional
    public TimeRecord editTimeRecord(TimeRecord timeRecord) {
        TimeRecord timeRecordToEdit = loadTimeRecord(timeRecord);
        timeRecordToEdit.setStartTime(timeRecord.getStartTime());
        timeRecordToEdit.setEndTime(timeRecord.getEndTime());
        timeRecordToEdit.setProject(timeRecord.getProject());
        timeRecordToEdit.setWorkGroup(timeRecord.getWorkGroup());
        timeRecordToEdit.setDescription(timeRecord.getDescription());
        timeRecordToEdit.setWorkMode(timeRecord.getWorkMode());
        timeRecordRepository.save(timeRecordToEdit);
        return timeRecordToEdit;
    }

    /**
     * Splits a given TimeRecord at the specified time.
     *
     * @param timeRecord The TimeRecord to be split.
     * @param splitAt    The LocalDateTime to split the TimeRecord at.
     * @return A list of the two new TimeRecords.
     */
    @Transactional
    public List<TimeRecord> splitTimeRecord(TimeRecord timeRecord, LocalDateTime splitAt) {
        TimeRecord timeRecordToEdit = loadTimeRecord(timeRecord);
        TimeRecord newTimeRecord = new TimeRecord();
        newTimeRecord.setUser(timeRecordToEdit.getUser());
        newTimeRecord.setStartTime(splitAt);
        newTimeRecord.setEndTime(timeRecordToEdit.getEndTime());
        newTimeRecord.setWorkMode(timeRecordToEdit.getWorkMode());
        timeRecordToEdit.setEndTime(splitAt);
        timeRecordRepository.save(newTimeRecord);
        timeRecordRepository.save(timeRecordToEdit);
        return List.of(timeRecordToEdit, newTimeRecord);
    }

    /**
     * Saves the given TimeRecord and closes any open TimeRecord for the same user.
     *
     * @param newTimeRecord The new TimeRecord to be saved.
     */
    @Transactional
    public void saveNewAndCloseOldTimeRecord(TimeRecord newTimeRecord) {
        TimeRecord oldTimeRecord = timeRecordRepository.findFirstByUserAndEndTimeIsNull(newTimeRecord.getUser());
        if (oldTimeRecord != null) {
            oldTimeRecord.setEndTime(newTimeRecord.getStartTime());
            saveTimeRecord(oldTimeRecord);
        }
        saveTimeRecord(newTimeRecord);
    }

    /**
     * Automatically closes all open TimeRecords at midnight and opens new ones for the new day.
     */
    @Scheduled(cron = "0 0 0 * * *")
    public void closeOpenTimeRecords() {
        List<TimeRecord> openTimeRecords = timeRecordRepository.findAllByEndTimeIsNull();
        for (TimeRecord oldTimeRecord : openTimeRecords) {
            oldTimeRecord.setEndTime(oldTimeRecord.getStartTime().toLocalDate().atTime(23, 59, 59));
            oldTimeRecord = timeRecordRepository.save(oldTimeRecord);
            TimeRecord newTimeRecord = new TimeRecord();
            newTimeRecord.setStartTime(LocalDate.now().atStartOfDay());
            newTimeRecord.setUser(oldTimeRecord.getUser());
            newTimeRecord.setWorkMode(oldTimeRecord.getWorkMode());
            newTimeRecord.setProject(oldTimeRecord.getProject());
            newTimeRecord.setWorkGroup(oldTimeRecord.getWorkGroup());
            newTimeRecord.setDescription(oldTimeRecord.getDescription());
            timeRecordRepository.save(newTimeRecord);
        }
    }

    /**
     * Gets all TimeRecords for the groups led by a Group Leader.
     *
     * @param groupLeader The Group Leader whose groups' TimeRecords are to be retrieved.
     * @return A list of TimeRecords for the Group Leader's groups, excluding OUT_OF_OFFICE records.
     */
    @PreAuthorize("hasAuthority('GROUP_LEADER')")
    public List<TimeRecord> getTimeRecordsForGroupLeader(Userx groupLeader) {
        List<TimeRecord> timeRecordsForGroupLeader = new ArrayList<>();
        List<WorkGroup> groupLeadersWorkGroups = workGroupService.getGroupsByGroupLeader(groupLeader);
        for (WorkGroup workGroup : groupLeadersWorkGroups) {
            timeRecordsForGroupLeader.addAll(timeRecordRepository.findAllByWorkGroup(workGroup));
        }
        return timeRecordsForGroupLeader.stream()
                .filter(timeRecord -> timeRecord.getWorkMode() != WorkMode.OUT_OF_OFFICE)
                .collect(Collectors.toList());
    }

    /**
     * Gets all TimeRecords for the projects managed by a Manager.
     *
     * @param manager The Manager whose projects' TimeRecords are to be retrieved.
     * @return A list of TimeRecords for the Manager's projects, excluding OUT_OF_OFFICE records.
     */
    @PreAuthorize("hasAuthority('MANAGER')")
    public List<TimeRecord> getTimeRecordsForManager(Userx manager) {
        List<TimeRecord> timeRecordsForManager = new ArrayList<>();
        List<Project> managersProjects = projectService.getProjectByManager(manager);
        for (Project project : managersProjects) {
            timeRecordsForManager.addAll(timeRecordRepository.findAllByProject(project));
        }
        return timeRecordsForManager.stream()
                .filter(timeRecord -> timeRecord.getWorkMode() != WorkMode.OUT_OF_OFFICE)
                .collect(Collectors.toList());
    }

    /**
     * Checks whether the selected TimeRecord's start time would be after its end time.
     *
     * @param selectedTimeRecord The TimeRecord to be checked.
     * @param newStartDateTime   The new start time to be set.
     * @param newEndDateTime     The new end time to be set.
     * @return true if the start time is after the end time, false otherwise.
     */
    public boolean isStartAfterEnd(TimeRecord selectedTimeRecord, LocalDateTime newStartDateTime, LocalDateTime newEndDateTime) {
        LocalDateTime selectedTrEndTime = selectedTimeRecord.getEndTime() != null ? selectedTimeRecord.getEndTime() : LocalDateTime.now();
        if (newEndDateTime == null) {
            newEndDateTime = LocalDateTime.now();
        }
        if (!newStartDateTime.equals(selectedTimeRecord.getStartTime()) && !newEndDateTime.equals(selectedTrEndTime)) {
            return !newStartDateTime.isBefore(newEndDateTime);
        } else {
            Boolean newStartAfterOldEnd = !newStartDateTime.isBefore(selectedTrEndTime);
            Boolean newEndBeforeOldStart = newEndDateTime != null && !newEndDateTime.isAfter(selectedTimeRecord.getStartTime());
            return newStartAfterOldEnd || newEndBeforeOldStart;
        }
    }

    /**
     * Detects if there is an overlap between the selected TimeRecord and another TimeRecord.
     *
     * @param selectedTimeRecord The selected TimeRecord.
     * @param otherTimeRecord    The other TimeRecord to check for overlap.
     * @param newStartDateTime   The new start time of the selected TimeRecord.
     * @param newEndDateTime     The new end time of the selected TimeRecord.
     * @return true if there is an overlap, false otherwise.
     */
    public boolean detectOverlap(TimeRecord selectedTimeRecord, TimeRecord otherTimeRecord, LocalDateTime newStartDateTime, LocalDateTime newEndDateTime) {
        if (newEndDateTime == null) {
            newEndDateTime = LocalDateTime.now();
        }
        if (!newStartDateTime.equals(selectedTimeRecord.getStartTime()) && isOverlapBefore(newStartDateTime, selectedTimeRecord, otherTimeRecord)) {
            return true;
        }
        return !newEndDateTime.equals(selectedTimeRecord.getEndTime()) && isOverlapAfter(newEndDateTime, selectedTimeRecord, otherTimeRecord);
    }

    private boolean isOverlapAfter(LocalDateTime newEndDateTime, TimeRecord selectedTimeRecord, TimeRecord otherTimeRecord) {
        LocalDateTime selectedTrEndTime = selectedTimeRecord.getEndTime() != null ? selectedTimeRecord.getEndTime() : LocalDateTime.now();
        if (!otherTimeRecord.getStartTime().isBefore(selectedTrEndTime)) {
            return newEndDateTime.isAfter(otherTimeRecord.getStartTime());
        } else {
            return false;
        }
    }

    private boolean isOverlapBefore(LocalDateTime newStartDateTime, TimeRecord selectedTimeRecord, TimeRecord otherTimeRecord) {
        LocalDateTime otherTrEndTime = otherTimeRecord.getEndTime() != null ? otherTimeRecord.getEndTime() : LocalDateTime.now();
        if (!otherTrEndTime.isAfter(selectedTimeRecord.getStartTime())) {
            return newStartDateTime.isBefore(otherTrEndTime);
        } else {
            return false;
        }
    }

    /**
     * Gets the current work mode of a user.
     *
     * @param user The user whose current work mode is to be retrieved.
     * @return The current work mode of the user.
     */
    public WorkMode getCurrentWorkModeOfUser(Userx user) {
        TimeRecord openTimeRecord = timeRecordRepository.findFirstByUserAndEndTimeIsNull(user);
        if (openTimeRecord != null) {
            return openTimeRecord.getWorkMode();
        } else {
            return WorkMode.OUT_OF_OFFICE;
        }
    }

    /**
     * Gets all TimeRecords for a user for the current day.
     *
     * @param currentUser The user whose today's TimeRecords are to be retrieved.
     * @return A list of today's TimeRecords for the user.
     */
    public List<TimeRecord> getTodaysTimeRecords(Userx currentUser) {
        return timeRecordRepository.findAllByUserAndStartTimeAfter(currentUser, LocalDate.now().atStartOfDay());
    }
}
