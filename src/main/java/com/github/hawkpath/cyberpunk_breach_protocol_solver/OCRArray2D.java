package com.github.hawkpath.cyberpunk_breach_protocol_solver;

import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

class OCRArrayNode {
  public int x;
  public int y;
  public Integer value;
  public Rectangle boundingBox;

  public OCRArrayNode(int x, int y, Integer value, Rectangle boundingBox) {
    this.x = x;
    this.y = y;
    this.value = value;
    this.boundingBox = boundingBox;
  }

  public boolean equals(int other) {
    return value.equals(other);
  }

  public boolean equals(OCRArrayNode other) {
    return value.equals(other.value);
  }

  public String toString() {
    return String.format("<OCRArrayNode %02X (%d, %d)>", value, x, y);
  }
}

public class OCRArray2D implements Iterable<List<OCRArrayNode>> {

  private List<List<OCRArrayNode>> rows;
  private List<OCRArrayNode> lastRow;
  private Boolean isGrid = true;

  public OCRArray2D() {
    rows = new ArrayList<>();
  }

  public Iterator<List<OCRArrayNode>> iterator() {
    return rows.iterator();
  }

  public String toString() {
    StringBuilder sb = new StringBuilder();
    for (int y=0; y<rows.size(); y++) {
      if (y != 0)
        sb.append("\n");

      for (OCRArrayNode cell : getRow(y)) {
        sb.append(String.format("%02X ", cell.value));
      }
    }
    return sb.toString();
  }

  public void addRow() {
    lastRow = new ArrayList<>();
    rows.add(lastRow);
  }

  public void add(Integer value, Rectangle boundingBox) {
    makeFieldsDirty();
    lastRow.add(new OCRArrayNode(lastRow.size(), rows.size() - 1, value, boundingBox));
  }

  private void makeFieldsDirty() {
    isGrid = null;
  }

  public OCRArrayNode get(int x, int y) throws IndexOutOfBoundsException {
    return rows.get(y).get(x);
  }

  public List<OCRArrayNode> getRow(int y) throws IndexOutOfBoundsException {
    return rows.get(y);
  }

  public boolean isGrid() {
    if (isGrid != null)
      return isGrid;

    if (rows.size() <= 1) {
      isGrid = true;
      return isGrid;
    }

    int width = rows.get(0).size();
    for (int i=1; i<rows.size(); i++) {
      if (rows.get(i).size() != width) {
        isGrid = false;
        return isGrid;
      }
    }

    isGrid = true;
    return isGrid;
  }

  public int getWidth() {
    return isGrid() ? lastRow.size() : -1;
  }

  public int getHeight() {
    return rows.size();
  }

  public OCRArrayNode findInRow(int row, OCRArrayNode nodeWithValue, int start) {
    return findInRow(row, nodeWithValue.value, start);
  }

  public OCRArrayNode findInRow(int row, int value, int start) {
    if (!isGrid())
      return null;

    for (int i=start; i<getWidth(); i++) {
      OCRArrayNode node = get(i, row);
      if (node.equals(value))
        return node;
    }

    return null;
  }

  public OCRArrayNode findInColumn(int col, OCRArrayNode nodeWithValue, int start) {
    return findInColumn(col, nodeWithValue.value, start);
  }

  public OCRArrayNode findInColumn(int col, int value, int start) {
    if (!isGrid())
      return null;

    for (int i=start; i<getHeight(); i++) {
      OCRArrayNode node = get(col, i);
      if (node.equals(value))
        return node;
    }

    return null;
  }

}