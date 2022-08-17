package com.mukhtar.UdaSecurity.data;

import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;

/**
 * Purely written for Testing purposes, to avoid testing our Database
 */
public class FakeDatabaseSecurityRepository implements SecurityRepository {

    private Set<Sensor> sensors = new TreeSet<>();

    private AlarmStatus alarmStatus;
    private ArmingStatus armingStatus;

    public FakeDatabaseSecurityRepository() {
        this.alarmStatus = AlarmStatus.NO_ALARM;
        this.armingStatus = ArmingStatus.DISARMED;
        sensors.addAll(List.of(new Sensor("Window Sensor", SensorType.WINDOW),
                new Sensor("Door Sensor", SensorType.DOOR)));
    }

    @Override
    public void addSensor(Sensor sensor) {
        Objects.requireNonNull(sensor);
        sensors.add(sensor);

    }

    @Override
    public void removeSensor(Sensor sensor) {
        Objects.requireNonNull(sensor);
        sensors.remove(sensor);
    }

    @Override
    public void updateSensor(Sensor sensor) {
        Objects.requireNonNull(sensor);
        sensors.remove(sensor);
        sensors.add(sensor);
    }

    @Override
    public void setAlarmStatus(AlarmStatus alarmStatus) {
        Objects.requireNonNull(alarmStatus);
        this.alarmStatus=alarmStatus;
    }

    @Override
    public void setArmingStatus(ArmingStatus armingStatus) {
        Objects.requireNonNull(armingStatus);
        this.armingStatus = armingStatus;
    }

    @Override
    public Set<Sensor> getSensors() {
        return this.sensors;
    }

    @Override
    public AlarmStatus getAlarmStatus() {
        return this.alarmStatus;
    }

    @Override
    public ArmingStatus getArmingStatus() {
        return this.armingStatus;
    }
}
