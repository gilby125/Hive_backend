package models.map;

import utils.Constants.*;


/**
 * This {@code GuideCell} class represents a cell in the guide map associated with
 * a target {@code DstHiveObject}.
 * <p>
 * A guide cell guides the agent towards its associated target.
 */
public class GuideCell {

    /**
     * The distance to reach the associated target {@code DstHiveObject}.
     */
    public int distance;

    /**
     * The direction to reach the associated target {@code DstHiveObject}.
     */
    public Direction direction;

    /**
     * Allocates and initializes a 2D array of {@code GuideCell}.
     *
     * @param n the first dimension of the array.
     * @param m the second dimension of the array.
     *
     * @return the allocated array.
     */
    public static GuideCell[][] allocate2D(int n, int m) {
        GuideCell[][] ret = new GuideCell[n][m];

        for (int i = 0; i < n; ++i) {
            for (int j = 0; j < m; ++j) {
                ret[i][j] = new GuideCell();
            }
        }

        return ret;
    }

    /**
     * Constructs a new guide cell.
     */
    public GuideCell() {
        this.distance = Integer.MAX_VALUE;
        this.direction = Direction.STILL;
    }

    /**
     * Constructs a new guide cell.
     *
     * @param distance  the distance to reach the target.
     * @param direction the direction to reach the target.
     */
    public GuideCell(int distance, Direction direction) {
        this.distance = distance;
        this.direction = direction;
    }

    /**
     * Sets the guides of this cell.
     *
     * @param distance  the distance to reach the target.
     * @param direction the direction to reach the target.
     */
    public void set(int distance, Direction direction) {
        this.distance = distance;
        this.direction = direction;
    }

    /**
     * Checks whether the target is reachable from this cell and vice versa.
     *
     * @return {@code true} if the target is reachable, {@code false} otherwise.
     */
    public boolean isReachable() {
        return this.distance < Integer.MAX_VALUE;
    }

    /**
     * Checks whether the target is unreachable from this cell and vice versa.
     *
     * @return {@code true} if the target is unreachable, {@code false} otherwise.
     */
    public boolean isUnreachable() {
        return this.distance == Integer.MAX_VALUE;
    }

    /**
     * Returns a string representation of object.
     * In general, the toString method returns a string that "textually represents" this object.
     *
     * @return a string representation of object.
     */
    @Override
    public String toString() {
        return ("Guide Cell: { Dis: " + distance + ", Dir: " + direction + " }");
    }
}
