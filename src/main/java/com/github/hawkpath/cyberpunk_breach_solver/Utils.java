package com.github.hawkpath.cyberpunk_breach_solver;

import java.io.File;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Paths;

public class Utils {

  public static File getResource(String name) {
    ClassLoader classLoader = Main.class.getClassLoader();
    URL resource = classLoader.getResource(name);
    assert resource != null;
    try {
      return Paths.get(resource.toURI()).toFile();
    } catch (URISyntaxException e) {
      return null;
    }
  }

}
