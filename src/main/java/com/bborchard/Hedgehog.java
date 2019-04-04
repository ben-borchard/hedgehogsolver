package com.bborchard;

import jdk.nashorn.internal.ir.Block;

import java.util.*;

import static com.bborchard.Hedgehog.Alignment.COL;
import static com.bborchard.Hedgehog.Alignment.ROW;

public class Hedgehog {

  private static boolean debug = false;

  private Stack<TreeRow> possibilityTree;
  private Map<Integer, Block> numberIndex;
  private Block[][][] positionIndex;
  private Map<Integer, Adjacency[]> adjacencyMap;
  private Block root;
  private static final Iterator<Block> NO_BLOCKS = new Iterator<Block>() {
    public boolean hasNext(){return false;}
    public Block next(){return null;}
    public void remove() {}
  };

  private int totalChunks;
  private int totalRows;
  private int totalCols;
  private int totalBlocks;

  public Hedgehog(int[][][] puzzle) {
    // initialize totals
    totalChunks = puzzle.length;
    totalRows = puzzle[0].length;
    totalCols = puzzle[0][0].length;
    totalBlocks = totalChunks * totalRows * totalCols;

    // initialize indexes
    numberIndex = new HashMap<Integer, Block>();
    positionIndex = new Block[totalChunks][totalRows][totalCols];
    int chunkIndex = 0;
    for (int[][] chunk : puzzle) {
      int rowIndex = 0;
      for (int[] row : chunk) {
        int colIndex = 0;
        for (int num : row) {
          Location location = new Location(chunkIndex, rowIndex, colIndex);
          Block block = new Block(location, num, num != -1);
          positionIndex[chunkIndex][rowIndex][colIndex] = block;
          if (num != -1) numberIndex.put(num, block);
          if (num == 1) { root = block; }
          colIndex++;
        }
        rowIndex++;
      }
      chunkIndex++;
    }

    // initialize adjacency map
    adjacencyMap = new HashMap<Integer, Adjacency[]>();
    if (totalChunks == 4) {
      // just hard code it - it's fine
      adjacencyMap.put(0, new Adjacency[] {new Adjacency(1, ROW), new Adjacency(2, COL)});
      adjacencyMap.put(1, new Adjacency[] {new Adjacency(0, ROW), new Adjacency(3, COL)});
      adjacencyMap.put(2, new Adjacency[] {new Adjacency(3, ROW), new Adjacency(0, COL)});
      adjacencyMap.put(3, new Adjacency[] {new Adjacency(2, ROW), new Adjacency(1, COL)});
    }

  }

  public void solve() {

    // initialize the possibility tree
    possibilityTree = new Stack<TreeRow>();
    possibilityTree.push(new TreeRow(root, possibilities(root)));

    // start possibility traversal
    while (!possibilityTree.isEmpty()) {

      // descend through possibilities
      while (possibilityTree.peek().blockPossibilities.hasNext()) {
        Block nextPossibility = possibilityTree.peek().blockPossibilities.next();
        if (!nextPossibility.fixed) {
          int nextNum = (possibilityTree.size() + 1);
          nextPossibility.num = nextNum;
          numberIndex.put(nextNum, nextPossibility);
        }
        possibilityTree.push(new TreeRow(nextPossibility, possibilities(nextPossibility)));
        debug("depth: %s", possibilityTree.size());
      }
      debug("no additional possibilities - backtracking");
      if (solved()) {
        debug("solved");
        break;
      }

      // dead end - back out of last possiblity
      Block lastPossibility = possibilityTree.pop().block;
      debug("depth: %s", possibilityTree.size());
      if (!lastPossibility.fixed) {
        numberIndex.remove(lastPossibility.num);
        lastPossibility.num = -1;
      }
    }

    // check if we solved it or if it failed
    if (possibilityTree.isEmpty()) {
      System.err.println("Puzzle not solvable!");
      System.exit(1);
    }

    // print the solved puzzle
    printPuzzle();
  }

  private boolean solved() {
    return numberIndex.size() == totalBlocks && adjacent(root, numberIndex.get(numberIndex.size()));
  }

  private void printPuzzle() {
    debug(Arrays.toString(numberIndex.entrySet().toArray()));
    debug("");
    debug("");
    for (Block[][] chunk : positionIndex) {
      for (Block[] row : chunk) {
        System.out.println(Arrays.toString(row));
      }
      System.out.println();
    }
  }

