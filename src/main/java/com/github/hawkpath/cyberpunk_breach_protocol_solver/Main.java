package com.github.hawkpath.cyberpunk_breach_protocol_solver;

import org.jnativehook.GlobalScreen;
import org.jnativehook.NativeHookException;
import org.jnativehook.keyboard.NativeKeyEvent;
import org.jnativehook.keyboard.NativeKeyListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.AWTException;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.logging.LogManager;

public class Main implements NativeKeyListener {

  final Logger logger = LoggerFactory.getLogger(Main.class.getName());

  String findSolutionKey, clearSolutionKey, bringToTopKey;
  Detector detector;
  Solver solver;
  Overlay overlay;

  public Main(boolean setVisible) {
    loadConfig();

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

  private void loadConfig() {
    HashMap<String, String> config = Utils.loadConfig("./config.txt");
    if (!config.containsKey("findSolution")) {
      logger.error("Missing config line \"findSolution=KEYBIND\"");
    } else if (!config.containsKey("clearSolution")) {
      logger.error("Missing config line \"clearSolution=KEYBIND\"");
    } else if (!config.containsKey("bringToTop")) {
      logger.error("Missing config line \"bringToTop=KEYBIND\"");
    }
    findSolutionKey = config.get("findSolution").toLowerCase();
    clearSolutionKey = config.get("clearSolution").toLowerCase();
    bringToTopKey = config.get("bringToTop").toLowerCase();
  }

  public void nativeKeyTyped(NativeKeyEvent e) {}
  public void nativeKeyPressed(NativeKeyEvent e) {}
  public void nativeKeyReleased(NativeKeyEvent e) {
    String key = NativeKeyEvent.getKeyText(e.getKeyCode());
    String modifiers = NativeKeyEvent.getModifiersText(e.getModifiers());
    String keypress = (modifiers.equals("")) ? key : modifiers + "+" + key;
    keypress = keypress.toLowerCase();
    if (keypress.equals(findSolutionKey)) {
      try {
        runSuite();
      } catch (Exception exc) {
        logger.error("Unexpected error", exc);
      }
    } else if (keypress.equals(clearSolutionKey)) {
      overlay.clearSolution();
    } else if (keypress.equals(bringToTopKey)) {
      overlay.forceOnTop();
    }
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

    logger.debug("Matrix:\n{}", detection.matrix);
    logger.debug("Sequences:\n{}", detection.sequences);
    logger.debug("Buffer size: {}", detection.bufferSize);

    solver.setAll(detection);
    solver.solve();
    List<OCRArrayNode> solution = solver.getSolution();
    if (solution == null) {
      overlay.clearSolution();
      return;
    }

    if (logger.isDebugEnabled()) {
      StringBuilder sb = new StringBuilder();
      for (OCRArrayNode node : solution) {
        sb.append(String.format("\n(%d, %d) %02X", node.x, node.y, node.value));
      }
      logger.debug("Solution:{}", sb);
    }

    overlay.setSolution(solution);

    System.gc();
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
