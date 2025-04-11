package at.qe.skeleton.ui.controllers;

import at.qe.skeleton.model.*;
import at.qe.skeleton.services.timeTracking.TimeRecordService;
import at.qe.skeleton.services.timeTracking.ProjectService;
import at.qe.skeleton.services.timeTracking.WorkGroupService;
import at.qe.skeleton.ui.beans.SessionInfoBean;
import jakarta.annotation.PostConstruct;
import jakarta.faces.application.FacesMessage;
import jakarta.faces.context.FacesContext;
import lombok.Getter;
import lombok.Setter;
import org.primefaces.event.SelectEvent;
import org.primefaces.model.DefaultScheduleEvent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;


@Getter
@Setter
@Component
@Scope("view")
public class TimeRecordDetailController implements Serializable {

    @Autowired
    private transient TimeRecordService timeRecordService;
    @Autowired
    private transient ProjectService projectService;
    @Autowired
    private transient WorkGroupService workGroupService;
    @Autowired
    private SessionInfoBean sessionInfoBean;

    @Autowired
    TimeRecordScheduleController scheduleController;
    @Autowired
    TimeRecordChartController chartController;
    @Autowired
    TimeRecordListController listController;

    public static final String NO_PROJECT = "no project assigned";
    public static final String NO_WORK_GROUP = "no work group assigned";

    // Fields for storing selected time record (to be edited) and the values the user sets
    private TimeRecord selectedTimeRecord;

    private String selectedProject;
    private String selectedWorkGroup;
    private String selectedDescription;
    private WorkMode selectedWorkMode;
    private LocalTime selectedStartTime;
    private LocalTime selectedEndTime;
    private LocalTime splitAt;

    // Lists for storing selectable projects, work groups, and work modes
    private final List<WorkMode> workModes = List.of(WorkMode.values());
    private List<String> selectableProjectsAsString;
    private List<String> selectableWorkGroupsAsString;

    @PostConstruct
    public void init() {
        selectableProjectsAsString = initializeProjectsAsString();
    }

    /**
     * Returns a list of names of all projects the current user works on, including an option for no project assigned.
     *
     * @return A list of project names.
     */
    private List<String> initializeProjectsAsString() {
        List<String> strings = new ArrayList<>(List.of(NO_PROJECT));
        strings.addAll(projectService.getProjectsByUser(sessionInfoBean.getCurrentUser())
                .stream().map(Project::toString).toList());
        return strings;
    }

    /**
     * Returns a list of names of all selectable work groups for a given project,
     * including an option for no work group assigned.
     *
     * @param project The project for which selectable work groups are to be retrieved.
     * @return A list of work group names.
     */
    private List<String> initializeWorkGroupsAsString(Project project) {
        List<String> strings = new ArrayList<>(List.of(NO_WORK_GROUP));
        if (project != null) {
            strings.addAll(workGroupService.findSelectableWorkGroupsForProjectAndUser(project, sessionInfoBean.getCurrentUser())
                    .stream().map(WorkGroup::toString).toList());
        }
        return strings;
    }

    /**
     * Event handler triggered when a time record is selected in the schedule.
     * Prepares the selected time record for editing.
     *
     * @param selectEvent The select event containing the selected time record.
     */
    public void onEventSelect(SelectEvent<DefaultScheduleEvent<TimeRecord>> selectEvent) {
        DefaultScheduleEvent<TimeRecord> event = selectEvent.getObject();
        prepareForEdit(event.getData());
    }

    /**
     * Event handler triggered when a project is selected while editing a TimeRecord.
     * Updates selectable work groups based on the selected project.
     */
    public void onProjectSelect() {
        if (selectedProject.equals(NO_PROJECT)) { // if the record is not assigned to a project
            selectedWorkGroup = NO_WORK_GROUP;    // it cannot be assigned to a workGroup
        } else {
            selectableWorkGroupsAsString = initializeWorkGroupsAsString(projectService.getProjectByName(selectedProject).orElse(null));
            if (selectableWorkGroupsAsString.size() == 2) { // if the list only contains NO_WORK_GROUP and one work group, directly select the work group
                selectedWorkGroup = selectableWorkGroupsAsString.get(1);
            } else {
                selectedWorkGroup = NO_WORK_GROUP;
            }
        }
    }