  private Iterator<Block> possibilities(Block block) {

    // Look for an fixed block next
    Block existingNext = numberIndex.get(block.num + 1);
    if (existingNext != null) {
      // There is a fixed block next - return it if adjacent
      if (adjacent(block, existingNext)) {
        debug("block %s is fixed and adjacent - it is the only possibility", existingNext.num);
        return Arrays.asList(existingNext).iterator();
      } else {
        debug("block %s is fixed but not adjacent - no possibilities", existingNext.num);
        return NO_BLOCKS;
      }
    } else {
      // Go through all adjacent blocks that line up to find possibilities
      List<Block> possibilities = new ArrayList<Block>();
      for (Adjacency adjacency : adjacencyMap.get(block.location.chunk)) {
        for (int i = 0; i < (adjacency.alignment == ROW ? totalCols : totalRows); i++) {
          Block possibility = adjacency.alignment == ROW ?
                  positionIndex[adjacency.chunk][block.location.row][i] :
                  positionIndex[adjacency.chunk][i][block.location.col];
          if (possibility.num == -1) {
            // blocks that already have a number are not possibilities
            possibilities.add(possibility);
          }
        }
      }
      debug("found %s possibilities for block %s", possibilities.size(), block.num);
      return possibilities.iterator();
    }
  }

  private boolean adjacent(Block a, Block b) {
    boolean adjacent = false;
    for (Adjacency adjacency : adjacencyMap.get(a.location.chunk)) {
      if (adjacency.chunk == b.location.chunk) {
        adjacent |= (adjacency.alignment == ROW ? a.location.row == b.location.row : a.location.col == b.location.col);
      }
    }
    return adjacent;
  }

  protected class TreeRow {
    public Block block;
    public Iterator<Block> blockPossibilities;

    public TreeRow(Block block, Iterator<Block> blockPossibilities) {
      this.block = block;
      this.blockPossibilities = blockPossibilities;
    }
  }

  protected class Block {
    public Location location;
    public int num;
    public boolean fixed;

    public Block(Location location, int num, boolean fixed) {
      this.location = location;
      this.num = num;
      this.fixed = fixed;
    }

    @Override
    public String toString() {
      return String.valueOf(num);
    }
  }

  protected class Location {
    public int chunk;
    public int row;
    public int col;

    public Location(int chunk, int row, int col) {
      this.chunk = chunk;
      this.row = row;
      this.col = col;
    }
  }

  protected class Adjacency {
    public int chunk;
    public Alignment alignment;

    public Adjacency(int chunk, Alignment alignment) {
      this.chunk = chunk;
      this.alignment = alignment;
    }
  }

  protected enum Alignment {
    ROW,
    COL
  }


  public static void debug(String message, Object... params) {
    if (debug) {
      System.out.println(String.format(message, params));
    }
  }

  public static void main(String[] args) {
    test23();

  }

  public static void test23() {
    Hedgehog hh = new Hedgehog(new int[][][] {
            new int[][] {
                    new int[] {-1, -1, -1, 27},
                    new int[] {-1, -1, -1, -1},
                    new int[] {55, 57,  5, 59},
                    new int[] {-1, 17, 63, 61}
            },
            new int[][] {
                    new int[] {-1, 32, -1, -1},
                    new int[] {-1, -1, -1, 40},
                    new int[] {58, -1, -1, -1},
                    new int[] {-1, 62, 14, 64}
            },
            new int[][] {
                    new int[] {-1, 18, 42, -1},
                    new int[] {24,  8, -1, -1},
                    new int[] {-1, -1, 34, 60},
                    new int[] {-1, -1, -1, -1}
            },
            new int[][] {
                    new int[] {21, 45, 43, 41},
                    new int[] {51,  9, 49, -1},
                    new int[] {-1, -1, 15,  1},
                    new int[] {53, -1, -1, 29}
            },
    });
    hh.solve();
  }

  public static void test22() {
    Hedgehog hh = new Hedgehog(new int[][][] {
            new int[][] {
                    new int[] {59, -1, -1, -1},
                    new int[] {61,  3, 39,  1},
                    new int[] {57, -1, 41, 55},
                    new int[] {31, 45, -1, -1}
            },
            new int[][] {
                    new int[] {34, 36, -1, 16},
                    new int[] {62, -1, 28,  2},
                    new int[] {-1, -1, -1, -1},
                    new int[] {44, 52, 24, 20}
            },
            new int[][] {
                    new int[] {60,  4, 40, -1},
                    new int[] {-1, -1, 38, -1},
                    new int[] {58, 46, -1, 22},
                    new int[] {-1, 14, -1, 64}
            },
            new int[][] {
                    new int[] {-1, -1, -1, -1},
                    new int[] {33, 11, 25, 17},
                    new int[] { 7, -1, -1, -1},
                    new int[] {63, -1, 29, -1}
            },
    });
    hh.solve();

  }

  public static void test19() {
    debug = true;
    Hedgehog hh = new Hedgehog(new int[][][] {
            new int[][] {
                    new int[] {32, -1, -1},
                    new int[] {36,  4, 18},
                    new int[] { 8, 24, 28}
            },
            new int[][] {
                    new int[] {11, 13, -1},
                    new int[] { 1, -1,  5},
                    new int[] {27, 29,  7}
            },
            new int[][] {
                    new int[] {-1, -1, 17},
                    new int[] { 9, 23, 15},
                    new int[] {33, 25, 19}
            },
            new int[][] {
                    new int[] { 2, 30,  6},
                    new int[] {-1, -1, -1},
                    new int[] {26, 34, -1}
            },
    });
    hh.solve();

  }

}
