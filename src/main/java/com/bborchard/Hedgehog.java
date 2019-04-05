package com.bborchard;

import static com.bborchard.Hedgehog.Alignment.COL;
import static com.bborchard.Hedgehog.Alignment.ROW;
import static com.bborchard.Hedgehog.Alignment.TUBE;
import static com.bborchard.Hedgehog.Type.CITY;
import static com.bborchard.Hedgehog.Type.ORCHARD;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Stack;

public class Hedgehog {

  private static boolean debug = false;

  private Stack<TreeRow> possibilityTree;
  private Map<Integer, Block> numberIndex;
  private Block[][][] positionIndex;
  private Map<Integer, Adjacency[]> adjacencyMap;
  private Block root;
  private Type type;
  private static final Iterator<Block> NO_BLOCKS =
      new Iterator<Block>() {
        @Override
        public boolean hasNext() {
          return false;
        }

        @Override
        public Block next() {
          return null;
        }

        @Override
        public void remove() {}
      };

  private int totalChunks;
  private int totalRows;
  private int totalCols;
  private int totalBlocks;

  public Hedgehog(int[][][] puzzle) {
    this(ORCHARD, puzzle);
  }

  public Hedgehog(Type type, int[][][] puzzle) {
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
          if (num != -1) {
            numberIndex.put(num, block);
          }
          if (num == 1) {
            root = block;
          }
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
      if (type == CITY) {
        // make space for a new adjacency
        for (int i = 0; i < totalChunks; i++) {
          adjacencyMap.put(i, Arrays.copyOf(adjacencyMap.get(i), 3));
        }
        adjacencyMap.get(0)[2] = new Adjacency(3, TUBE);
        adjacencyMap.get(1)[2] = new Adjacency(2, TUBE);
        adjacencyMap.get(2)[2] = new Adjacency(1, TUBE);
        adjacencyMap.get(3)[2] = new Adjacency(0, TUBE);
      }
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
      }
      debug(
          "no additional possibilities for block %s - backtracking",
          possibilityTree.peek().block.num);
      if (solved()) {
        debug("solved");
        break;
      }

      // dead end - back out of last possiblity
      Block lastPossibility = possibilityTree.pop().block;
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
    return possibilityTree.peek().block.num == totalBlocks
        && adjacent(root, numberIndex.get(numberIndex.size()));
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

