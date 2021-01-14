package com.github.hawkpath.cyberpunk_breach_solver;

import org.jnativehook.GlobalScreen;
import org.jnativehook.NativeHookException;
import org.jnativehook.keyboard.NativeKeyEvent;
import org.jnativehook.keyboard.NativeKeyListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.AWTException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.logging.LogManager;

public class Main implements NativeKeyListener {

  final Logger logger = LoggerFactory.getLogger(Main.class.getName());

  Detector detector;
  Solver solver;
  Overlay overlay;

  public Main(boolean setVisible) {
    try {
      detector = new Detector();
    } catch (AWTException e) {
      logger.error("Screen capture is unavailable on your system.");
      System.exit(1);
    }
    solver = new Solver();
    overlay = new Overlay();
    overlay.setVisible(setVisible);
  }

  public void nativeKeyTyped(NativeKeyEvent e) {}
  public void nativeKeyPressed(NativeKeyEvent e) {}
  public void nativeKeyReleased(NativeKeyEvent e) {
    String key = NativeKeyEvent.getKeyText(e.getKeyCode());
    String modifiers = NativeKeyEvent.getModifiersText(e.getModifiers());
    switch (key) {
      case "5":
        try {
          runSuite();
        } catch (Exception exc) {
          logger.error("Unexpected error ", exc);
        }
        break;
      case "0":
        overlay.clearSolution();
        break;
      case "9":
        overlay.forceOnTop();
        break;
    }
  }

  public void runSuite() {
    StringBuilder sb;

    overlay.clearSolution();
    try {
      Thread.sleep(100);
    } catch (InterruptedException e) {
      // Let the last solution be unpainted
    }

    DetectionResult detection = detector.detect();

    if (detection == null) {
      overlay.clearSolution();
      return;
    }

    if (logger.isDebugEnabled()) {
      sb = new StringBuilder();
      for (ArrayList<Integer> row : detection.matrix.values) {
        for (Integer cell : row) {
          sb.append(String.format("%02X ", cell));
        }
        sb.append("\n");
      }
      logger.debug("Matrix:\n{}", sb);

      sb = new StringBuilder();
      for (ArrayList<Integer> row : detection.sequences.values) {
        for (Integer i : row) {
          sb.append(String.format("%02X ", i));
        }
        sb.append("\n");
      }
      logger.debug("Sequences:\n{}", sb);

      logger.debug("Buffer size: {}", detection.bufferSize);
    }

    Integer[][] matrixArr = Utils.tryGet2DSubarray(detection.matrix.values);
    if (matrixArr == null) {
      overlay.clearSolution();
      return;
    }

    solver.setAll(matrixArr, detection.sequences.values, detection.bufferSize);
    solver.solve();
    ArrayList<GridNode> solution = solver.getSolution();
    if (solution == null) {
      overlay.clearSolution();
      return;
    }

    if (logger.isDebugEnabled()) {
      sb = new StringBuilder();
      for (GridNode s : solver.getSolution()) {
        sb.append(String.format("%02X (%d, %d)\n", s.value, s.x, s.y));
      }
      sb.append("\n");
      logger.debug("Solution:\n{}", sb);
    }

    int matrixWidth = matrixArr[0].length;
    overlay.setRegions(detection.matrix.regions);
    overlay.setSolution(solution, matrixWidth);
  }

  public static void main(String[] args) {
    try {
      // Check out the default logging.properties at %JDK_HOME%/jre/lib/logging.properties
      InputStream configFile = Main.class.getClassLoader().getResourceAsStream("logging.properties");
      LogManager.getLogManager().readConfiguration(configFile);
    } catch (IOException ex) {
      System.out.println("WARNING: Could not open configuration file");
      System.out.println("WARNING: Logging not configured (console output only)");
    }

    Main main = new Main(true);

    try {
      GlobalScreen.registerNativeHook();
    }
    catch (NativeHookException exc) {
      System.out.println(
          "WARNING: There was a problem registering the native hook (for "
              + "handling global key events)."
      );
      System.out.println(exc.getMessage());
      System.exit(1);
    }

    GlobalScreen.addNativeKeyListener(main);
  }

}
