package com.mukhtar.UdaSecurity.ImageServices;

import java.awt.image.BufferedImage;
import java.util.Random;

/**
 * Service that tries to guess if an image displays a cat.
 */
public class FakeImageService implements ImageServicesInterface {
    private final Random r = new Random();

    @Override
    public boolean imageContainsCat(BufferedImage image, float confidenceThreshhold) {
        return r.nextBoolean();
    }
}
