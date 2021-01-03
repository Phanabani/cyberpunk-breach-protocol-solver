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
    boolean solved = solveRecursive(stack, seq);
    solution = solved ? stack.toArray(new SolutionNode[0]) : null;
  }

  private boolean solveRecursive(Deque<SolutionNode> stack, Integer[] sequence) {
    return solveRecursive(stack, sequence, 0, 0, 0, -1);
  }

  private boolean solveRecursive(
      Deque<SolutionNode> stack, Integer[] sequence,
      int bufferIndex, int seqIndex, int rowOrCol, int lastColOrRow
  ) {
    if (sequence.length - seqIndex + bufferIndex > bufferSize)
      // It's impossible to finish this sequence without overflowing the buffer
      return false;

    int width = data.getWidth();
    int height = data.getHeight();
    boolean horizontal = bufferIndex % 2 == 0;

    for (
        int i = horizontal
            ? data.findInRow(rowOrCol, sequence[seqIndex], 0)
            : data.findInColumn(rowOrCol, sequence[seqIndex], 0);
        i != -1 && i < data.getWidth();
        i = horizontal
            ? data.findInRow(rowOrCol, sequence[seqIndex], i+1)
            : data.findInColumn(rowOrCol, sequence[seqIndex], i+1)
    ) {
      // Iterate through all instances of value in this row or col
      if (i == lastColOrRow)
        // This is the same element we just found one call up the stack;
        // skip it
        continue;
      if (seqIndex == sequence.length - 1
          || solveRecursive(stack, sequence, bufferIndex + 1, seqIndex + 1, i, rowOrCol)) {
        // We reached the end of the sequence and found everything.
        // Add to the solution stack and collapse back up the call stack.
        if (horizontal)
          stack.push(new SolutionNode(i, rowOrCol, sequence[seqIndex]));
        else
          stack.push(new SolutionNode(rowOrCol, i, sequence[seqIndex]));
        return true;
      }
    }

    if (bufferIndex == 0) {
      for (int i = 0; i < (horizontal ? width : height); i++) {
        // We didn't find anything in this row/col, so just use its cells as a
        // bridge to get to the right value.
        // Do NOT increment seqIndex because we didn't actually find the value here
        if (solveRecursive(stack, sequence, bufferIndex + 1, seqIndex, i, rowOrCol)) {
          if (horizontal)
            stack.push(new SolutionNode(i, rowOrCol, data.get(i, rowOrCol)));
          else
            stack.push(new SolutionNode(rowOrCol, i, data.get(rowOrCol, i)));
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

    int nextIdx = 0;
    SolutionNode next = solutionSorted[0];
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