    /**
     * Prepares a selected time record for editing by populating the fields with its details.
     * @param selectedTimeRecord The time record to be edited.
     */
    public void prepareForEdit(TimeRecord selectedTimeRecord) {
        this.selectedTimeRecord = selectedTimeRecord;
        selectedProject = selectedTimeRecord.getProject() == null ? NO_PROJECT : selectedTimeRecord.getProject().toString();
        selectedWorkGroup = selectedTimeRecord.getWorkGroup() == null ? NO_WORK_GROUP : selectedTimeRecord.getWorkGroup().toString();
        selectableWorkGroupsAsString = initializeWorkGroupsAsString(selectedTimeRecord.getProject());
        selectedDescription = selectedTimeRecord.getDescription();
        selectedWorkMode = selectedTimeRecord.getWorkMode();
        selectedStartTime = selectedTimeRecord.getStartTime().toLocalTime();
        if (selectedTimeRecord.getEndTime() != null) {
            selectedEndTime = selectedTimeRecord.getEndTime().toLocalTime();
        } else {
            selectedEndTime = null;
        }
        splitAt = selectedTimeRecord.getStartTime().toLocalTime();

        scheduleController.setInitialDate(selectedTimeRecord.getStartTime().toLocalDate());
    }


    /**
     * Edits the selected time record after performing validity checks.
     *
     * Checks if the work group is assigned to the project, if start time is before end time,
     * and if there are no overlaps with other time records.
     */
    public void editTimeRecord() {
        LocalDateTime newStartDateTime = selectedStartTime.atDate(selectedTimeRecord.getStartTime().toLocalDate());
        LocalDateTime newEndDateTime = selectedTimeRecord.getEndTime() != null ? selectedEndTime.atDate(selectedTimeRecord.getEndTime().toLocalDate()) : null;
        // check if work group works on project
        if (!(selectedProject.equals(NO_PROJECT) || selectedWorkGroup.equals(NO_WORK_GROUP))
                && !isWorkGroupAssignedToProject()) {
            reportErrorMessage("Work group must be working on project.");
            return;
        }
        // check if start is before end
        if (timeRecordService.isStartAfterEnd(selectedTimeRecord, newStartDateTime, newEndDateTime)) {
            reportErrorMessage("Start time must be before end time.");
            return;
        }
        // Check for overlaps
        if ((isStartTimeChanged() || isEndTimeChanged()) &&
                isOverlapDetected(selectedTimeRecord, newStartDateTime, newEndDateTime)) {
            reportErrorMessage("Time records must not overlap.");
            return;
        }
        updateFieldsOnSelectedTimeRecord();
        timeRecordService.editTimeRecord(selectedTimeRecord);
        updateOtherTimeRecordControllers();
        reportSuccessMessage("Time record has successfully been edited.");
    }


    /**
     * Splits the selected time record at the specified time.
     * Checks if the split time is within the start and end times of the record.
     */
    public void splitTimeRecord() {
        if (!(splitAt.isAfter(selectedStartTime) && splitAt.isBefore(selectedEndTime != null ? selectedEndTime : LocalTime.now()))) {
            reportErrorMessage("TimeRecord can only be split between start time and end time.");
        } else {
            timeRecordService.splitTimeRecord(selectedTimeRecord, splitAt.atDate(selectedTimeRecord.getStartTime().toLocalDate()));
            updateOtherTimeRecordControllers();
            reportSuccessMessage("Time record has successfully been split.");
        }
    }

    public TimeRecord getCurrentTimeRecord() {
        return timeRecordService.getCurrentTimeRecordForUser(sessionInfoBean.getCurrentUser());
    }

