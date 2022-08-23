module com.mukhtar.UdaSecurity.SecurityServices{

    requires com.mukhtar.UdaSecurity.ImageServices;
    requires com.miglayout.swing;
    requires java.desktop;
    requires com.google.common;
    requires com.google.gson;
    requires java.prefs;
    opens com.mukhtar.UdaSecurity.data to com.google.gson;
}