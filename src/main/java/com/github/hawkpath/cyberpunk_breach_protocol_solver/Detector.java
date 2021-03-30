package com.github.hawkpath.cyberpunk_breach_protocol_solver;

import net.sourceforge.tess4j.ITessAPI;
import net.sourceforge.tess4j.Tesseract;
import net.sourceforge.tess4j.Word;
import org.apache.commons.text.similarity.LevenshteinDistance;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.image.RasterFormatException;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;

class DetectionResult {
  OCRArray2D matrix, sequences;
  int bufferSize;

  public DetectionResult(OCRArray2D matrix, OCRArray2D sequences, int bufferSize) {
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

  private static final int BOX_THRESHOLD = 80;
  private static final int MATRIX_THRESHOLD = 110;
  private static final int MATRIX_THRESHOLD_MAX = 140;
  private static final int MATRIX_THRESHOLD_DELTA = 15;
  private static final int SEQUENCES_THRESHOLD = 130;
  private static final ArrayList<String> possibleCells = new ArrayList<>(Arrays.asList(
      "FF", "55", "1C", "BD", "E9", "7A"
  ));
  private static final LevenshteinDistance leven = new LevenshteinDistance(4);

  private static final Dimension basisDim = new Dimension(2560, 1440);
  private static final Point matrixFindBoxStart = new Point(655, 465);
  private static final Point sequencesFindBoxStart = new Point(1484, 450);
  private static final Point bufferFindBoxStart = new Point(1300, 250);

  private ScreenScaler screenScaler;
  private Tesseract tess;
  private Robot robot;

  public Detector() throws AWTException {
    robot = new Robot();
    screenScaler = new ScreenScaler(basisDim);
    initTesseract();
  }

  private void initTesseract() {
    tess = new Tesseract();

    // https://github.com/tesseract-ocr/tessdata_best
    File tessdata = Utils.getRelativeFile("./tessdata");
    if (tessdata == null) {
      System.err.println("Missing tessdata folder.");
      System.exit(1);
    }
    tess.setDatapath(tessdata.getAbsolutePath());

    tess.setTessVariable("load_system_dawg", "false");
    tess.setTessVariable("load_freq_dawg", "false");
    tess.setTessVariable("tessedit_char_whitelist", " 1579ABCDEF");
    tess.setTessVariable("user_defined_dpi", "300");
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

    // Search down
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

  private OCRArray2D doOCR(BufferedImage img, Rectangle boundingBox) {
    try {
      img = img.getSubimage(
          boundingBox.x, boundingBox.y, boundingBox.width, boundingBox.height
      );
    } catch (RasterFormatException e) {
      return null;
    }

    OCRArray2D array = new OCRArray2D();
    Rectangle lastBounds = null;
    for (Word word : tess.getWords(img, ITessAPI.TessPageIteratorLevel.RIL_WORD)) {
      Rectangle bounds = word.getBoundingBox();
      bounds.translate(boundingBox.x, boundingBox.y);
      if (lastBounds == null || bounds.y > lastBounds.y + lastBounds.height)
        // The bounding box is below the last one, so we're on a new row
        array.addRow();
      array.add(parseWord(word.getText()), bounds);
      lastBounds = bounds;
    }

    return array;
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
    BufferedImage captureMaster = robot.createScreenCapture(screenScaler.scale(new Rectangle(basisDim)));

    // Find bounding boxes

    BufferedImage captureBoundingBoxes = ImageProcessing.copy(captureMaster);
    ImageProcessing.threshold(captureBoundingBoxes, BOX_THRESHOLD);
    ImageProcessing.invert(captureBoundingBoxes);

    Rectangle matrixBox = findBox(
        captureBoundingBoxes, screenScaler.scale(matrixFindBoxStart), 10
    );
    if (matrixBox == null)
      return null;

    Rectangle sequencesBox = findBox(
        captureBoundingBoxes, screenScaler.scale(sequencesFindBoxStart), 6
    );
    if (sequencesBox == null)
      return null;
    // cut out the part of the sequences box with the hack descriptions
    sequencesBox.width = sequencesBox.width * 4 / 10;

    Rectangle bufferBox = findBox(
        captureBoundingBoxes, screenScaler.scale(bufferFindBoxStart), 1
    );
    int bufferSize;
    if (bufferBox == null)
      bufferSize = 8;
    else
      bufferSize = calcBufferSize(bufferBox);

    // Perform OCR

    // Detect the matrix
    BufferedImage captureMatrix;
    OCRArray2D matrix = null;
    for (int thresh=MATRIX_THRESHOLD; thresh<=MATRIX_THRESHOLD_MAX;
         thresh+=MATRIX_THRESHOLD_DELTA) {
      // TODO it'd probably be nice to optimize the image processing by
      //   cropping rather than full copies ;)
      captureMatrix = ImageProcessing.copy(captureMaster);
      ImageProcessing.threshold(captureMatrix, thresh);
      ImageProcessing.invert(captureMatrix);

      matrix = doOCR(captureMatrix, matrixBox);
      if (matrix != null && matrix.isGrid())
        // the OCR successfully found a well-formed grid
        break;
    }
    if (matrix == null)
      return null;

    // Detect the sequences
    BufferedImage captureSequences = ImageProcessing.copy(captureMaster);
    ImageProcessing.threshold(captureSequences, SEQUENCES_THRESHOLD);
    ImageProcessing.invert(captureSequences);

    OCRArray2D sequences = doOCR(captureSequences, sequencesBox);
    if (sequences == null)
      return null;

    return new DetectionResult(matrix, sequences, bufferSize);
  }

}
