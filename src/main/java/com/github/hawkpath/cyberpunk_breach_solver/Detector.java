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

public class Detector {

  private static final Rectangle basisRect = new Rectangle(0, 0, 2560, 1440);
  private static final Rectangle basisMatrixRect = new Rectangle(375, 450, 566, 510);
  // 3 sequences; might be taller downwards for more
  private static final Rectangle basisSequencesRect = new Rectangle(1100, 450, 384, 80);
  // 6 buffer slots; might be longer to the right
  private static final Rectangle basisBufferRect = new Rectangle(1143, 241, 364, 84);
  private static final ArrayList<String> possibleCells = new ArrayList<>(Arrays.asList(
      "FF", "55", "1C", "BD", "E9", "7A"
  ));

  private static final int THRESHOLD = 100;

  private Tesseract tess;
  private Robot robot;
  private Rectangle screenRect;
  private Rectangle screenMatrixRect;
  private Rectangle screenSequencesRect;
  private Rectangle screenBufferRect;

  public Detector() throws AWTException {
    DisplayMode display = GraphicsEnvironment.getLocalGraphicsEnvironment()
        .getDefaultScreenDevice().getDisplayMode();
    robot = new Robot();
    screenRect = new Rectangle(0, 0, display.getWidth(), display.getHeight());
    screenMatrixRect = scaleToScreen(basisMatrixRect);
    screenSequencesRect = scaleToScreen(basisSequencesRect);
    screenBufferRect = scaleToScreen(basisBufferRect);

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

  private Rectangle scaleToScreen(Rectangle rect) {
    float scaleX = (float) screenRect.width / basisRect.width;
    float scaleY = (float) screenRect.height / basisRect.height;
    int x = (int)(scaleX * rect.x);
    int y = (int)(scaleY * rect.y);
    int w = (int)(scaleX * rect.width);
    int h = (int)(scaleY * rect.height);
    return new Rectangle(x, y, w, h);
  }

  private DetectionResult doOCR(BufferedImage img) {
    try {
      ImageProcessing.threshold(img, THRESHOLD);
      ImageProcessing.invert(img);
      String text = tess.doOCR(img);
      ArrayList<Rectangle> regions = (ArrayList<Rectangle>) tess.getSegmentedRegions(
          img, ITessAPI.TessPageIteratorLevel.RIL_WORD
      );
      return new DetectionResult(parseText(text), regions);
    } catch (TesseractException e) {
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
      word = findNearestPossible(word);
    }
    try {
      return Integer.parseInt(word, 16);
    } catch (NumberFormatException e) {
      return -1;
    }
  }

  /**
   * Find the nearest string by Levenshtein distance
   */
  private static String findNearestPossible(String word) {
    LevenshteinDistance leven = new LevenshteinDistance(4);

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
