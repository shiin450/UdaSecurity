package com.mukhtar.UdaSecurity.Service;

import java.awt.image.BufferedImage;

public interface ImageServicesInterface {

    boolean imageContainsCat(BufferedImage image, float confidenceThreshhold);
}