    debug("looking for possibilities for block %s", block.num);
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
        if (adjacency.alignment == TUBE) {
          // Check the tube if that is the aligment
          Block possibility =
              positionIndex[adjacency.chunk][block.location.row][block.location.col];
          if (possibility.num == -1) {
            possibilities.add(possibility);
          }
        } else {
          // Go through row and col adjacency alignments
          for (int i = 0; i < (adjacency.alignment == ROW ? totalCols : totalRows); i++) {
            Block possibility =
                adjacency.alignment == ROW
                    ? positionIndex[adjacency.chunk][block.location.row][i]
                    : positionIndex[adjacency.chunk][i][block.location.col];
            if (possibility.num == -1) {
              possibilities.add(possibility);
            }
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
        if (adjacency.alignment == ROW) {
          adjacent |= a.location.row == b.location.row;
        } else if (adjacency.alignment == COL) {
          adjacent |= a.location.col == b.location.col;
        } else {
          // TUBE requires adjacency on same row and col
          adjacent |= (a.location.row == b.location.row && a.location.col == b.location.col);
        }
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
    COL,
    TUBE // only for the city
  }

  protected enum Type {
    ORCHARD,
    CITY
  }

  public static void debug(String message, Object... params) {
    if (debug) {
      System.out.println(String.format(message, params));
    }
  }

  public static void main(String[] args) {
    test36();
  }

  public static void test36() {
    Hedgehog hh =
        new Hedgehog(
            CITY,
            new int[][][] {
              new int[][] {
                new int[] {51, 42, -1, 2, -1},
                new int[] {-1, 85, 74, 46, -1},
                new int[] {53, 38, -1, 68, 94},
                new int[] {55, -1, -1, 89, -1},
                new int[] {29, 62, -1, 35, 96}
              },
              new int[][] {
                new int[] {1, 5, 19, 7, -1},
                new int[] {-1, 73, -1, 9, 45},
                new int[] {57, -1, -1, -1, 67},
                new int[] {49, -1, 12, -1, 90},
                new int[] {-1, -1, 30, 97, -1}
              },
              new int[][] {
                new int[] {80, 41, 20, -1, 95},
                new int[] {-1, -1, -1, 26, -1},
                new int[] {56, 61, -1, 36, 93},
                new int[] {-1, 84, 11, 34, -1},
                new int[] {52, 63, 99, 65, 14}
              },
              new int[][] {
                new int[] {50, -1, 31, -1, 79},
                new int[] {27, 72, 18, 25, 16},
                new int[] {48, 37, -1, -1, 44},
                new int[] {-1, -1, -1, -1, -1},
                new int[] {100, -1, 13, 98, -1}
              }
            });
    hh.solve();
  }

  public static void test35() {
    Hedgehog hh =
        new Hedgehog(
            CITY,
            new int[][][] {
              new int[][] {
                new int[] {9, 19, 53, -1},
                new int[] {-1, -1, -1, 44},
                new int[] {-1, 17, -1, -1},
                new int[] {-1, 61, 57, -1}
              },
              new int[][] {
                new int[] {-1, -1, 64, 8},
                new int[] {-1, -1, 4, -1},
                new int[] {-1, -1, 2, 47},
                new int[] {-1, 14, 62, -1}
              },
              new int[][] {
                new int[] {24, -1, 32, 30},
                new int[] {10, -1, 36, 12},
                new int[] {-1, 60, 58, -1},
                new int[] {40, 20, -1, -1}
              },
              new int[][] {
                new int[] {31, -1, 25, -1},
                new int[] {-1, -1, 63, 48},
                new int[] {42, 59, -1, 7},
                new int[] {-1, -1, 1, -1}
              },
            });
    hh.solve();
  }

  public static void test34() {
    Hedgehog hh =
        new Hedgehog(
            CITY,
            new int[][][] {
              new int[][] {
                new int[] {34, 51, -1, -1},
                new int[] {2, -1, 4, -1},
                new int[] {31, 59, 57, 61},
                new int[] {39, -1, -1, 37}
              },
              new int[][] {
                new int[] {-1, 18, -1, 13},
                new int[] {1, -1, 5, 3},
                new int[] {58, 60, 62, 30},
                new int[] {36, 54, -1, 28}
              },
              new int[][] {
                new int[] {40, 15, 43, -1},
                new int[] {64, 48, -1, -1},
                new int[] {33, -1, 11, 7},
                new int[] {22, -1, -1, 24}
              },
              new int[][] {
                new int[] {35, -1, -1, 14},
                new int[] {49, -1, 63, 27},
                new int[] {-1, 8, -1, -1},
                new int[] {-1, -1, 23, -1}
              },
            });
    hh.solve();
  }

  public static void test33() {
    debug = true;
    Hedgehog hh =
        new Hedgehog(
            CITY,
            new int[][][] {
              new int[][] {
                new int[] {-1, -1, -1},
                new int[] {8, -1, 23},
                new int[] {30, 26, 33}
              },
              new int[][] {
                new int[] {-1, -1, 35},
                new int[] {-1, -1, -1},
                new int[] {-1, 17, 31}
              },
              new int[][] {
                new int[] {-1, -1, 5},
                new int[] {1, -1, 15},
                new int[] {29, -1, 32}
              },
              new int[][] {
                new int[] {-1, -1, -1},
                new int[] {9, -1, 36},
                new int[] {-1, 28, 34}
              },
            });
    hh.solve();
  }

  public static void test24() {
    Hedgehog hh =
        new Hedgehog(
            new int[][][] {
              new int[][] {
                new int[] {20, 10, -1, 66, -1},
                new int[] {38, 12, 84, 14, -1},
                new int[] {40, 70, 92, -1, -1},
                new int[] {36, 98, 6, -1, 50},
                new int[] {-1, -1, 90, -1, 28}
              },
              new int[][] {
                new int[] {61, 21, 19, 9, 67},
                new int[] {-1, 95, -1, 75, 83},
                new int[] {79, 53, 17, -1, 41},
                new int[] {-1, -1, -1, 7, -1},
                new int[] {31, -1, -1, -1, -1}
              },
              new int[][] {
                new int[] {-1, 99, -1, 1, 59},
                new int[] {-1, -1, 23, -1, -1},
                new int[] {37, 69, 5, -1, -1},
                new int[] {-1, 97, -1, -1, 47},
                new int[] {-1, 71, 93, 65, -1}
              },
              new int[][] {
                new int[] {58, 52, 100, -1, 26},
                new int[] {32, 22, 88, 74, 34},
                new int[] {-1, -1, -1, 76, 68},
                new int[] {62, 96, 18, 46, 48},
                new int[] {30, 94, -1, -1, -1}
              }
            });
    hh.solve();
  }

  public static void test23() {
    Hedgehog hh =
        new Hedgehog(
            new int[][][] {
              new int[][] {
                new int[] {-1, -1, -1, 27},
                new int[] {-1, -1, -1, -1},
                new int[] {55, 57, 5, 59},
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
                new int[] {24, 8, -1, -1},
                new int[] {-1, -1, 34, 60},
                new int[] {-1, -1, -1, -1}
              },
              new int[][] {
                new int[] {21, 45, 43, 41},
                new int[] {51, 9, 49, -1},
                new int[] {-1, -1, 15, 1},
                new int[] {53, -1, -1, 29}
              },
            });
    hh.solve();
  }

  public static void test22() {
    Hedgehog hh =
        new Hedgehog(
            new int[][][] {
              new int[][] {
                new int[] {59, -1, -1, -1},
                new int[] {61, 3, 39, 1},
                new int[] {57, -1, 41, 55},
                new int[] {31, 45, -1, -1}
              },
              new int[][] {
                new int[] {34, 36, -1, 16},
                new int[] {62, -1, 28, 2},
                new int[] {-1, -1, -1, -1},
                new int[] {44, 52, 24, 20}
              },
              new int[][] {
                new int[] {60, 4, 40, -1},
                new int[] {-1, -1, 38, -1},
                new int[] {58, 46, -1, 22},
                new int[] {-1, 14, -1, 64}
              },
              new int[][] {
                new int[] {-1, -1, -1, -1},
                new int[] {33, 11, 25, 17},
                new int[] {7, -1, -1, -1},
                new int[] {63, -1, 29, -1}
              },
            });
    hh.solve();
  }

  public static void test19() {
    debug = true;
    Hedgehog hh =
        new Hedgehog(
            new int[][][] {
              new int[][] {
                new int[] {32, -1, -1},
                new int[] {36, 4, 18},
                new int[] {8, 24, 28}
              },
              new int[][] {
                new int[] {11, 13, -1},
                new int[] {1, -1, 5},
                new int[] {27, 29, 7}
              },
              new int[][] {
                new int[] {-1, -1, 17},
                new int[] {9, 23, 15},
                new int[] {33, 25, 19}
              },
              new int[][] {
                new int[] {2, 30, 6},
                new int[] {-1, -1, -1},
                new int[] {26, 34, -1}
              },
            });
    hh.solve();
  }
}
