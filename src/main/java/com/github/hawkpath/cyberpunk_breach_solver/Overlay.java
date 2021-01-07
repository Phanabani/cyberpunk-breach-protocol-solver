package com.github.hawkpath.cyberpunk_breach_solver;

import com.sun.jna.Native;
import com.sun.jna.platform.win32.User32;
import com.sun.jna.platform.win32.WinDef;
import com.sun.jna.platform.win32.WinDef.HWND;
import com.sun.jna.platform.win32.WinUser;

import javax.swing.*;
import java.awt.*;
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
    overlayComponent.repaint();
  }

  public void setSolution(ArrayList<GridNode> solution, int matrixWidth) {
    overlayComponent.solution = solution;
    overlayComponent.matrixWidth = matrixWidth;
    overlayComponent.repaint();
  }

  public void clearSolution() {
    setSolution(null, -1);
  }

}

class OverlayComponent extends JComponent {

  static final Color lineColor = new Color(0x54FFE7);
  static final int boxPadding = 10;
  static final Stroke stroke = new BasicStroke(
      6, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND
  );
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
    g.setColor(lineColor);
    g.setStroke(stroke);

    GridNode s, sNext;
    Rectangle r, rNext;
    int x1, y1, x2, y2;
    for (int i=0; i<solution.size(); i++) {
      s = solution.get(i);
      r = getRegion(s);
      assert r != null : "Region out of bounds. Matrix width is possibly wrong";

      g.drawRoundRect(
          r.x - boxPadding, r.y - boxPadding,
          r.width + 2*boxPadding, r.height + 2*boxPadding,
          4, 4
      );

      if (i < solution.size() - 1) {
        // Draw line to next solution
        sNext = solution.get(i + 1);
        rNext = getRegion(sNext);
        assert rNext != null : "Region out of bounds. Matrix width is possibly wrong";

        if (s.x != sNext.x) {
          // Horizontal
          if (s.x < sNext.x) {
            x1 = r.x + r.width + boxPadding;
            x2 = rNext.x - boxPadding;
          } else {
            x1 = rNext.x + rNext.width + boxPadding;
            x2 = r.x - boxPadding;
          }
          y1 = y2 = r.y + r.height/2;
        } else {
          // Vertical
          if (s.y < sNext.y) {
            y1 = r.y + r.height + boxPadding;
            y2 = rNext.y - boxPadding;
          } else {
            y1 = rNext.y + rNext.height + boxPadding;
            y2 = r.y - boxPadding;
          }
          x1 = x2 = r.x + r.width/2;
        }
        g.drawLine(x1, y1, x2, y2);
      }
    }

    g.dispose();
  }

}
