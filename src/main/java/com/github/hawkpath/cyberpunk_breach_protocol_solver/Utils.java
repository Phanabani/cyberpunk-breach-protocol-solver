package com.github.hawkpath.cyberpunk_breach_protocol_solver;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Color;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;

public class Utils {

  final static Logger logger = LoggerFactory.getLogger(Utils.class.getName());

  public static File getResource(String name) {
    ClassLoader classLoader = Main.class.getClassLoader();
    URL resource = classLoader.getResource(name);
    assert resource != null;
    try {
      return Paths.get(resource.toURI()).toFile();
    } catch (Exception e) {
      logger.warn("Failed to get resource", e);
      return null;
    }
  }

  public static File getRelativeFile(String path) {
    return Paths.get(path).toAbsolutePath().normalize().toFile();
  }

  public static HashMap<String, String> loadConfig(String path) {
    HashMap<String, String> map = new HashMap<>(4);
    List<String> lines;
    try {
      lines = Files.readAllLines(Paths.get(path));
    } catch (IOException e) {
      logger.warn("Failed to read config file", e);
      return map;
    }

    for (String line : lines) {
      if (line.equals("") || line.startsWith("#"))
        continue;
      String[] keyVal = line.split("=");
      if (keyVal.length != 2) {
        logger.warn("Bad config line, expected KEY=VALUE, got {}", line);
        continue;
      }
      map.put(keyVal[0], keyVal[1]);
    }

    return map;
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
