package com.github.hawkpath.cyberpunk_breach_solver;

import org.jnativehook.GlobalScreen;
import org.jnativehook.NativeHookException;
import org.jnativehook.keyboard.NativeKeyEvent;
import org.jnativehook.keyboard.NativeKeyListener;

import java.awt.AWTException;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Main implements NativeKeyListener {

  Detector detector;
  Solver solver;
  Overlay overlay;

  public Main(boolean setVisible) {
    try {
      detector = new Detector();
    } catch (AWTException e) {
      System.err.println("Screen capture is unavailable on your system.");
      return;
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
    if (key.equals("5"))
      runSuite();
    else if (key.equals("0"))
      overlay.clearSolution();
  }

  public void runSuite() {
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

    for (ArrayList<Integer> row : detection.matrix.values) {
      for (Integer cell : row) {
        System.out.printf("%02X ", cell);
      }
      System.out.println("\n");
    }

    System.out.println("Sequences:");
    for (ArrayList<Integer> row : detection.sequences.values) {
      for (Integer i : row) {
        System.out.printf("%02X ", i);
      }
      System.out.println();
    }
    System.out.println();

    System.out.printf("Buffer size: %d\n\n", detection.bufferSize);

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

    System.out.println("Solution:");
    for (GridNode s : solver.getSolution()) {
      System.out.printf("%02X (%d, %d)\n", s.value, s.x, s.y);
    }
    System.out.println();

    int matrixWidth = matrixArr[0].length;
    overlay.setRegions(detection.matrix.regions);
    overlay.setSolution(solution, matrixWidth);
  }

  public static void main(String[] args) {
    Main main = new Main(true);

    Logger logger = Logger.getLogger(GlobalScreen.class.getPackage().getName());
    logger.setLevel(Level.WARNING);

    logger.setUseParentHandlers(false);
    try {
      GlobalScreen.registerNativeHook();
    }
    catch (NativeHookException ex) {
      System.err.println("There was a problem registering the native hook.");
      System.err.println(ex.getMessage());
      System.exit(1);
    }

    GlobalScreen.addNativeKeyListener(main);
  }

}
