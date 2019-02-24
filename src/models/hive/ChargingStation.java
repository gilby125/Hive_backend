package models.hive;


/**
 * This {@code Gate} class is a model for charging station for robot agents.
 * <p>
 * A charging station is the location where robot agents go to re-charge their batteries.
 */
public class ChargingStation extends DstHiveObject {

    /**
     * Constructs a new charging station of items.
     *
     * @param id the id of the charging station.
     */
    public ChargingStation(int id) {
        super(id);
    }

    /**
     * Constructs a new charging station of items.
     *
     * @param id  the id of the charging station.
     * @param row the row position of the charging station.
     * @param col the column position of the charging station.
     */
    public ChargingStation(int id, int row, int col) {
        super(id, row, col);
    }
}
