package com.github.hawkpath.cyberpunk_breach_solver;

import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;

public class ImageProcessing {

  public static void threshold(BufferedImage img, int threshold) {
    int[] pixels = ((DataBufferInt)img.getRaster().getDataBuffer()).getData();

    for(int i = 0; i < pixels.length; i++){
      int r = (0xFF_00_00 & pixels[i]) >> 16;
      int g = (0x00_FF_00 & pixels[i]) >> 8;
      int b = (0x00_00_FF & pixels[i]);
      int value = Math.max(Math.max(r, g), b);
      pixels[i] = value > threshold ? 0xFFFFFF : 0;
    }
  }

  public static void invert(BufferedImage img) {
    int[] pixels = ((DataBufferInt)img.getRaster().getDataBuffer()).getData();

    for(int i = 0; i < pixels.length; i++){
      int r = (0xFF_00_00 & pixels[i]) >> 16;
      int g = (0x00_FF_00 & pixels[i]) >> 8;
      int b = (0x00_00_FF & pixels[i]);
      pixels[i] = (255 - r) << 16 | (255 - g) << 8 | (255 - b);
    }
  }

}
