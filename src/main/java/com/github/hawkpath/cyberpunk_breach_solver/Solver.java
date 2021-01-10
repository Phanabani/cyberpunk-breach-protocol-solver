package com.github.hawkpath.cyberpunk_breach_solver;

import com.github.dakusui.combinatoradix.Combinator;
import com.github.dakusui.combinatoradix.Permutator;
import static com.github.dakusui.combinatoradix.Utils.nCk;

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

class SequencePermutator implements Iterable<List<List<Integer>>> {
  /*
   * We're assuming that the sequences are ordered in ascending priority
   *
   * Get all permutations of:
   * 1234
   * 123
   * 124
   * 134
   * 12
   * 13
   * 14
   * 1
   * 234
   * 23
   * 24
   * 2
   * 34
   * 3
   * 4
   */

  private List<? extends List<Integer>> sequences;
  private int maxBufferSize;
  private List<List<List<Integer>>> permutations;

  public SequencePermutator(List<? extends List<Integer>> sequences, int maxBufferSize) {
    Collections.reverse(sequences);
    this.sequences = sequences;
    this.maxBufferSize = maxBufferSize;
    generatePermutations();
  }

  @Override
  public Iterator<List<List<Integer>>> iterator() {
    return permutations.iterator();
  }

  private static int totalLength(List<List<Integer>> list) {
    int total = 0;
    for (List<Integer> row : list) {
      total += row.size();
    }
    return total;
  }

  private static long rowsWithItemInFirstPosition(long total, long select, long index) {
    try {
      return nCk(total - index - 1, select - 1);
    } catch (ArithmeticException e) {
      return 0;
    }
  }

  private void generatePermutations() {
    int total = sequences.size();
    Combinator<List<Integer>> combinator;
    Permutator<List<Integer>> permutator;
    List<List<List<Integer>>> permutations = new ArrayList<>();
    int[] startPositions = new int[total];

    for (int element=0; element<total; element++) {
      // Iterate through each element in the list. We want to get all combinations
      // with this element in the first position, then the next element, etc.

      for (int select=total; select>=1; select--) {
        // Iterate from largest combinations to smallest
        combinator = new Combinator<>(sequences, select);
        // Get the index where this element first shows up as the first element,
        // get the number of times it appears in the first position, and add
        // this number of times to the start index for the next element with this
        // value of `select`.
        long startPos = startPositions[select-1];
        long elementCount = rowsWithItemInFirstPosition(total, select, element);
        startPositions[select-1] += elementCount;

        for (long i=startPos; i<startPos+elementCount; i++) {
          // Iterate through each combination with this element in the first position
          permutator = new Permutator<>(combinator.get(i), select);

          for (List<List<Integer>> permutation : permutator) {
            // Iterate through each permutation of this combination
            if (totalLength(permutation) <= maxBufferSize)
              permutations.add(permutation);
          }
        }
      }
    }
    this.permutations = permutations;
  }
}

public class Solver {

  private Grid2D data = null;
  private SequencePermutator sequencePermutator = null;
  private int bufferSize = -1;
  private ArrayList<GridNode> solution = null;

  public Solver() {}

  public Solver(Integer[][] data, List<List<Integer>> sequences, int bufferSize) {
    setAll(data, sequences, bufferSize);
  }

  public void setAll(Integer[][] data, List<? extends List<Integer>> sequences, int bufferSize) {
    this.data = new Grid2D(data);
    this.sequencePermutator = new SequencePermutator(sequences, bufferSize);
    this.bufferSize = bufferSize;
    solution = null;
  }

  public ArrayList<GridNode> getSolution() {
    return solution;
  }

  public void solve() {
    if (data == null || sequencePermutator == null || bufferSize == -1)
      return;
    solution = null;
    ArrayDeque<GridNode> stack = new ArrayDeque<>(bufferSize);
    for (List<List<Integer>> sequences : sequencePermutator) {
      if (solveRecursive(stack, sequences)) {
        solution = new ArrayList<>(stack);
        break;
      }
    }
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

    if (seqIndex == 0 && bufferIndex + sequence.length < bufferSize) {
      // We didn't find anything in this row/col, so just use its cells as a
      // bridge to get to the right value.
      // Do NOT increment seqIndex because we didn't actually find the value here
      for (
          GridNode node = horizontal
              ? data.get(0, lastNode.y)
              : data.get(lastNode.x, 0);
          horizontal ? node.x < width - 1 : node.y < height - 1;
          node = horizontal
              ? data.get(node.x + 1, lastNode.y)
              : data.get(lastNode.x, node.y + 1)
      ) {
        if (bufferIndex != 0 && node == lastNode)
          // Skip the node we just were on
          continue;
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
