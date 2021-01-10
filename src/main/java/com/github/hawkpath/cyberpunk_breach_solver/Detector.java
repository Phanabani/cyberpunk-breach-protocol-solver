package com.github.hawkpath.cyberpunk_breach_solver;

import net.sourceforge.tess4j.ITessAPI;
import net.sourceforge.tess4j.Tesseract;
import net.sourceforge.tess4j.TesseractException;
import org.apache.commons.text.similarity.LevenshteinDistance;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;

class DetectionResult {
  ArrayList<ArrayList<Integer>> values;
  ArrayList<Rectangle> regions;

  public DetectionResult(ArrayList<ArrayList<Integer>> values, ArrayList<Rectangle> regions) {
    this.values = values;
    this.regions = regions;
  }
}

class ScreenScaler {

  private Dimension basis;
  private Dimension screen;
  private float scaleX;
  private float scaleY;

  public ScreenScaler(Dimension basis) {
    this.basis = basis;
    DisplayMode display = GraphicsEnvironment.getLocalGraphicsEnvironment()
        .getDefaultScreenDevice().getDisplayMode();
    screen = new Dimension(display.getWidth(), display.getHeight());
    scaleX = (float) screen.width / basis.width;
    scaleY = (float) screen.height / basis.height;
  }

  private Point scale(int x, int y) {
    x = (int)(scaleX * x);
    y = (int)(scaleY * y);
    return new Point(x, y);
  }

  private Point scale(Point point) {
    int x = (int)(scaleX * point.x);
    int y = (int)(scaleY * point.y);
    return new Point(x, y);
  }

  private Dimension scale(Dimension dim) {
    int w = (int)(scaleX * dim.width);
    int h = (int)(scaleY * dim.height);
    return new Dimension(w, h);
  }

  private Rectangle scale(Rectangle rect) {
    int x = (int)(scaleX * rect.x);
    int y = (int)(scaleY * rect.y);
    int w = (int)(scaleX * rect.width);
    int h = (int)(scaleY * rect.height);
    return new Rectangle(x, y, w, h);
  }

}

public class Detector {

  private static final int IMAGE_THRESHOLD = 90;
  private static final Dimension basisDim = new Dimension(2560, 1440);
  private static final ArrayList<String> possibleCells = new ArrayList<>(Arrays.asList(
      "FF", "55", "1C", "BD", "E9", "7A"
  ));
  private static final LevenshteinDistance leven = new LevenshteinDistance(4);

  private ScreenScaler screenScaler;
  private Tesseract tess;
  private Robot robot;

  public Detector() throws AWTException {
    robot = new Robot();
    screenScaler = new ScreenScaler(basisDim);
    initTesseract();
  }

  private void initTesseract() {
    // https://github.com/tesseract-ocr/tessdata_best
    File tessdata = Utils.getResource("tessdata");
    assert tessdata != null;
    tess = new Tesseract();
    tess.setDatapath(tessdata.toString());
    tess.setTessVariable("load_system_dawg", "false");
    tess.setTessVariable("load_freq_dawg", "false");
    tess.setTessVariable("tessedit_char_whitelist", " 1579ABCDEF");
  }

  private static void offsetRegions(ArrayList<Rectangle> regions, Point origin) {
    for (Rectangle r : regions) {
      r.translate(origin.x, origin.y);
    }
  }

  private DetectionResult doOCR(BufferedImage img) {
    ImageProcessing.threshold(img, IMAGE_THRESHOLD);
    ImageProcessing.invert(img);
    try {
      String text = tess.doOCR(img);
      ArrayList<Rectangle> regions = (ArrayList<Rectangle>) tess.getSegmentedRegions(
          img, ITessAPI.TessPageIteratorLevel.RIL_WORD
      );
      return new DetectionResult(parseText(text), regions);
    } catch (NullPointerException | TesseractException e) {
      return null;
    }
  }

  private static ArrayList<ArrayList<Integer>> parseText(String text) {
    ArrayList<ArrayList<Integer>> rows = new ArrayList<>();
    for (String rowText : text.split("\n")) {
      // Split up lines
      ArrayList<Integer> row = new ArrayList<>();
      for (String word : rowText.split(" ")) {
        // Parse each word in the line as a hex value
        row.add(parseWord(word));
      }
      rows.add(row);
    }
    return rows;
  }

  private static Integer parseWord(String word) {
    if (!possibleCells.contains(word)) {
      word = findNearestPossibleCellValue(word);
    }
    try {
      return Integer.parseInt(word, 16);
    } catch (NumberFormatException e) {
      return -1;
    }
  }

  /**
   * Find the nearest cell value by Levenshtein distance
   */
  private static String findNearestPossibleCellValue(String word) {
    int lowestDist = 0;
    String bestMatch = null;
    for (String possible : possibleCells) {
      Integer dist = leven.apply(possible, word);
      if (bestMatch == null || dist < lowestDist) {
        lowestDist = dist;
        bestMatch = possible;
      }
    }

    return bestMatch;
  }

  private DetectionResult detect(Rectangle captureRegion) {
    BufferedImage capture = robot.createScreenCapture(captureRegion);
    DetectionResult detectionResult = doOCR(capture);
    if (detectionResult == null)
      return null;
    offsetRegions(detectionResult.regions, captureRegion.getLocation());
    return detectionResult;
  }

  public DetectionResult detectMatrix() {
    return detect(screenMatrixRect);
  }

  public DetectionResult detectSequences() {
    return detect(screenSequencesRect);
  }

}
