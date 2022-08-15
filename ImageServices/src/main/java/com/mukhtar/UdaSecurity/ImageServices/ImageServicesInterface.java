package com.mukhtar.UdaSecurity.ImageServices;

import java.awt.image.BufferedImage;

public interface ImageServicesInterface {

    boolean imageContainsCat(BufferedImage image, float confidenceThreshhold);
}
