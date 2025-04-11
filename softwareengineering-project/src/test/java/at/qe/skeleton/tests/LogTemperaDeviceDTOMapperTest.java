package at.qe.skeleton.tests;

import at.qe.skeleton.model.*;
import at.qe.skeleton.rest.dto.LogTemperaDeviceDTO;
import at.qe.skeleton.rest.mapper.LogTemperaDeviceDTOMapper;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;


import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

//tests were created with help of chatGPT

class LogTemperaDeviceDTOMapperTest {
    private final LogTemperaDeviceDTOMapper mapper = new LogTemperaDeviceDTOMapper();

    @Test
    public void mapToWithNullEntityShouldReturnNullDTO() {
        LogTemperaDeviceDTO dto = mapper.mapTo(null);
        Assertions.assertNull(dto);
    }

    @Test
    public void mapToWithValidEntityShouldMapCorrectly() {
        LogTemperaDevice entity = new LogTemperaDevice();
        entity.setTimestamp(LocalDateTime.parse("2024-04-04T14:45"));
        entity.setLogStatus(LogStatus.UPDATED);
        entity.setTemperaDeviceId(1L);
        entity.setNewStatus(DeviceStatus.ENABLED);

        LogTemperaDeviceDTO dto = mapper.mapTo(entity);

        assertNotNull(dto);
        assertEquals(LocalDateTime.parse("2024-04-04T14:45"), dto.timestamp());
        assertEquals("UPDATED", dto.logStatus());
        assertEquals(1L, dto.temperaDeviceId());
        assertEquals("ENABLED", dto.newStatus());
    }






}