    // could be extracted to Project- or WorkGroupService (or possibly deleted, editDialog makes it impossible to adda wrong work group)
    private boolean isWorkGroupAssignedToProject() {
        WorkGroup workGroup = workGroupService.getGroupByName(selectedWorkGroup).orElse(null);
        Project project = projectService.getProjectByName(selectedProject).orElse(null);
        List<Project> projectsOfGroup = projectService.getProjectByWorkGroups(List.of(workGroup));
        return projectsOfGroup.contains(project);
    }

    /**
     * Checks if there is an overlap between the selected time record and other time records of the current user.
     *
     * @param selectedTimeRecord The selected time record.
     * @param newStartDateTime The new start date and time for the time record.
     * @param newEndDateTime The new end date and time for the time record.
     * @return True if there is an overlap, false otherwise.
     */
    private boolean isOverlapDetected(TimeRecord selectedTimeRecord, LocalDateTime newStartDateTime, LocalDateTime newEndDateTime) {
        List<TimeRecord> userTimeRecords = timeRecordService.getTimeRecordsForUser(sessionInfoBean.getCurrentUser());
        for (TimeRecord otherTimeRecord : userTimeRecords) {
            if (!otherTimeRecord.equals(selectedTimeRecord) &&
                    timeRecordService.detectOverlap(selectedTimeRecord, otherTimeRecord, newStartDateTime, newEndDateTime)) {
                return true;
            }
        }
        return false;
    }

    private boolean isStartTimeChanged() {
        return !selectedStartTime.equals(selectedTimeRecord.getStartTime().toLocalTime());
    }

    private boolean isEndTimeChanged() {
        if (selectedEndTime != null) {
            return !selectedEndTime.equals(selectedTimeRecord.getStartTime().toLocalTime());
        } else {
            // end time of open time record cannot be changed in UI
            return false;
        }
    }

    /**
     * Updates the fields of the selected time record with the edited values.
     */
    private void updateFieldsOnSelectedTimeRecord() {
        selectedTimeRecord.setProject(projectService.getProjectByName(selectedProject).orElse(null));
        selectedTimeRecord.setWorkGroup(workGroupService.getGroupByName(selectedWorkGroup).orElse(null));
        selectedTimeRecord.setDescription(selectedDescription);
        selectedTimeRecord.setWorkMode(selectedWorkMode);
        selectedTimeRecord.setStartTime(selectedStartTime.atDate(selectedTimeRecord.getStartTime().toLocalDate()));
        if (selectedTimeRecord.getEndTime() != null) {
            selectedTimeRecord.setEndTime(selectedEndTime.atDate(selectedTimeRecord.getEndTime().toLocalDate()));
        }
    }

    /**
     * Updates other time record controllers after a successful edit or split.
     */
    private void updateOtherTimeRecordControllers() {
        listController.initForEmployee();
        scheduleController.init();
        chartController.initForEmployee();
    }

    /**
     * Reports an error message to the user.
     *
     * @param message The error message to be reported.
     */
    private void reportErrorMessage(String message) {
        FacesContext.getCurrentInstance().addMessage("editDialog", new FacesMessage(FacesMessage.SEVERITY_ERROR, "Error", message));
    }


    /**
     * Reports a success message to the user.
     *
     * @param message The success message to be reported.
     */
    private void reportSuccessMessage(String message) {
        FacesContext.getCurrentInstance().addMessage("editDialog", new FacesMessage(FacesMessage.SEVERITY_INFO, "Success", message));
    }


    public String getTodaysTotalDuration() {
        long totalMinutes = timeRecordService.getTodaysTimeRecords(sessionInfoBean.getCurrentUser())
                .stream().mapToLong(TimeRecord::getDuration).sum();
        long hours = totalMinutes / 60;
        long minutes = totalMinutes % 60;
        return String.format("%02d:%02d", hours, minutes);
    }



}
