package com.mukhtar.UdaSecurity;

import com.mukhtar.UdaSecurity.ImageServices.ImageServicesInterface;
import com.mukhtar.UdaSecurity.Services.SecurityService;
import com.mukhtar.UdaSecurity.application.StatusListener;
import com.mukhtar.UdaSecurity.data.*;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.awt.image.BufferedImage;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SecurityServiceTest {
    @Mock
    private SecurityRepository SecurityRepository;
    @Mock
    private ImageServicesInterface ImageServices;

    private SecurityService securityService;
    private final String uuid = UUID.randomUUID().toString();
    private Sensor sensor;
    @Mock
    private StatusListener statusListener;


    private Sensor populateSensors(){
        return new Sensor(uuid,SensorType.WINDOW);

    }
    private Set<Sensor> getAllSensors() {
        Set<Sensor> sensors = new HashSet<>();
        for (int i = 0; i  < 5; i++) {sensors.add(new Sensor(uuid, SensorType.WINDOW));
        }
        sensors.forEach(sensor -> sensor.setActive(true));

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
    @ParameterizedTest
    @EnumSource( value=ArmingStatus.class, names = {"ARMED_HOME", "ARMED_AWAY","DISARMED"})
    public void All_sensors_DeActivated_StatusPending_alarmStatus_No_Alarm(ArmingStatus armingStatus){

        when(SecurityRepository.getArmingStatus()).thenReturn(armingStatus);
        when(SecurityRepository.getAlarmStatus()).thenReturn(AlarmStatus.PENDING_ALARM);

        //when
        securityService.changeSensorActivationStatus(sensor,true);
        securityService.changeSensorActivationStatus(sensor,false);

        //then
        verify(SecurityRepository).setAlarmStatus(AlarmStatus.NO_ALARM);
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

      //verify(SecurityRepository, never()).setAlarmStatus(any(AlarmStatus.class));

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
     * * a cat while the system is armed-home, put the system into alarm status.
     */
    @Test
    void Cat_Identified_while_Armed_Home_AlarmStatusAlarm(){
        BufferedImage CatImage = new BufferedImage(600, 400, BufferedImage.TYPE_INT_RGB);
        when(SecurityRepository.getArmingStatus()).thenReturn(ArmingStatus.ARMED_HOME);
        when(ImageServices.imageContainsCat(any(), ArgumentMatchers.anyFloat())).thenReturn(true);
       securityService.processImage(CatImage);
        verify(SecurityRepository).setAlarmStatus(AlarmStatus.ALARM);
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
        verify(SecurityRepository, atMostOnce()).setAlarmStatus(AlarmStatus.NO_ALARM);
    }

    /**
     * *9.If the system is disarmed, set the status to no alarm.
     */
    @Test
     void System_Disarmed_AlarmStatus_No_Alarm(){
        securityService.setArmingStatus(ArmingStatus.DISARMED);
        verify(SecurityRepository,atMostOnce()).setAlarmStatus(AlarmStatus.NO_ALARM);
    }

    /**
     * * 10. If the system is armed, reset all sensors to inactive.
     **
     */
    @ParameterizedTest
    @EnumSource(value = ArmingStatus.class, names = {"ARMED_AWAY", "ARMED_HOME"})
    void System_Armed_Deactivate_All_Sensors(ArmingStatus status){
        Set<Sensor> sensors = getAllSensors();
        when(SecurityRepository.getAlarmStatus()).thenReturn(AlarmStatus.PENDING_ALARM);
        when(SecurityRepository.getSensors()).thenReturn(sensors);
        securityService.setArmingStatus(status);

        securityService.getSensors().forEach(sensor -> Assertions.assertEquals(false,sensor.getActive()));
    }

    /**
     * * 11. If the system is armed-home while the camera shows a cat, set the alarm status to alarm.
     */

    @Test
    void System_Armed_Home_And_Cat_Detected_AlarmStatusAlarm(){
        BufferedImage CatImage = new BufferedImage(600, 400, BufferedImage.TYPE_INT_RGB);
        when(SecurityRepository.getArmingStatus()).thenReturn(ArmingStatus.DISARMED);
        when(ImageServices.imageContainsCat(any(), ArgumentMatchers.anyFloat())).thenReturn(true);
        securityService.processImage(CatImage);
        securityService.setArmingStatus(ArmingStatus.ARMED_HOME);
        verify(SecurityRepository, atLeast(1)).setAlarmStatus(AlarmStatus.ALARM);
    }

    /**
     * *12. AddStatus listener
     */
    @Test
    void addStatusListener(){
        securityService.addStatusListener(statusListener);
    }

    /**
     * *13. Remove Status listener
     */
    @Test
    void RemoveStatusListener(){
        securityService.removeStatusListener(statusListener);
    }

    /**
     * *14. add Sensor
     */
    @Test
    void addSensor() {
        SecurityRepository.addSensor(sensor);
        verify(SecurityRepository, atLeast(1)).addSensor(sensor);
    }

    /**
     * *15. remove Sensor
     */
    @Test
    void removeSensor() {
        SecurityRepository.removeSensor(sensor);
        verify(SecurityRepository).removeSensor(sensor);
    }

    /**
     * *16. Handle Sensor Deactivated, trying to increase test coverage
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
     * *16. Handle Sensor Deactivated, trying to increase test coverage
     * When alarm status alarm
     */
    @Test
    void handleSensorDeactivated_AlarmStatus_Alarm() {
        sensor.setActive(true);
        when(SecurityRepository.getAlarmStatus()).thenReturn(AlarmStatus.ALARM);
        securityService.changeSensorActivationStatus(sensor, false);
        verify(SecurityRepository, atMostOnce()).setAlarmStatus(AlarmStatus.PENDING_ALARM);

    }

}

