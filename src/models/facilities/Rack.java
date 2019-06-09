package models.facilities;

import models.agents.Agent;
import models.items.Item;
import models.items.QuantityAddable;
import models.items.QuantityReservable;
import models.warehouses.Warehouse;

import server.Server;

import utils.Constants;
import utils.Constants.*;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;


/**
 * This {@code Rack} class is a one of the {@link Facility} components
 * in our Hive Warehouse System.
 * <p>
 * A rack component is a container located in the {@link models.warehouses.Warehouse Warehouse} grid
 * where {@link Item Items} are stored.
 * A rack can possibly contain different items with different quantities.
 * <p>
 * A rack by itself is a static {@link Facility} component,
 * but it is different from other facilities in the point that it can be loaded and be moved around by
 * an {@link Agent}.
 *
 * @see models.HiveObject HiveObject
 * @see models.agents.Agent Agent
 * @see models.facilities.Facility Facility
 * @see models.facilities.Gate Gate
 * @see models.facilities.Station Station
 * @see models.tasks.Task Task
 */
public class Rack extends Facility implements QuantityAddable<Item>, QuantityReservable<Item> {

    //
    // Member Variables
    //

    /**
     * The maximum storing weight of the {@code Rack}.
     */
    private int capacity = Constants.RACK_DEFAULT_STORE_CAPACITY;

    /**
     * The weight of the {@code Rack} itself when being empty.
     */
    private int containerWeight = Constants.RACK_DEFAULT_CONTAINER_WEIGHT;

    /**
     * The total stored weight of all the items in the {@code Rack}.
     */
    private int storedWeight;

    /**
     * The map of available items this {@code Rack} is storing.<p>
     * The key is an {@code Item}.<p>
     * The mapped value represents the quantity of this {@code Item}.
     */
    private Map<Item, Integer> items = new HashMap<>();

    // ===============================================================================================
    //
    // Static Methods
    //

    /**
     * Creates a new {@code Rack} object from JSON data.
     * <p>
     * TODO: add checks and throw exceptions
     *
     * @param data the un-parsed JSON data.
     * @param row  the row position of the {@code MapCell} to create.
     * @param col  the column position of the {@code MapCell} to create.
     *
     * @return an {@code Rack} object.
     */
    public static Rack create(JSONObject data, int row, int col) throws Exception {
        Rack ret = new Rack();

        ret.capacity = data.getInt(Constants.MSG_KEY_RACK_CAPACITY);
        ret.setPosition(row, col);

        JSONArray itemsJSON = data.getJSONArray(Constants.MSG_KEY_ITEMS);

        for (int i = 0; i < itemsJSON.length(); ++i) {
            JSONObject itemJSON = itemsJSON.getJSONObject(i);

            int itemId = itemJSON.getInt(Constants.MSG_KEY_ID);
            int quantity = itemJSON.getInt(Constants.MSG_KEY_ITEM_QUANTITY);
            Item item = Warehouse.getInstance().getItemById(itemId);

            if (quantity < 0) {
                throw new Exception("Invalid quantity to add to the rack!");
            }

            if (item == null) {
                throw new Exception("Invalid item to add to the rack!");
            }

            ret.add(item, quantity);
        }

        return ret;
    }

    // ===============================================================================================
    //
    // Member Methods
    //

    /**
     * Constructs a new {@code Rack} object.
     */
    public Rack() {
        super();
    }

    /**
     * Constructs a new {@code Rack} object.
     *
     * @param id              the id of the {@code Rack}.
     * @param capacity        the maximum storing weight of the {@code Rack}.
     * @param containerWeight the weight of the {@code Rack}'s container.
     */
    public Rack(int id, int capacity, int containerWeight) {
        super(id);
        this.capacity = capacity;
        this.containerWeight = containerWeight;
    }

    /**
     * Returns the maximum storing weight of this {@code Rack}.
     *
     * @return the storing capacity of the {@code Rack}.
     */
    public int getCapacity() {
        return capacity;
    }

    /**
     * Returns the weight of the {@code Rack} itself when being empty.
     *
     * @return the weight of the {@code Rack}'s container.
     */
    public int getContainerWeight() {
        return containerWeight;
    }

    /**
     * Returns the stored weight in this {@code Rack}.
     *
     * @return the current stored weight of this {@code Rack}.
     */
    public int getStoredWeight() {
        return storedWeight;
    }

    /**
     * Returns the total weight of this {@code Rack}.
     * <p>
     * That is, the weight of the container plus the total weight of the stored units.
     *
     * @return the current total weight of this {@code Rack}.
     */
    public int getTotalWeight() {
        return containerWeight + storedWeight;
    }

