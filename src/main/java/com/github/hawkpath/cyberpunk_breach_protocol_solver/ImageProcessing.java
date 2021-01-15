package com.github.hawkpath.cyberpunk_breach_protocol_solver;

import java.awt.Point;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.DataBufferInt;
import java.awt.image.WritableRaster;

public class ImageProcessing {

  public static BufferedImage copy(BufferedImage img) {
    ColorModel cm = img.getColorModel();
    boolean isAlphaPremultiplied = cm.isAlphaPremultiplied();
    WritableRaster raster = img.copyData(null);
    return new BufferedImage(cm, raster, isAlphaPremultiplied, null);
  }

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

  public static Point searchDirectionallyUntil(
      BufferedImage img,
      Point startPos, int deltaX, int deltaY,
      ColorHSV color, float tolH, float tolS, float tolV
  ) {
    int x = startPos.x;
    int y = startPos.y;
    int width = img.getWidth();
    int height = img.getHeight();
    int[] pixels = ((DataBufferInt)img.getRaster().getDataBuffer()).getData();

    for ( ; x <= width && y <= height; x+=deltaX, y+=deltaY) {
      int pixel = pixels[y * width + x];
      if (color.withinTolerance(ColorHSV.fromRGB(pixel), tolH, tolS, tolV))
        return new Point(x, y);
    }
    return null;
  }

  public static Point searchDirectionallyUntil(
      BufferedImage img, Point startPos, int deltaX, int deltaY, int color
  ) {
    int x = startPos.x;
    int y = startPos.y;
    int width = img.getWidth();
    int height = img.getHeight();
    int[] pixels = ((DataBufferInt)img.getRaster().getDataBuffer()).getData();
    int pixel;

    for ( ; x >= 0 && x < width && y >= 0 && y < height; x+=deltaX, y+=deltaY) {
      pixel = pixels[y*width + x];
      if (pixel == color)
        return new Point(x, y);
    }
    return null;
  }

}
