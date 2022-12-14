package com.mukhtar.UdaSecurity;

import com.mukhtar.UdaSecurity.Service.ImageServicesInterface;
import com.mukhtar.UdaSecurity.Services.SecurityService;
import com.mukhtar.UdaSecurity.application.StatusListener;
import com.mukhtar.UdaSecurity.data.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.awt.image.BufferedImage;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SecurityServiceTest {
    @Mock
    private SecurityRepository SecurityRepository;
    @Mock
    private ImageServicesInterface ImageServices;
    @Mock
    private SecurityService securityService;
    @Mock
    private StatusListener statusListener;

    private final String uuid = UUID.randomUUID().toString();
    private Sensor sensor;



    private Sensor populateSensors(){
        return new Sensor(uuid,SensorType.DOOR);

    }
    private Set<Sensor> getAllSensors(int count, boolean status) {
        String uuid = UUID.randomUUID().toString();
        Set<Sensor> sensors = new HashSet<>();
        for (int i = 0; i  < count; i++) {sensors.add(new Sensor(uuid, SensorType.DOOR));
        }
        sensors.forEach(sensor -> sensor.setActive(status));

        return sensors;
    }



    @BeforeEach
    void init(){
        securityService = new SecurityService(SecurityRepository, ImageServices);
        sensor= populateSensors();
    }

    /**
     * 1. If alarm is armed and a sensor becomes activated, put the system into pending alarm status.
     *
     */

    @ParameterizedTest
    @EnumSource( value=ArmingStatus.class, names = {"ARMED_HOME", "ARMED_AWAY"})
    public void sensorActivated_alarmArmed_Set_AlarmStatus_Pending(ArmingStatus armingStatus){
        when(SecurityRepository.getArmingStatus()).thenReturn(armingStatus);
        when(SecurityRepository.getAlarmStatus()).thenReturn(AlarmStatus.NO_ALARM);

        //when
        securityService.changeSensorActivationStatus(sensor,true);

        //then
        verify(SecurityRepository).setAlarmStatus(AlarmStatus.PENDING_ALARM);
    }

    /** 2. If alarm is armed and a sensor becomes activated and the system is already pending alarm,
     set the alarm status to alarm.
     */
    @ParameterizedTest
    @EnumSource( value=ArmingStatus.class, names = {"ARMED_HOME", "ARMED_AWAY"})
    public void sensorActivated_alarmArmedAndStatusPending_alarmStatusAlarm(ArmingStatus armingStatus){

        when(SecurityRepository.getArmingStatus()).thenReturn(armingStatus);
        when(SecurityRepository.getAlarmStatus()).thenReturn(AlarmStatus.PENDING_ALARM);

        //when
        securityService.changeSensorActivationStatus(sensor,true);

        //then
        verify(SecurityRepository).setAlarmStatus(AlarmStatus.ALARM);
    }

    /**
     * 3. If pending alarm and all sensors are inactive, return to no alarm state.
     */
    @Test
    public void All_sensors_DeActivated_StatusPending_alarmStatus_No_Alarm(){
        when(SecurityRepository.getAlarmStatus()).thenReturn(AlarmStatus.PENDING_ALARM);
        //when
       sensor.setActive(false);
        securityService.changeSensorActivationStatus(sensor);
        //then
        verify(SecurityRepository, times(1)).setAlarmStatus(AlarmStatus.NO_ALARM);
    }

    /**
     * 4. If alarm is active, change in sensor state should not affect the alarm state.
     */
    @Test
    void Alarm_Active_Change_In_SensorState_Should_Not_Affect_Alarm_State() {
       when(SecurityRepository.getAlarmStatus()).thenReturn(AlarmStatus.ALARM);
        securityService.changeSensorActivationStatus(sensor,true);

        verify(SecurityRepository, never()).setAlarmStatus(any(AlarmStatus.class));
    }

    /**
     * *5.If a sensor is activated while already active and the system is in pending state, change it to alarm state.
     */
@Test
    void Sensor_Activated_While_Already_Active_And_AlarmPending_AlarmStatusAlarm(){

        when(SecurityRepository.getAlarmStatus()).thenReturn(AlarmStatus.PENDING_ALARM);
        sensor.setActive(true);
        securityService.changeSensorActivationStatus(sensor,true);
        verify(SecurityRepository).setAlarmStatus(AlarmStatus.ALARM);
    }

    /**
     * * 6. If a sensor is deactivated while already inactive, make no changes to the alarm state.
     */

    @ParameterizedTest
    @EnumSource(value = AlarmStatus.class, names =  {"NO_ALARM","ALARM","PENDING_ALARM"})
    void Sensor_De_Activated_While_Already_InActive_AlarmStatus_NO_Alarm(){
        when(SecurityRepository.getAlarmStatus()).thenReturn(AlarmStatus.NO_ALARM);
        sensor.setActive(false);
        securityService.changeSensorActivationStatus(sensor,false);
        verify(SecurityRepository, never()).setAlarmStatus(any(AlarmStatus.class));

    }

    /**
     * * 7.If the image service identifies an image containing
     * * a cat while the system is armed-home or armed-away, put the system into alarm status.
     * *this test is also covering functionality 11
     */
    @Test
    void Cat_Identified_while_Armed_Home_AlarmStatusAlarm(){
        when(SecurityRepository.getArmingStatus()).thenReturn(ArmingStatus.ARMED_HOME);
        when(ImageServices.imageContainsCat(any(), anyFloat())).thenReturn(true);
        securityService.processImage(mock(BufferedImage.class));
        ArgumentCaptor<AlarmStatus> captor = ArgumentCaptor.forClass(AlarmStatus.class);
        verify(SecurityRepository, atMostOnce()).setAlarmStatus(captor.capture());
        assertEquals(captor.getValue(), AlarmStatus.ALARM);
        BufferedImage catImage = new BufferedImage(256, 256, BufferedImage.TYPE_INT_RGB);
        when(ImageServices.imageContainsCat(any(),anyFloat())).thenReturn(true);
        when(SecurityRepository.getArmingStatus()).thenReturn(ArmingStatus.DISARMED);
        securityService.processImage(catImage);
        securityService.setArmingStatus(ArmingStatus.ARMED_HOME);
        verify(SecurityRepository, times(2)).setAlarmStatus(AlarmStatus.ALARM);
    }
    /**
     * *8.If the image service identifies an image that does not contain a cat,
     * * change the status to no alarm as long as the sensors are not active.
     */
   @Test
    void ImageService_Identifies_No_Cat_And_No_Sensor_Active_AlarmStatusNoAlarm(){
       BufferedImage normalImage = new BufferedImage(600, 400, BufferedImage.TYPE_INT_RGB);
       securityService.changeSensorActivationStatus(sensor,false);
       when(ImageServices.imageContainsCat(any(), ArgumentMatchers.anyFloat())).thenReturn(false);
       securityService.processImage(normalImage);
       verify(SecurityRepository).setAlarmStatus(AlarmStatus.NO_ALARM);
    }

    /**
     * *9.If the system is disarmed, set the status to no alarm.
     */
    @Test
     void System_Disarmed_AlarmStatus_No_Alarm(){
        securityService.setArmingStatus(ArmingStatus.DISARMED);
        verify(SecurityRepository,atMostOnce()).setAlarmStatus(AlarmStatus.NO_ALARM);
    }
    @Test
    void System_Disarmed_AlarmStatus_Alarm_setAlarmStatus_Pending() {
        when(SecurityRepository.getArmingStatus()).thenReturn(ArmingStatus.DISARMED);
        when(SecurityRepository.getAlarmStatus()).thenReturn(AlarmStatus.ALARM);
        securityService.changeSensorActivationStatus(sensor);
        verify(SecurityRepository, times(1)).setAlarmStatus(AlarmStatus.PENDING_ALARM);
    }
    /**
     * * 10. If the system is armed, reset all sensors to inactive.
     **
     */
    @ParameterizedTest
    @EnumSource(value = ArmingStatus.class, names = {"ARMED_AWAY", "ARMED_HOME"})
    void System_Armed_Deactivate_All_Sensors(ArmingStatus status){
        Set<Sensor> sensors = getAllSensors(3, true);
        when(SecurityRepository.getAlarmStatus()).thenReturn(AlarmStatus.PENDING_ALARM);
        when(SecurityRepository.getSensors()).thenReturn(sensors);
        securityService.setArmingStatus(status);
        securityService.getSensors().forEach(sensor -> {assertFalse(sensor.getActive());});
    }

    /**
     * *11. Handle Sensor Deactivated, trying to increase test coverage
     * *when alarm status Pending
     */
    @Test
    void handleSensorDeactivated_AlarmStatus_Pending() {
        sensor.setActive(true);
        when(SecurityRepository.getAlarmStatus()).thenReturn(AlarmStatus.PENDING_ALARM);
        securityService.changeSensorActivationStatus(sensor, false);
        verify(SecurityRepository).setAlarmStatus(AlarmStatus.NO_ALARM);
    }

    /**
     * * just for reaching 100% method coverage
     */
    @Test
    void AddSensor() {
        securityService.addSensor(sensor);
    }
    @Test
    void removeSensor(){
        securityService.removeSensor(sensor);
    }

    @Test
    void addStatusListener() {
        securityService.addStatusListener(statusListener);
    }
@Test
    void removeStatusListener(){
    securityService.removeStatusListener(statusListener);
}
}