    /**
     * Returns the current quantity of an {@code Item} in this {@code Rack}.
     *
     * @param item the {@code Item} to get its quantity.
     *
     * @return the quantity of the given {@code Item}.
     */
    @Override
    public int get(Item item) {
        return items.getOrDefault(item, 0);
    }

    /**
     * Adds or removes some units of an {@code Item} into/from this {@code Rack}.
     * <p>
     * This function is used to add extra units of the given {@code Item} if the given
     * quantity is positive,
     * and used to remove existing units if the given quantity is negative.
     * <p>
     * This function should be called with appropriate parameters so that:
     * <ol>
     * <li>the total capacity of this {@code Rack} does not exceed the maximum limit</li>
     * <li>no {@code Item} has negative number of units</li>
     * </ol>
     *
     * @param item     the {@code Item} to be updated.
     * @param quantity the quantity to be updated with.
     */
    @Override
    public void add(Item item, int quantity) {
        QuantityAddable.update(items, item, quantity);
        storedWeight += quantity * item.getWeight();
        item.add(this, quantity);
    }

    /**
     * Returns an {@code Iterator} to iterate over the available items in this {@code Rack}.
     * <p>
     * Note that this iterator should be used in read-only operations;
     * otherwise undefined behaviour could arises.
     *
     * @return an {@code Iterator}.
     */
    @Override
    public Iterator<Map.Entry<Item, Integer>> iterator() {
        return items.entrySet().iterator();
    }

    /**
     * Reserves some units specified by the given {@code QuantityAddable} container.
     * <p>
     * This functions removes some items from the rack without actually reducing
     * the weight of this {@code Rack}.
     * The items are physically removed when the reservation is confirmed.
     * <p>
     * This function should only be called once per {@code Task} activation.
     *
     * @param container the {@code QuantityAddable} container.
     *
     * @see Rack#confirmReservation(QuantityAddable)
     */
    @Override
    public void reserve(QuantityAddable<Item> container) {
        for (Map.Entry<Item, Integer> pair : container) {
            // Get item and its quantity
            Item item = pair.getKey();
            int quantity = pair.getValue();

            // Remove the specified quantity for the map of available items
            QuantityAddable.update(items, item, -quantity);
        }
    }

    /**
     * Confirms the previously assigned reservations specified by the given
     * {@code QuantityAddable} container, and removes those reserved units from this object.
     * <p>
     * This function physically removes some of the reserved items and reduces the weight
     * of this {@code Rack}.
     * <p>
     * This function should be called after reserving a same or a super container first;
     * otherwise un-expected behaviour could occur.
     * <p>
     * This function should only be called once per {@code Task} termination.
     *
     * @param container the {@code QuantityAddable} container.
     *
     * @see Rack#reserve(QuantityAddable)
     */
    @Override
    public void confirmReservation(QuantityAddable<Item> container) {
        for (Map.Entry<Item, Integer> pair : container) {
            // Get item and its quantity
            Item item = pair.getKey();
            int quantity = pair.getValue();

            // Remove item units and confirm reservation
            storedWeight -= item.getWeight() * quantity;
            item.add(this, -quantity);
            item.confirmReservation(container);
        }
    }

    /**
     * Binds this {@code Rack} with the given {@code Agent}.
     * <p>
     * It is preferable to allocate the {@code Rack} before binding it to an {@code Agent}.
     * <p>
     * This function should be called after checking that it is currently possible to bind
     * the given {@code Agent}; otherwise un-expected behaviour could occur.
     *
     * @param agent the {@code Agent} to bind.
     *
     * @see Rack#isBound()
     * @see Rack#canBind(Agent)
     * @see Rack#canUnbind()
     * @see Rack#unbind()
     */
    @Override
    public void bind(Agent agent) throws Exception {
        // Bind
        agent.loadRack(this);
        super.bind(agent);

        // Send binding to the front frontend
        Server.getInstance().sendAction(agent, AgentAction.BIND_RACK);
    }

    /**
     * Unbinds the bound {@code Agent} from this {@code Rack}.
     * <p>
     * This function should be called after checking that it is currently possible to unbind
     * the bound {@code Agent}; otherwise un-expected behaviour could occur.
     *
     * @see Rack#isBound()
     * @see Rack#canBind(Agent)
     * @see Rack#bind(Agent)
     * @see Rack#canUnbind()
     */
    @Override
    public void unbind() throws Exception {
        // Unbind
        boundAgent.offloadRack(this);
        super.unbind();

        // Send unbinding to the front frontend
        Server.getInstance().sendAction(boundAgent, AgentAction.UNBIND_RACK);
    }
}
