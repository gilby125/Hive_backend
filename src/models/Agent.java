package models;

import utils.Constants;
import utils.Pair;

import java.util.Scanner;

public class Agent {

    //
    // Constants & Enums
    //

    // Agent status
    enum Status {
        READY,
        ACTIVE,
        CHARGING,
        OUT_OF_SERVICE
    }

    // ===============================================================================================
    //
    // Member Variables
    //

    private int mId;
    private int mPriority;
    private int mRow, mCol;

    private int mCapacity;
    private int mChargeMaxCap;
    private int mChargeLevel;

    private Status mStatus;

    // ===============================================================================================
    //
    // Static Functions
    //

    public static Agent create(Scanner reader) {
        Agent ret = new Agent();
        ret.setup(reader);
        return ret;
    }

    // ===============================================================================================
    //
    // Public Member Functions
    //

    public Agent() {

    }

    public Agent(int id, int row, int col) {
        this.mId = id;
        this.mRow = row;
        this.mCol = col;

        this.mPriority = 0;

        this.mCapacity = Constants.AGENT_DEFAULT_CAPACITY;
        this.mChargeMaxCap = Constants.AGENT_DEFAULT_CHARGE_CAPACITY;
        this.mChargeLevel = 0;

        this.mStatus = Status.OUT_OF_SERVICE;
    }

    public void setup(Scanner reader) {
        mId = reader.nextInt();
        mRow = reader.nextInt();
        mCol = reader.nextInt();
    }

    // ===============================================================================================
    //
    // Public Getters & Setters
    //

    public int getId() {
        return this.mId;
    }

    public int getPriority() {
        return this.mPriority;
    }

    public Pair getPosition() {
        return new Pair(this.mRow, this.mCol);
    }

    public int getCapacity() {
        return this.mCapacity;
    }

    public int getChargeLevel() {
        return this.mChargeLevel;
    }

    public Status getStatus() {
        return this.mStatus;
    }
}
