package models.hive;

import models.map.Position;


/**
 * This {@code HiveObject} class is the base class of all the basic terminal Hive System's components
 * such as {@code Agent}, {@code Rack}, {@code Gate}, {@code ChargingStation}, ..etc.
 */
public class HiveObject {

    //
    // Member Variables
    //

    /**
     * The id of this Hive object.
     */
    protected int id;

    /**
     * The position of an object in terms of row, column pairs.
     */
    protected int row, col;

    // ===============================================================================================
    //
    // Member Methods
    //

    /**
     * Constructs a new Hive object.
     *
     * @param id the id of the Hive object.
     */
    public HiveObject(int id) {
        this.id = id;
    }

    /**
     * Constructs a new Hive object.
     *
     * @param id  the id of the Hive object.
     * @param row the row position of the Hive object.
     * @param id  the column position of the Hive object.
     */
    public HiveObject(int id, int row, int col) {
        this.id = id;
        this.row = row;
        this.col = col;
    }

    /**
     * Returns the id of this Hive object.
     *
     * @return an integer unique id of this Hive object.
     */
    public int getId() {
        return this.id;
    }

    /**
     * Returns the row position of the Hive object in the map's grid.
     *
     * @return an integer representing the row position of this Hive object.
     */
    public int getRow() {
        return this.row;
    }

    /**
     * Returns the column position of the Hive object in the map's grid.
     *
     * @return an integer representing the column position of this Hive object.
     */
    public int getCol() {
        return this.col;
    }

    /**
     * Returns the position of the position of the Hive object in the map's grid.
     *
     * @return a {@code Position} object holding the coordinates of this Hive object.
     */
    public Position getPosition() {
        return new Position(this.row, this.col);
    }
}
