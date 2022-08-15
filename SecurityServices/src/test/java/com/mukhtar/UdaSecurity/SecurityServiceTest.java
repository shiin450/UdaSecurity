package com.mukhtar.UdaSecurity;

import com.mukhtar.UdaSecurity.ImageServices.ImageServicesInterface;
import com.mukhtar.UdaSecurity.Services.SecurityService;
import com.mukhtar.UdaSecurity.data.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.HashSet;
import java.util.Set;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SecurityServiceTest {
@Mock
private SecurityRepository SecurityRepository;
@Mock
private ImageServicesInterface ImageServices;


private Set<Sensor> sensors = new HashSet<>();
private ArmingStatus armingStatus;
private AlarmStatus alarmStatus;
private com.mukhtar.UdaSecurity.Services.SecurityService SecurityService;

@BeforeEach
void init(){
    SecurityService = new SecurityService(SecurityRepository, ImageServices);
}

    @ParameterizedTest
    @EnumSource( value=ArmingStatus.class, names = {"ARMED_HOME", "ARMED_AWAY"})
public void sensorActivated_alarmArmedAndStatusPending_alarmStatusAlarm(ArmingStatus armingStatus){

    //Given
        sensors.add(new Sensor("TestDoor", SensorType.DOOR,false));
        sensors.add(new Sensor("TestWindow", SensorType.WINDOW,false));
        sensors.add(new Sensor("TestMotion", SensorType.MOTION,false));

       when(SecurityRepository.getArmingStatus()).thenReturn(armingStatus);
       when(SecurityRepository.getAlarmStatus()).thenReturn(AlarmStatus.NO_ALARM);

       //when
        sensors.forEach(sensor -> SecurityService.changeSensorActivationStatus(sensor,true));

        //then
        verify(SecurityRepository).setAlarmStatus(AlarmStatus.PENDING_ALARM);
}


}