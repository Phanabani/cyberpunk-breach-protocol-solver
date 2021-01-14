package com.github.hawkpath.cyberpunk_breach_solver;

import java.awt.Color;
import java.io.File;
import java.net.URL;
import java.nio.file.Paths;
import java.util.ArrayList;

public class Utils {

  public static File getResource(String name) {
    ClassLoader classLoader = Main.class.getClassLoader();
    URL resource = classLoader.getResource(name);
    assert resource != null;
    try {
      return Paths.get(resource.toURI()).toFile();
    } catch (Exception e) {
      return null;
    }
  }

  public static File getRelativeFile(String path) {
    return Paths.get(path).toAbsolutePath().normalize().toFile();
  }

  public static boolean isGridUniform(ArrayList<ArrayList<Integer>> list) {
    if (list.size() <= 1)
      return true;

    int rowSize = list.get(0).size();
    for (int i=1; i<list.size(); i++) {
      if (list.get(i).size() != rowSize)
        return false;
    }

    return true;
  }

  public static Integer[][] toJaggedArray(ArrayList<ArrayList<Integer>> list) {
    if (list.size() == 0)
      return null;

    Integer[][] outArr = new Integer[list.size()][];
    for (int i=0; i<list.size(); i++) {
      outArr[i] = list.get(i).toArray(new Integer[0]);
    }

    return outArr;
  }

  public static Integer[][] tryGet2DSubarray(ArrayList<ArrayList<Integer>> list) {
    if (list.size() == 0)
      return null;

    int rowLen = list.get(0).size();
    int sameSizedRows = 0;
    for ( ; sameSizedRows<list.size(); sameSizedRows++) {
      if (list.get(sameSizedRows).size() != rowLen)
        break;
    }
    if (list.size() - sameSizedRows > 2)
      return null;

    Integer[][] outArr = new Integer[sameSizedRows][];
    for (int i=0; i<sameSizedRows; i++) {
      outArr[i] = list.get(i).toArray(new Integer[0]);
    }

    return outArr;
  }

  public static Color ColorRGBLerp(Color colorA, Color colorB, float t) {
    float tt = 1f - t;
    int r = (int)(tt * colorA.getRed() + t * colorB.getRed());
    int g = (int)(tt * colorA.getGreen() + t * colorB.getGreen());
    int b = (int)(tt * colorA.getBlue() + t * colorB.getBlue());
    int a = (int)(tt * colorA.getAlpha() + t * colorB.getAlpha());
    return new Color(r, g, b, a);
  }

}
