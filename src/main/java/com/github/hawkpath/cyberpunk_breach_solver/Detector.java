package com.github.hawkpath.cyberpunk_breach_solver;

import net.sourceforge.tess4j.ITessAPI;
import net.sourceforge.tess4j.Tesseract;
import net.sourceforge.tess4j.TesseractException;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.ArrayList;

class OCRResult {
  String text;
  ArrayList<Rectangle> regions;

  public OCRResult(String text, ArrayList<Rectangle> regions) {
    this.text = text;
    this.regions = regions;
  }
}

public class Detector {

  private static final Rectangle basisRect = new Rectangle(0, 0, 2560, 1440);
  private static final Rectangle basisMatrixRect = new Rectangle(375, 450, 566, 510);
  // 3 sequences; might be taller downwards for more
  private static final Rectangle basisSequencesRect = new Rectangle(1155, 450, 384, 304);
  // 6 buffer slots; might be longer to the right
  private static final Rectangle basisBufferRect = new Rectangle(1143, 241, 364, 84);
  private static final String[] possibleCells = {"FF", "55", "1C", "BD", "E9", "7A"};

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

  private OCRResult doOCR(BufferedImage img) {
    try {
      ImageProcessing.threshold(img, THRESHOLD);
      ImageProcessing.invert(img);
      String text = tess.doOCR(img);
      ArrayList<Rectangle> regions = (ArrayList<Rectangle>) tess.getSegmentedRegions(
          img, ITessAPI.TessPageIteratorLevel.RIL_WORD
      );
      return new OCRResult(text, regions);
    } catch (TesseractException e) {
      return null;
    }
  }

  private OCRResult detect(Rectangle captureRegion) {
    BufferedImage capture = robot.createScreenCapture(captureRegion);
    OCRResult ocrResult = doOCR(capture);
    if (ocrResult == null)
      return null;
    offsetRegions(ocrResult.regions, captureRegion.getLocation());
    return ocrResult;
  }

  public OCRResult detectMatrix() {
    return detect(screenMatrixRect);
  }

  public OCRResult detectSequences() {
    return detect(screenSequencesRect);
  }

}
