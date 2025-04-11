package at.qe.skeleton.ui.controllers;

import at.qe.skeleton.model.*;
import at.qe.skeleton.services.timeTracking.TimeRecordService;
import at.qe.skeleton.ui.beans.SessionInfoBean;
import lombok.Getter;
import lombok.Setter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

@Getter
@Setter
@Component
@Scope("view")
public class TimeRecordListController implements Serializable {

    @Autowired
    private transient TimeRecordService timeRecordService;

    @Autowired
    private SessionInfoBean sessionInfoBean;



    private List<TimeRecord> timeRecords;

    private List<TimeRecord> filteredTimeRecords;
    private Long totalDuration;



    public void initForEmployee() {
        timeRecords = timeRecordService.getTimeRecordsForUser(sessionInfoBean.getCurrentUser());
        if (totalDuration == null) {
            calculateTotalDuration(timeRecords);
        }
    }

    public void initForGroupLeader() {
        timeRecords = timeRecordService.getTimeRecordsForGroupLeader(sessionInfoBean.getCurrentUser());
        if (totalDuration == null) {
            calculateTotalDuration(timeRecords);
        }
    }

    public void initForManager() {
        timeRecords = timeRecordService.getTimeRecordsForManager(sessionInfoBean.getCurrentUser());
        if (totalDuration == null) {
            calculateTotalDuration(timeRecords);
        }
    }

    public boolean filterByDate(Object value, Object filter, Locale locale) {
        if (filter == null || filter.toString().isEmpty()) {
            return true;
        }
        if (value == null) {
            return false;
        }
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");
        try {
            LocalDateTime recordDate = (LocalDateTime) value;
            String recordDateString = recordDate.format(formatter);
            String filterString = filter.toString();
            return recordDateString.contains(filterString);
        } catch (Exception e) {
            return false; // If any error occurs
        }
    }

    public boolean filterByDateWithoutTime(Object value, Object filter, Locale locale) {
        if (filter == null || filter.toString().isEmpty()) {
            return true;
        }
        if (value == null) {
            return false;
        }

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy");
        try {
            LocalDate recordDate = (LocalDate) value;
            String recordDateString = recordDate.format(formatter);
            String filterString = filter.toString();
            return recordDateString.contains(filterString);
        } catch (Exception e) {
            return false; // If any error occurs
        }
    }

    public void calculateTotalDuration(List<TimeRecord> timeRecordList) {
        if ((timeRecordList == null) || timeRecordList.isEmpty()) {
            totalDuration = 0L;
        } else {
            totalDuration = timeRecordList.stream()
                    .mapToLong(TimeRecord::getDuration)
                    .sum();
        }
    }

    public void onFilter() {
        // Recalculate the total duration when the dataTable is filtered
        calculateTotalDuration(filteredTimeRecords);
    }
}




