package cyberpunk_decryption;

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

    int lastRowLen = data[0].length;
    for (int y=0; y<data.length; y++) {
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

  private Grid2D data;
  private Integer[][] sequences;
  private int bufferSize;
  private GridNode[] solution = null;

  public Solver(Integer[][] data, Integer[][] sequences, int bufferSize) {
    this.data = new Grid2D(data);
    this.sequences = sequences;
    this.bufferSize = bufferSize;
  }

  public Solver(String data, String sequences, int bufferSize) {
    this(parseHex(data), parseHex(sequences), bufferSize);
  }

  public Solver(String[] data, String[] sequences, int bufferSize) {
    this(parseHex(data), parseHex(sequences), bufferSize);
  }

  private static Integer[][] parseHex(String hex) {
    ArrayList<Integer[]> lines = new ArrayList<>();
    for (String hexLine : hex.split("\n")) {
      lines.add(parseHexLine(hexLine));
    }
    return lines.toArray(new Integer[0][0]);
  }

  private static Integer[][] parseHex(String[] hexLines) {
    ArrayList<Integer[]> lines = new ArrayList<>();
    for (String hexLine : hexLines) {
      lines.add(parseHexLine(hexLine));
    }
    return lines.toArray(new Integer[0][0]);
  }

  private static Integer[] parseHexLine(String hexLine) {
    ArrayList<Integer> line = new ArrayList<>();
    for (String b : hexLine.split(" ")) {
      line.add(Integer.parseInt(b, 16));
    }
    return line.toArray(new Integer[0]);
  }

  public void solve() {
    // Organize a list of possible sequence solutions.
    // The last sequence should be guaranteed, and work down to lower priority
    // sequences.
    // We can join sequences end to end, optionally merging first/last elements.
    // Search orientation (vertical/horizontal) alternates each element.
    // This is a recursive problem.
    ArrayDeque<GridNode> stack = new ArrayDeque<>(bufferSize);
    Integer[] seq = sequences[sequences.length - 1];
    boolean solved = solveRecursive(stack, seq);
    solution = solved ? stack.toArray(new GridNode[0]) : null;
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

    GridNode[] solutionSorted = solution.clone();
    Arrays.sort(solutionSorted, (GridNode a, GridNode b) -> {
      if (a.y != b.y)
        return Integer.compare(a.y, b.y);
      return Integer.compare(a.x, b.x);
    });

    int nextIdx = 0;
    GridNode next = solutionSorted[0];
    int width = data.getWidth();
    int height = data.getHeight();
    for (int y=0; y<height; y++) {
      for (int x=0; x<width; x++) {
        if (next != null && next.x == x && next.y == y) {
          System.out.print(String.format("%H ", next.value));
          next = nextIdx < solution.length - 1 ? solutionSorted[++nextIdx] : null;
        } else {
          System.out.print("-- ");
        }
      }
      System.out.println();
    }
  }
}
