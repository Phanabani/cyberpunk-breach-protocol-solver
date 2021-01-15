package com.github.hawkpath.cyberpunk_breach_protocol_solver;

import com.sun.jna.Native;
import com.sun.jna.platform.win32.User32;
import com.sun.jna.platform.win32.WinDef;
import com.sun.jna.platform.win32.WinDef.HWND;
import com.sun.jna.platform.win32.WinUser;

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowEvent;
import java.util.List;

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

  public void setSolution(List<OCRArrayNode> solution) {
    overlayComponent.solution = solution;
    forceOnTop();
    overlayComponent.repaint();
  }

  public void forceOnTop() {
    setAlwaysOnTop(false);
    setAlwaysOnTop(true);
  }

  public void clearSolution() {
    setSolution(null);
  }

}

class OverlayComponent extends JComponent {

  static final Color strokeColor = new Color(0xD054FFE7, true);
  static final int boxPadding = 10;
  static final int strokeWidth = 6;
  static final Stroke stroke = new BasicStroke(
      strokeWidth, BasicStroke.CAP_BUTT, BasicStroke.JOIN_ROUND
  );
  static final Stroke strokeFirst = new BasicStroke(
      2, BasicStroke.CAP_BUTT, BasicStroke.JOIN_ROUND
  );

  protected List<OCRArrayNode> solution = null;

  @Override
  public void paintComponent(Graphics g0) {
    super.paintComponent(g0);
    if (solution == null) {
      return;
    }

    Graphics2D g = (Graphics2D) g0.create();
    g.setRenderingHint(
        RenderingHints.KEY_ANTIALIASING,
        RenderingHints.VALUE_ANTIALIAS_ON
    );
    g.setStroke(stroke);
    g.setColor(strokeColor);

    OCRArrayNode node, nodeNext;

    for (int i=0; i<solution.size(); i++) {
      node = solution.get(i);

      paintBoxAroundCell(g, node, boxPadding);
      if (i == 0) {
        g.setStroke(strokeFirst);
        paintBoxAroundCell(g, node, boxPadding + 8);
        g.setStroke(stroke);
      }

      if (i < solution.size() - 1) {
        // Draw line to next solution
        nodeNext = solution.get(i + 1);
        paintLineBetweenCells(
            g, node, nodeNext, (float)i/(solution.size()-1), boxPadding
        );
      }
    }

    g.dispose();
  }

  private void paintBoxAroundCell(Graphics2D g, OCRArrayNode node, int padding) {
    Rectangle r = node.boundingBox;
    g.drawRoundRect(
        r.x - padding, r.y - padding,
        r.width + 2*padding, r.height + 2*padding,
        2*padding/3, 2*padding/3
    );
  }

  private void paintLineBetweenCells(
      Graphics2D g, OCRArrayNode node1, OCRArrayNode node2,
      float offset, int padding
  ) {
    int x1, x2, y1, y2;
    int margin = padding + strokeWidth/2;
    offset = 0.5f * offset + 0.25f;
    Rectangle rect1 = node1.boundingBox;
    Rectangle rect2 = node2.boundingBox;

    if (node1.x != node2.x) {
      // Horizontal
      if (node1.x < node2.x) {
        x1 = rect1.x + rect1.width + margin;
        x2 = rect2.x - margin;
      } else {
        x1 = rect2.x + rect2.width + margin;
        x2 = rect1.x - margin;
      }
      y1 = y2 = rect1.y + (int)(rect1.height*offset);
    } else {
      // Vertical
      if (node1.y < node2.y) {
        y1 = rect1.y + rect1.height + margin;
        y2 = rect2.y - margin;
      } else {
        y1 = rect2.y + rect2.height + margin;
        y2 = rect1.y - margin;
      }
      x1 = x2 = rect1.x + (int)(rect1.width*offset);
    }
    g.drawLine(x1, y1, x2, y2);
  }

}
