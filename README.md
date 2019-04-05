# Hedgehog Solver

Solves the hedgehog logic puzzle of
- orchard type (4 zones - hedgehog can move clockwise or counter-clockwise)
- city type (4 zones - same as orchard mode, but hedgehog can take the 'tube' to the diaganal zone's block with the same row and column)


Pass type (default is 'orchard') and in zones as `int[][]` objects in 'left -> right, top -> down' order.  Blank spaces should be denoted with the int `-1`
Then run `solve()` to print full solution (in the same manner as it was inputted).
There are many static examples in the repository

There is a static `debug` variable that can be set to true to inspect the workings of the solving engine
