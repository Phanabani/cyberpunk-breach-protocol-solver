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

class OCRResult {
  ArrayList<ArrayList<Integer>> values;
  ArrayList<Rectangle> regions;

  public OCRResult(ArrayList<ArrayList<Integer>> values, ArrayList<Rectangle> regions) {
    this.values = values;
    this.regions = regions;
  }
}

class DetectionResult {
  OCRResult matrix, sequences;
  int bufferSize;

  public DetectionResult(OCRResult matrix, OCRResult sequences, int bufferSize) {
    this.matrix = matrix;
    this.sequences = sequences;
    this.bufferSize = bufferSize;
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

  public Point scale(int x, int y) {
    x = (int)(scaleX * x);
    y = (int)(scaleY * y);
    return new Point(x, y);
  }

  public Point scale(Point point) {
    int x = (int)(scaleX * point.x);
    int y = (int)(scaleY * point.y);
    return new Point(x, y);
  }

  public Dimension scale(Dimension dim) {
    int w = (int)(scaleX * dim.width);
    int h = (int)(scaleY * dim.height);
    return new Dimension(w, h);
  }

  public Rectangle scale(Rectangle rect) {
    int x = (int)(scaleX * rect.x);
    int y = (int)(scaleY * rect.y);
    int w = (int)(scaleX * rect.width);
    int h = (int)(scaleY * rect.height);
    return new Rectangle(x, y, w, h);
  }

}

public class Detector {

  private static final int IMAGE_THRESHOLD = 90;
  private static final ArrayList<String> possibleCells = new ArrayList<>(Arrays.asList(
      "FF", "55", "1C", "BD", "E9", "7A"
  ));
  private static final LevenshteinDistance leven = new LevenshteinDistance(4);

  private static final Dimension basisDim = new Dimension(2560, 1440);
  private static final Point matrixFindBoxStart = new Point(655, 465);
  private static final Point sequencesFindBoxStart = new Point(1484, 450);
  private static final Point bufferFindBoxStart = new Point(1160, 246);

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

  /**
   * Find a black outlined rectangle in the image. The algorithm first searches
   * left until it finds a black pixel (leftmost bounds of the box), searches
   * up to get the top left bounds, padding, then searches from this point to
   * the right and down to find the dimensions of the box. Each time an edge
   * is found, the cursor moves inward by `padding` pixels to account for edges
   * that aren't perfectly straight.
   * @param img the image to use
   * @param start a point in the top-middle(ish) of the box
   * @param padding how many pixels to move inward after finding a border (to
   *                account for any non-straight-line borders in the binarized
   *                image).
   */
  private Rectangle findBox(BufferedImage img, Point start, int padding) {
    Point p;
    Point topLeft = new Point();
    Dimension dim = new Dimension();

    // Search left
    p = ImageProcessing.searchDirectionallyUntil(img, start, -1, 0, 0x000000);
    if (p == null)
      return null;
    p.translate(padding, 0);
    topLeft.x = p.x;

    // Search up
    p = ImageProcessing.searchDirectionallyUntil(img, p, 0, -1, 0x000000);
    if (p == null)
      return null;
    p.translate(0, padding);
    topLeft.y = p.y;

    // Search right
    p = ImageProcessing.searchDirectionallyUntil(img, topLeft, 1, 0, 0x000000);
    if (p == null)
      return null;
    p.translate(-padding, 0);
    dim.width = p.x - topLeft.x;

    // Search right
    p = ImageProcessing.searchDirectionallyUntil(img, topLeft, 0, 1, 0x000000);
    if (p == null)
      return null;
    p.translate(0, -padding);
    dim.height = p.y - topLeft.y;

    return new Rectangle(topLeft, dim);
  }

  private int calcBufferSize(Rectangle bufferBoundingBox) {
    float width = bufferBoundingBox.width;
    float height = bufferBoundingBox.height;
    // Calculate padding size between buffer bounding box and inner buffer boxes
    float pad = height * 5/18;
    // Get width/height of inner buffer boxes
    float innerWidth = width - 2 * pad;
    float innerHeight = height - 2 * pad;
    // Attenuate for padding between buffer boxes and divide by inner height
    // to get number of boxes
    return (int)(innerWidth * 18/23 / innerHeight);
  }

  private OCRResult doOCR(BufferedImage img, Rectangle boundingBox) {
    img = img.getSubimage(
        boundingBox.x, boundingBox.y, boundingBox.width, boundingBox.height
    );
    try {
      String text = tess.doOCR(img);
      ArrayList<Rectangle> regions = (ArrayList<Rectangle>) tess.getSegmentedRegions(
          img, ITessAPI.TessPageIteratorLevel.RIL_WORD
      );
      offsetRegions(regions, boundingBox.getLocation());
      return new OCRResult(parseText(text), regions);
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
   * Find the most similar cell value by Levenshtein distance (to correct weird
   * values obtained by OCR)
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

  public DetectionResult detect() {
    BufferedImage capture = robot.createScreenCapture(screenScaler.scale(new Rectangle(basisDim)));
    ImageProcessing.threshold(capture, IMAGE_THRESHOLD);
    ImageProcessing.invert(capture);

    Rectangle matrixBox = findBox(capture, screenScaler.scale(matrixFindBoxStart), 10);
    if (matrixBox == null)
      return null;

    Rectangle sequencesBox = findBox(capture, screenScaler.scale(sequencesFindBoxStart), 6);
    if (sequencesBox == null)
      return null;
    // cut out the part of the box with the hack descriptions
    sequencesBox.width = sequencesBox.width * 4 / 10;

    Rectangle bufferBox = findBox(capture, screenScaler.scale(bufferFindBoxStart), 1);
    int bufferSize;
    if (bufferBox == null)
      bufferSize = 8;
    else
      bufferSize = calcBufferSize(bufferBox);

    OCRResult matrix = doOCR(capture, matrixBox);
    if (matrix == null)
      return null;

    OCRResult sequences = doOCR(capture, sequencesBox);
    if (sequences == null)
      return null;

    return new DetectionResult(matrix, sequences, bufferSize);
  }

}
