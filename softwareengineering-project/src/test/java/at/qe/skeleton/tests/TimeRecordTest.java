package at.qe.skeleton.tests;

import at.qe.skeleton.model.*;
import at.qe.skeleton.rest.dto.TimeRecordDTO;
import at.qe.skeleton.rest.mapper.TimeRecordMapper;
import at.qe.skeleton.services.TemperaDeviceService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.when;

class TimeRecordTest {

    @Mock
    private TemperaDeviceService temperaDeviceService;

    @InjectMocks
    private TimeRecordMapper timeRecordMapper;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void testMapFromWithNullDTOShouldReturnNull() {
        assertNull(timeRecordMapper.mapFrom(null));
    }

    @Test
    public void testMapFromWithValidDTOShouldMapCorrectly() {
        TimeRecordDTO dto = new TimeRecordDTO(1L, LocalDateTime.parse("2024-04-05T15:30:00"), 1L,"AVAILABLE");

        Project project = new Project();
        project.setId(1L);

        Userx user1 = new Userx();
        user1.setUsername("user1");
        user1.setDefaultProject(project);

        TimeRecord expected = new TimeRecord();
        expected.setStartTime(LocalDateTime.parse("2024-04-05T15:30:00"));
        expected.setWorkMode(WorkMode.fromString("AVAILABLE"));
        expected.setUser(user1);

        when(temperaDeviceService.findUserOfTemperaDevice(1L)).thenReturn(user1);

        TimeRecord actual = timeRecordMapper.mapFrom(dto);

        assertEquals(expected.getStartTime(), actual.getStartTime());
        assertEquals(expected.getWorkMode(), actual.getWorkMode());
        assertEquals(expected.getUser(), actual.getUser());
        assertEquals(expected.getUser().getDefaultProject(), actual.getProject());


    }


}