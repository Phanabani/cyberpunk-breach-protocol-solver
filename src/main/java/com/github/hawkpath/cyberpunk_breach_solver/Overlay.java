package com.github.hawkpath.cyberpunk_breach_solver;

import com.sun.jna.Native;
import com.sun.jna.platform.win32.User32;
import com.sun.jna.platform.win32.WinDef;
import com.sun.jna.platform.win32.WinDef.HWND;
import com.sun.jna.platform.win32.WinUser;

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowEvent;
import java.util.ArrayList;

public class Overlay extends JFrame {

  private OverlayComponent overlayComponent;

  public Overlay() {
    super("Cyberpunk Breach Solver");

    DisplayMode display = GraphicsEnvironment.getLocalGraphicsEnvironment()
        .getDefaultScreenDevice().getDisplayMode();
    int refreshRate = display.getRefreshRate();
    int width = display.getWidth();
    int height = display.getHeight();

    setSize(new Dimension(width, height));
    setAlwaysOnTop(true);
    setUndecorated(true);
    setBackground(new Color(0,0,0,0));
    setOpacity(0.95f);

    overlayComponent = new OverlayComponent();
    add(overlayComponent);

    addWindowListener(new java.awt.event.WindowAdapter() {
      public void windowClosing(WindowEvent winEvt) {
        System.exit(0);
      }
    });
  }

  @Override
  public void setVisible(boolean b) {
    super.setVisible(b);
    if (b)
      setTransparent(this);
  }

  /**
   * Source: https://stackoverflow.com/a/28772306
   */
  private static void setTransparent(Component w) {
    WinDef.HWND hwnd = getHWnd(w);
    int wl = User32.INSTANCE.GetWindowLong(hwnd, WinUser.GWL_EXSTYLE);
    wl = wl | WinUser.WS_EX_LAYERED | WinUser.WS_EX_TRANSPARENT;
    User32.INSTANCE.SetWindowLong(hwnd, WinUser.GWL_EXSTYLE, wl);
  }

  /**
   * Get the window handle from the OS
   */
  private static HWND getHWnd(Component w) {
    HWND hwnd = new HWND();
    hwnd.setPointer(Native.getComponentPointer(w));
    return hwnd;
  }

  public void setRegions(ArrayList<Rectangle> regions) {
    overlayComponent.regions = regions;
    forceOnTop();
    overlayComponent.repaint();
  }

  public void setSolution(ArrayList<GridNode> solution, int matrixWidth) {
    overlayComponent.solution = solution;
    overlayComponent.matrixWidth = matrixWidth;
    forceOnTop();
    overlayComponent.repaint();
  }

  public void forceOnTop() {
    setAlwaysOnTop(false);
    setAlwaysOnTop(true);
  }

  public void clearSolution() {
    setSolution(null, -1);
  }

}

class OverlayComponent extends JComponent {

  // static final Color startColor = new Color(0xD0FFAE25, true);
  static final Color startColor = new Color(0xD0FFAE25, true);
  static final Color endColor = new Color(0xD054FFE7, true);
  static final int boxPadding = 10;
  static final int strokeWidth = 6;
  static final Stroke stroke = new BasicStroke(
      strokeWidth, BasicStroke.CAP_BUTT, BasicStroke.JOIN_ROUND
  );
  static final String regionNullMsg = "Region out of bounds. Matrix width is possibly wrong";

  protected ArrayList<Rectangle> regions = null;
  protected ArrayList<GridNode> solution = null;
  protected int matrixWidth = -1;

  private Rectangle getRegion(GridNode node) {
    int index = node.y * matrixWidth + node.x;
    if (index < regions.size())
      return regions.get(index);
    return null;
  }

  @Override
  public void paintComponent(Graphics g0) {
    super.paintComponent(g0);
    if (solution == null || regions == null || matrixWidth == -1) {
      return;
    }

    Graphics2D g = (Graphics2D) g0.create();
    g.setRenderingHint(
        RenderingHints.KEY_ANTIALIASING,
        RenderingHints.VALUE_ANTIALIAS_ON
    );
    g.setStroke(stroke);

    GridNode node, nodeNext;
    Rectangle rect, rectNext;

    for (int i=0; i<solution.size(); i++) {
      node = solution.get(i);
      rect = getRegion(node);
      assert rect != null : regionNullMsg;

      Color c = Utils.ColorRGBLerp(startColor, endColor, (float) i / (solution.size()-1));
      g.setColor(c);
      paintBoxAroundCell(g, rect);

      if (i < solution.size() - 1) {
        // Draw line to next solution
        nodeNext = solution.get(i + 1);
        rectNext = getRegion(nodeNext);
        assert rectNext != null : regionNullMsg;

        g.setColor(Utils.ColorRGBLerp(startColor, endColor, (float)(2*i+1)/(solution.size()-1)/2));
        paintLineBetweenCells(g, node, rect, nodeNext, rectNext);
      }
    }

    g.dispose();
  }

  private void paintBoxAroundCell(Graphics2D g, Rectangle r) {
    g.drawRoundRect(
        r.x - boxPadding, r.y - boxPadding,
        r.width + 2*boxPadding, r.height + 2*boxPadding,
        4, 4
    );
  }

  private void paintLineBetweenCells(
      Graphics2D g, GridNode node1, Rectangle rect1, GridNode node2, Rectangle rect2
  ) {
    int x1, x2, y1, y2;
    int margin = boxPadding + strokeWidth/2;
    if (node1.x != node2.x) {
      // Horizontal
      if (node1.x < node2.x) {
        x1 = rect1.x + rect1.width + margin;
        x2 = rect2.x - margin;
      } else {
        x1 = rect2.x + rect2.width + margin;
        x2 = rect1.x - margin;
      }
      y1 = y2 = rect1.y + rect1.height/2;
    } else {
      // Vertical
      if (node1.y < node2.y) {
        y1 = rect1.y + rect1.height + margin;
        y2 = rect2.y - margin;
      } else {
        y1 = rect2.y + rect2.height + margin;
        y2 = rect1.y - margin;
      }
      x1 = x2 = rect1.x + rect1.width/2;
    }
    g.drawLine(x1, y1, x2, y2);
  }

}
