package cyberpunk_decryption;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Deque;

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
    boolean solved = solveRecursive(stack, seq, 0, 0, 0);
    solution = solved ? stack.toArray(new SolutionNode[0]) : null;
  }

  private boolean solveRecursive(
      Deque<SolutionNode> deque, Integer[] sequence,
      int bufferIndex, int seqIndex, int rowOrCol
  ) {
    if (sequence.length - seqIndex + bufferIndex > bufferSize)
      // It's impossible to finish this sequence without overflowing the buffer
      return false;

    boolean horizontal = bufferIndex % 2 == 0;

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
        } else if (solveRecursive(deque, sequence, bufferIndex+1, seqIndex+1, x)) {
          // We searched recursively and found everything.
          // Add to the solution stack and collapse up.
          deque.push(new SolutionNode(x, rowOrCol, sequence[seqIndex]));
          return true;
        }
      }

      for (int x = 0; x < data.getWidth(); x++) {
        // We didn't find anything in this row, so just use cells in this row
        // as a bridge to get to the right value
        // Do NOT increment seqIndex because we didn't actually find the value here
        if (solveRecursive(deque, sequence, bufferIndex+1, seqIndex, x)) {
          deque.push(new SolutionNode(x, rowOrCol, data.get(x, rowOrCol)));
          return true;
        }
      }
    }

    else {  // vertical
      for (
          int y = data.findInColumn(rowOrCol, sequence[seqIndex], 0);
          y != -1 && y < data.getHeight();
          y = data.findInColumn(rowOrCol, sequence[seqIndex], y+1)
      ) {
        // Iterate through all instances of value in this column
        if (seqIndex == sequence.length - 1) {
          // We reached the end of the sequence and found everything.
          // Add to the solution stack and collapse back up the call stack.
          deque.push(new SolutionNode(rowOrCol, y, sequence[seqIndex]));
          return true;
        } else if (solveRecursive(deque, sequence, bufferIndex+1, seqIndex+1, y)) {
          // We searched recursively and found everything.
          // Add to the solution stack and collapse up.
          deque.push(new SolutionNode(rowOrCol, y, sequence[seqIndex]));
          return true;
        }
      }

      for (int y = 0; y < data.getHeight(); y++) {
        // We didn't find anything in this col, so just use cells in this col
        // as a bridge to get to the right value
        // Do NOT increment seqIndex because we didn't actually find the value here
        if (solveRecursive(deque, sequence, bufferIndex+1, seqIndex, y)) {
          deque.push(new SolutionNode(rowOrCol, y, data.get(rowOrCol, y)));
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

    SolutionNode[] solutionSorted = solution.clone();
    Arrays.sort(solutionSorted, (SolutionNode a, SolutionNode b) -> {
      if (a.y != b.y)
        return Integer.compare(a.y, b.y);
      return Integer.compare(a.x, b.x);
    });

    int width = data.getWidth();
    int height = data.getHeight();
    int lastX = -1;
    int lastY = 0;
    for (SolutionNode s : solutionSorted) {

      if (s.y > lastY) {
        for (int i = lastX; i < width - 1; i++)
          // Print the row's remaining empty cells
          System.out.print("-- ");

        System.out.println();

        for (int i = lastY; i < s.y - 1; i++) {
          // Print fully empty lines
          for (int j = 0; j < width; j++)
            System.out.print("-- ");
          System.out.println();
        }

        lastX = -1;
      }

      if (s.x > lastX) {
        // Print preceding empty cells
        for (int i = lastX; i < s.x - 1; i++)
          System.out.print("-- ");
      }

      lastY = s.y;
      lastX = s.x;
      System.out.print(String.format("%H ", s.value));
    }

    for (int i=lastY; i<height-1; i++)
      System.out.println();
      for (int j = 0; j < width; j++)
        System.out.print("-- ");
  }
}
