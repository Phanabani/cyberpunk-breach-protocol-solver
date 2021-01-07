package com.github.hawkpath.cyberpunk_breach_solver;

import java.io.File;
import java.net.URISyntaxException;
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
    } catch (URISyntaxException e) {
      return null;
    }
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

}