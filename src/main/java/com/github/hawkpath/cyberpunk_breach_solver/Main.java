package com.github.hawkpath.cyberpunk_breach_solver;

import java.awt.AWTException;

public class Main {

  public static void main(String[] args) {

    Detector detector;
    try {
      detector = new Detector();
    } catch (AWTException e) {
      System.err.println("Screen capture is unavailable on your system.");
      return;
    }
    OCRResult matrix = detector.detectMatrix();
    OCRResult sequences = detector.detectSequences();
    System.out.println(matrix.text);

    Overlay overlay = new Overlay();
    overlay.setRegions(matrix.regions);
    overlay.setVisible(true);
  }

}
