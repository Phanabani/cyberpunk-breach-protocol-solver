package com.github.hawkpath.cyberpunk_breach_solver;

import java.awt.Color;

public class ColorHSV {

  private float hue, saturation, value;

  public ColorHSV() {
    this(0f, 0f, 0f);
  }

  public ColorHSV(float hue, float saturation, float value) {
    this.hue = hue;
    this.saturation = saturation;
    this.value = value;
  }

  public float getHue() {
    return hue;
  }

  public void setHue(float newHue) {
    if (newHue < 0f || newHue >= 360f)
      throw new IllegalArgumentException("hue must be in bounds [0, 360)");
    hue = newHue;
  }

  public float getSaturation() {
    return saturation;
  }

  public void setSaturation(float newSaturation) {
    if (newSaturation < 0f || newSaturation >= 360f)
      throw new IllegalArgumentException("saturation must be in bounds [0, 360)");
    saturation = newSaturation;
  }

  public float getValue() {
    return value;
  }

  public void setValue(float newValue) {
    if (newValue < 0f || newValue >= 360f)
      throw new IllegalArgumentException("value must be in bounds [0, 360)");
    value = newValue;
  }

  public static ColorHSV RGBtoHSV(Color rgb) {
    // https://www.rapidtables.com/convert/color/rgb-to-hsv.html
    float R = (float)rgb.getRed();
    float G = (float)rgb.getGreen();
    float B = (float)rgb.getBlue();
    float Cmax = Math.max(Math.max(R, G), B);
    float Cmin = Math.min(Math.min(R, G), B);
    float delta = Cmax - Cmin;

    float h;
    if (delta == 0f)
      h = 0;
    else if (Cmax == R)
      h = 60f * (((G - B) / delta) % 6);
    else if (Cmax == G)
      h = 60f * (((B - R) / delta) + 2);
    else  // (Cmax == B)
      h = 60f * (((R - G) / delta) + 4);

    float s = (Cmax == 0) ? 0 : delta / Cmax;
    float v = Cmax;

    return new ColorHSV(h, s, v);
  }

  public Color toRGB() {
    // https://www.rapidtables.com/convert/color/hsv-to-rgb.html
    float C = value * saturation;
    float X = C * (1 - Math.abs(((hue / 60f) % 2) - 1));
    float m = value - C;

    float R, G, B;
    if (hue < 60f) {
      R = C;
      G = X;
      B = 0;
    } else if (hue < 120f) {
      R = X;
      G = C;
      B = 0;
    } else if (hue < 180f) {
      R = 0;
      G = C;
      B = X;
    } else if (hue < 240f) {
      R = 0;
      G = X;
      B = C;
    } else if (hue < 300f) {
      R = X;
      G = 0;
      B = C;
    } else {
      R = C;
      G = 0;
      B = X;
    }

    int r = (int)((R + m) * 255);
    int g = (int)((G + m) * 255);
    int b = (int)((B + m) * 255);

    return new Color(r, g, b);
  }

  public boolean withinTolerance(ColorHSV other, float tolH, float tolS, float tolV) {
    float deltaH = Math.abs(getHue() - other.getHue());
    float deltaS = Math.abs(getSaturation() - other.getSaturation());
    float deltaV = Math.abs(getValue() - other.getValue());
    return (deltaH < tolH) && (deltaS < tolS) && (deltaV < tolV);
  }

}
