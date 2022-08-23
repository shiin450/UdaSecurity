package com.mukhtar.UdaSecurity.Services;

import com.mukhtar.UdaSecurity.Service.ImageServicesInterface;
import com.mukhtar.UdaSecurity.application.StatusListener;
import com.mukhtar.UdaSecurity.data.*;

import java.awt.image.BufferedImage;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.stream.Collectors;

import static com.mukhtar.UdaSecurity.data.AlarmStatus.ALARM;
import static com.mukhtar.UdaSecurity.data.AlarmStatus.NO_ALARM;

/**
 * Service that receives information about changes to the security system. Responsible for
 * forwarding updates to the repository and making any decisions about changing the system state.
 *
 * This is the class that should contain most of the business logic for our system, and it is the
 * class you will be writing unit tests for.
 */
public class SecurityService {

    private final ImageServicesInterface imageService;
    private final  SecurityRepository securityRepository;
    private Boolean catDetection = false;
    private final Set<StatusListener> statusListeners = new HashSet<>();

    public SecurityService(SecurityRepository securityRepository, ImageServicesInterface imageService) {
        this.securityRepository = securityRepository;
        this.imageService = imageService;
    }

    /**
     * Sets the current arming status for the system. Changing the arming status
     * may update both the alarm status.
     */
    public void setArmingStatus(ArmingStatus armingStatus) {
            if (catDetection && armingStatus == ArmingStatus.ARMED_HOME){
                setAlarmStatus(ALARM);
            }
            if ( armingStatus == ArmingStatus.DISARMED){
                setAlarmStatus(NO_ALARM);
            }
            else {
                ConcurrentSkipListSet<Sensor> sensors = new ConcurrentSkipListSet<>(getSensors());
                sensors.forEach(sensor -> changeSensorActivationStatus(sensor,false));
            }

        securityRepository.setArmingStatus(armingStatus);
        statusListeners.forEach(StatusListener::sensorStatusChanged);
    }

    /**
     * Internal method that handles alarm status changes based on whether
     * the camera currently shows a cat.
     * @param cat True if a cat is detected, otherwise false.
     */
    private void catDetected(Boolean cat) {
        catDetection =cat;
        if(cat && getArmingStatus() == ArmingStatus.ARMED_HOME) {
            setAlarmStatus(ALARM);
        }
        else if(!cat && allActiveSensors()){
            setAlarmStatus(NO_ALARM);
        }
     //case ArmingStatus == Disarmed and cat is Detected - no alarm

        statusListeners.forEach(sl -> sl.catDetected(cat));
    }

    /**
     * Register the StatusListener for alarm system updates from within the SecurityService.
     * @param statusListener
     */
    public void addStatusListener(StatusListener statusListener) {
        statusListeners.add(statusListener);
    }

    public void removeStatusListener(StatusListener statusListener) {
        statusListeners.remove(statusListener);
    }

    /**
     * Change the alarm status of the system and notify all listeners.
     * @param status
     */
    public void setAlarmStatus(AlarmStatus status) {
        securityRepository.setAlarmStatus(status);
        statusListeners.forEach(sl -> sl.notify(status));
    }

    /**
     * Internal method for updating the alarm status when a sensor has been activated.
     */
    private void handleSensorActivated() {
        if(securityRepository.getArmingStatus() == ArmingStatus.DISARMED) {
            return; //no problem if the system is disarmed
        }
        switch(securityRepository.getAlarmStatus()) {
            case NO_ALARM -> setAlarmStatus(AlarmStatus.PENDING_ALARM);
            case PENDING_ALARM -> setAlarmStatus(ALARM);
        }
    }

    /**
     * Internal method for updating the alarm status when a sensor has been deactivated
     */
    private void handleSensorDeactivated() {
        switch(securityRepository.getAlarmStatus()) {
            case PENDING_ALARM -> setAlarmStatus(AlarmStatus.NO_ALARM);
            case ALARM -> setAlarmStatus(AlarmStatus.PENDING_ALARM);
        }
    }

    /**
     * Change the activation status for the specified sensor and update alarm status if necessary.
     * @param sensor
     * @param active
     */
    public void changeSensorActivationStatus(Sensor sensor, Boolean active) {
        AlarmStatus currentAlarmStatus = securityRepository.getAlarmStatus();
        if (currentAlarmStatus != ALARM){
            if(active) {
                handleSensorActivated();
            } else if (sensor.getActive()) {
                handleSensorDeactivated();
            }

        }
        sensor.setActive(active);
        securityRepository.updateSensor(sensor);
    }
    /**
     * Change the activation status for the specified sensor if no active status is passed
     * * and update alarm status if necessary.
     * @param sensor
     *
     */
    public void changeSensorActivationStatus(Sensor sensor) {
        AlarmStatus currentAlarmStatus = securityRepository.getAlarmStatus();
        ArmingStatus currentArmingStatus= securityRepository.getArmingStatus();
        if (currentAlarmStatus == AlarmStatus.PENDING_ALARM && !sensor.getActive()){
            handleSensorDeactivated();
        }
        else if (currentAlarmStatus == ALARM && currentArmingStatus== ArmingStatus.DISARMED){
            handleSensorDeactivated();
        }


        securityRepository.updateSensor(sensor);
    }

    /**
     * Send an image to the SecurityService for processing. The securityService will use it's provided
     * ImageService to analyze the image for cats and update the alarm status accordingly.
     * @param currentCameraImage
     */
    public void processImage(BufferedImage currentCameraImage) {
        catDetected(imageService.imageContainsCat(currentCameraImage, 50.0f));
    }

    public AlarmStatus getAlarmStatus() {
        return securityRepository.getAlarmStatus();}

    public Set<Sensor> getSensors() {
        return securityRepository.getSensors();
    }

    public void addSensor(Sensor sensor) {
        securityRepository.addSensor(sensor);
    }

    public void removeSensor(Sensor sensor) {
        securityRepository.removeSensor(sensor);
    }

    public ArmingStatus getArmingStatus() {
        return securityRepository.getArmingStatus();
    }

    public boolean allActiveSensors(){
        Set<Sensor> activeSensors = getSensors().stream().
                filter(sensor -> sensor.getActive()==true).
                collect(Collectors.toSet());
        return activeSensors.isEmpty();
    }

}
