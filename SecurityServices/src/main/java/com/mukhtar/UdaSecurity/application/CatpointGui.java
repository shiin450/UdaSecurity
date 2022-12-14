package com.mukhtar.UdaSecurity.application;

import com.mukhtar.UdaSecurity.Service.AwsImageService;
import com.mukhtar.UdaSecurity.data.PretendDatabaseSecurityRepositoryImpl;
import com.mukhtar.UdaSecurity.data.SecurityRepository;
import net.miginfocom.swing.MigLayout;
import com.mukhtar.UdaSecurity.Services.SecurityService;

import javax.swing.*;

/**
 * This is the primary JFrame for the com.mukhtar.UdaSecurity.application that contains all the top-level JPanels.
 *
 * We're not using any dependency injection framework, so this class also handles constructing
 * all our dependencies and providing them to other classes as necessary.
 */
public class CatpointGui extends JFrame {
  private SecurityRepository securityRepository = new PretendDatabaseSecurityRepositoryImpl();
    private AwsImageService awsImageService = new AwsImageService();
    private SecurityService securityService = new SecurityService(securityRepository, awsImageService);
    private DisplayPanel displayPanel = new DisplayPanel(securityService);
    private ControlPanel controlPanel = new ControlPanel(securityService);
    private SensorPanel sensorPanel = new SensorPanel(securityService);
    private ImagePanel imagePanel = new ImagePanel(securityService);

    public CatpointGui() {
        setLocation(100, 100);
        setSize(600, 850);
        setTitle("Very Secure App");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        JPanel mainPanel = new JPanel();
        mainPanel.setLayout(new MigLayout());
        mainPanel.add(displayPanel, "wrap");
        mainPanel.add(imagePanel, "wrap");
        mainPanel.add(controlPanel, "wrap");
        mainPanel.add(sensorPanel);

        getContentPane().add(mainPanel);

    }
}
