package com.github.hawkpath.cyberpunk_breach_solver;

import java.util.*;

class GridNode {
  public int x;
  public int y;
  public Integer value;

  public GridNode(int x, int y, Integer value) {
    this.x = x;
    this.y = y;
    this.value = value;
  }
}

class Grid2D {

  private GridNode[][] data;
  private int width;
  private int height;

  public Grid2D(Integer[][] data) {
    width = data[0].length;
    height = data.length;
    this.data = new GridNode[height][];

    int lastRowLen = width;
    for (int y=0; y<height; y++) {
      assert data[y].length == lastRowLen : "Rows must be of equal length";
      lastRowLen = data[y].length;

      this.data[y] = new GridNode[width];
      for (int x=0; x<data[y].length; x++) {
        this.data[y][x] = new GridNode(x, y, data[y][x]);
      }
    }
  }

  public GridNode get(int x, int y) throws IndexOutOfBoundsException {
    return data[y][x];
  }

  public int getWidth() {
    return width;
  }

  public int getHeight() {
    return height;
  }

  public GridNode findInRow(int row, int value, int start) {
    for (int i=start; i<width; i++) {
      GridNode node = get(i, row);
      if (node.value.equals(value))
        return node;
    }
    return null;
  }

  public GridNode findInColumn(int col, int value, int start) {
    for (int i=start; i<height; i++) {
      GridNode node = get(col, i);
      if (node.value.equals(value))
        return node;
    }
    return null;
  }

}

public class Solver {

  private Grid2D data = null;
  private Integer[][] sequences = null;
  private int bufferSize = -1;
  private ArrayList<GridNode> solution = null;

  public Solver() {}

  public Solver(Integer[][] data, Integer[][] sequences, int bufferSize) {
    setAll(data, sequences, bufferSize);
  }

  public void setAll(Integer[][] data, Integer[][] sequences, int bufferSize) {
    this.data = new Grid2D(data);
    this.sequences = sequences;
    this.bufferSize = bufferSize;
    solution = null;
  }

  public ArrayList<GridNode> getSolution() {
    return solution;
  }

  public void solve() {
    if (data == null || sequences == null || bufferSize == -1)
      return;
    ArrayDeque<GridNode> stack = new ArrayDeque<>(bufferSize);
    Integer[] seq = sequences[0];
    boolean solved = solveRecursive(stack, seq);
    solution = solved ? new ArrayList<>(stack) : null;
  }

  private boolean solveRecursive(Deque<GridNode> stack, Integer[] sequence) {
    return solveRecursive(stack, sequence, 0, 0, null);
  }

  private boolean solveRecursive(
      Deque<GridNode> deque, Integer[] sequence, int bufferIndex, int seqIndex,
      GridNode lastNode
  ) {
    if (sequence.length - seqIndex + bufferIndex > bufferSize)
      // It's impossible to finish this sequence without overflowing the buffer
      return false;

    int width = data.getWidth();
    int height = data.getHeight();
    boolean horizontal = bufferIndex % 2 == 0;
    if (lastNode == null)
      lastNode = new GridNode(0, 0, 0);

    for (
        GridNode node = horizontal
            ? data.findInRow(lastNode.y, sequence[seqIndex], 0)
            : data.findInColumn(lastNode.x, sequence[seqIndex], 0);
        node != null;
        node = horizontal
            ? data.findInRow(lastNode.y, sequence[seqIndex], node.x + 1)
            : data.findInColumn(lastNode.x, sequence[seqIndex], node.y + 1)
    ) {
      // Iterate through all instances of value in this row or col
      if (deque.contains(node))
        // Already used this node; skip!
        continue;
      deque.addLast(node);
      if (seqIndex == sequence.length - 1
          || solveRecursive(deque, sequence, bufferIndex + 1, seqIndex + 1, node)) {
        // We reached the end of the sequence and found everything.
        // Collapse back up the call stack.
        return true;
      }
      deque.removeLast();
    }

    if (bufferIndex == 0) {
      for (
          GridNode node = horizontal
              ? data.get(0, lastNode.y)
              : data.get(lastNode.x, 0);
          horizontal ? node.x < width - 1 : node.y < height - 1;
          node = horizontal
              ? data.get(node.x + 1, lastNode.y)
              : data.get(lastNode.x, node.y + 1)
      ) {
        // We didn't find anything in this row/col, so just use its cells as a
        // bridge to get to the right value.
        // Do NOT increment seqIndex because we didn't actually find the value here
        deque.addLast(node);
        if (solveRecursive(deque, sequence, bufferIndex + 1, seqIndex, node))
          return true;
        deque.removeLast();
      }
    }

    return false;
  }

  public void print() {
    if (solution == null) {
      System.out.println("No solution");
      return;
    }

    for (GridNode s : solution) {
      System.out.println(String.format("%H (%d, %d)", s.value, s.x, s.y));
    }

    ArrayList<GridNode> solutionSorted = new ArrayList<>(solution);
    solutionSorted.sort((GridNode a, GridNode b) -> {
      if (a.y != b.y)
        return Integer.compare(a.y, b.y);
      return Integer.compare(a.x, b.x);
    });

    int nextIdx = 0;
    GridNode next = solutionSorted.get(0);
    int width = data.getWidth();
    int height = data.getHeight();
    for (int y=0; y<height; y++) {
      for (int x=0; x<width; x++) {
        if (next != null && next.x == x && next.y == y) {
          System.out.print(String.format("%H ", next.value));
          next = nextIdx < solution.size() - 1 ? solutionSorted.get(++nextIdx) : null;
        } else {
          System.out.print("-- ");
        }
      }
      System.out.println();
    }
  }
}
