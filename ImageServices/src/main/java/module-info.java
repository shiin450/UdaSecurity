module com.mukhtar.UdaSecurity.ImageServices{
    requires org.slf4j;
    requires software.amazon.awssdk.auth;
    requires software.amazon.awssdk.core;
    requires software.amazon.awssdk.regions;
    requires software.amazon.awssdk.services.rekognition;
    requires java.desktop;
    exports com.mukhtar.UdaSecurity.Service;
}