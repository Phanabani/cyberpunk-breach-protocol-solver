package cyberpunk_decryption;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.Stack;

class SolutionNode {
  public int x;
  public int y;
  public int value;

  public SolutionNode(int x, int y, int value) {
    this.x = x;
    this.y = y;
    this.value = value;
  }
}

class Array2D<T> {

  private T[][] data;
  private int width;
  private int height;

  public Array2D(T[][] data) {
    this.data = data;
    width = data[0].length;
    height = data.length;

    for (int i=1; i<data.length; i++) {
      assert data[i].length == data[i-1].length : "Rows must be of equal length";
    }
  }

  public T get(int x, int y) throws IndexOutOfBoundsException {
    return data[y][x];
  }

  public int getWidth() {
    return width;
  }

  public int getHeight() {
    return height;
  }

  public int findInRow(int row, T value, int start) {
    for (int i=start; i<width; i++) {
      if (get(i, row).equals(value))
        return i;
    }
    return -1;
  }

  public int findInColumn(int col, T value, int start) {
    for (int i=start; i<height; i++) {
      if (get(col, i).equals(value))
        return i;
    }
    return -1;
  }

}

public class Solver {

  private Array2D<Integer> data;
  private Integer[][] sequences;
  private int bufferSize;
  private SolutionNode[] solution = null;

  public Solver(Integer[][] data, Integer[][] sequences, int bufferSize) {
    this.data = new Array2D<>(data);
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
    ArrayDeque<SolutionNode> stack = new ArrayDeque<>();
    Integer[] seq = sequences[sequences.length - 1];
    boolean solved = solveRecursive(stack, seq, 0, 0);
    solution = solved ? stack.toArray(new SolutionNode[0]) : null;
  }

  private boolean solveRecursive(
      Deque<SolutionNode> deque, Integer[] sequence, int seqIndex, int rowOrCol
  ) {
    boolean horizontal = seqIndex % 2 == 0;

    if (horizontal) {
      for (
          int x = data.findInRow(rowOrCol, sequence[seqIndex], 0);
          x != -1 && x < data.getWidth();
          x = data.findInRow(rowOrCol, sequence[seqIndex], x+1)
      ) {
        // Iterate through all instances of value in this row
        if (seqIndex == sequence.length - 1) {
          // We reached the end of the sequence and found everything.
          // Add to the solution stack and collapse back up the call stack.
          deque.push(new SolutionNode(x, rowOrCol, sequence[seqIndex]));
          return true;
        } else if (solveRecursive(deque, sequence, seqIndex + 1, x)) {
          // We searched recursively and found everything.
          // Add to the solution stack and collapse up.
          deque.push(new SolutionNode(x, rowOrCol, sequence[seqIndex]));
          return true;
        }
      }
    }

    else {
      for (
          int y = data.findInColumn(rowOrCol, sequence[seqIndex], 0);
          y != -1 && y < data.getHeight();
          y = data.findInColumn(rowOrCol, sequence[seqIndex], y+1)
      ) {
        // Iterate through all instances of value in this row
        if (seqIndex == sequence.length - 1) {
          // We reached the end of the sequence and found everything.
          // Add to the solution stack and collapse back up the call stack.
          deque.push(new SolutionNode(rowOrCol, y, sequence[seqIndex]));
          return true;
        } else if (solveRecursive(deque, sequence, seqIndex + 1, y)) {
          // We searched recursively and found everything.
          // Add to the solution stack and collapse up.
          deque.push(new SolutionNode(rowOrCol, y, sequence[seqIndex]));
          return true;
        }
      }
    }

    return false;
  }

  public void print() {
    if (solution == null) {
      System.out.println("No solution");
      return;
    }

    for (SolutionNode s : solution) {
      System.out.println(String.format("%H (%d, %d)", s.value, s.x, s.y));
    }
  }

}
