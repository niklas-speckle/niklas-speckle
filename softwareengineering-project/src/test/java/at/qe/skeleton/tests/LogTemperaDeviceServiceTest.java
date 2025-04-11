package at.qe.skeleton.tests;

import at.qe.skeleton.model.AccessPoint;
import at.qe.skeleton.model.LogTemperaDevice;
import at.qe.skeleton.services.AccessPointService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.ArrayList;

@SpringBootTest
public class LogTemperaDeviceServiceTest {
    @Autowired
    private AccessPointService accessPointService;

    @Test
    public void testDeleteLogTemperaDeviceSuccess() {
        // Given
        AccessPoint accessPoint = new AccessPoint();
        accessPoint.setLogTemperaDevices(new ArrayList<>());
        LogTemperaDevice logTemperaDevice = new LogTemperaDevice();
        accessPoint.getLogTemperaDevices().add(logTemperaDevice);

        // When
        accessPointService.resetLogTemperaDevice(accessPoint);

        // Then
        Assertions.assertTrue(accessPoint.getLogTemperaDevices().isEmpty());
    }

    @Test
    public void testDeleteLogTemperaDeviceLogIsNull() {
        // Given
        AccessPoint accessPoint = new AccessPoint();

        // When/Then
        Assertions.assertThrows(NullPointerException.class, () -> {
            accessPointService.resetLogTemperaDevice(accessPoint);
        });
    }

    @Test
    public void testDeleteLogTemperaDeviceLogIsEmpty() {
        // Given
        AccessPoint accessPoint = new AccessPoint();
        accessPoint.setLogTemperaDevices(new ArrayList<>());

        // When/Then
        Assertions.assertThrows(IndexOutOfBoundsException.class, () -> {
            accessPointService.resetLogTemperaDevice(accessPoint);
        });
    }

    @Test
    public void testDeleteLogTemperaDeviceAccessPointIsNull() {
        // When/Then
        Assertions.assertThrows(NullPointerException.class, () -> {
            accessPointService.resetLogTemperaDevice(null);
        });
    }
}
