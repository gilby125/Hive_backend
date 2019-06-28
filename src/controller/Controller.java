package controller;

import communicators.CommunicationListener;
import communicators.frontend.FrontendCommunicator;
import communicators.frontend.FrontendConstants;
import communicators.hardware.HardwareCommunicator;
import communicators.hardware.HardwareConstants;

import models.agents.Agent;
import models.agents.AgentListener;
import models.items.Item;
import models.tasks.Task;
import models.tasks.orders.Order;
import models.tasks.orders.OrderListener;
import models.warehouses.Warehouse;

import utils.Constants;
import utils.Constants.*;

import java.util.Collection;
import java.util.Map;


public class Controller implements CommunicationListener, AgentListener, OrderListener {

    //
    // Member Variables
    //

    /**
     * The current state of this {@code Controller} object.
     */
    private ServerState currentState = ServerState.IDLE;

    /**
     * The running mode of this {@code Controller} object. Either simulation or deployment.
     */
    private RunningMode currentMode;

    /**
     * The {@code Warehouse} object.
     */
    private final Warehouse warehouse = Warehouse.getInstance();

    /**
     * The {@code FrontendCommunicator} object.
     */
    private FrontendCommunicator frontendComm;

    /**
     * The {@code HardwareCommunicator} object.
     */
    private HardwareCommunicator hardwareComm;

    /**
     * Object used to lock threads from modifying
     * the state/mode of this {@code Controller} simultaneously.
     */
    private final Object lock = new Object();

    // ===============================================================================================
    //
    // Member Methods
    //

    /**
     * Constructs a new {@code Controller} object.
     */
    public Controller() {
        frontendComm = new FrontendCommunicator(Constants.FRONTEND_COMM_PORT, this);
        hardwareComm = new HardwareCommunicator(Constants.HARDWARE_COMM_PORT, this);
    }

    /**
     * Starts and initializes this {@code Controller} object.
     */
    public void start() {
        frontendComm.start();

        synchronized (warehouse) {
            while (true) {
                run();
            }
        }
    }

    /**
     * Keeps running this {@code Controller} object until an exit criterion is met.
     */
    private void run() {
        // Must be in RUNNING state
        while (getState() != ServerState.RUNNING) {
            waitOnWarehouse();
        }

        // Check if last time step has been completed
        while (!isLastStepCompleted()) {
            waitOnWarehouse();
        }

        // Try running a single time step in the warehouse
        try {
            if (warehouse.run()) {
                System.out.println(warehouse);
            } else {
                waitOnWarehouse();
            }
        } catch (Exception ex) {
            onStop();
            frontendComm.sendErr(FrontendConstants.ERR_SERVER, "Internal server error.");
            System.err.println(ex.getMessage());
            ex.printStackTrace();
        }
    }

    /**
     * Blocks the current thread on the singleton {@code Warehouse} object.
     * <p>
     * To be called from the main thread only.
     */
    private void waitOnWarehouse() {
        try {
            warehouse.wait();
        } catch (InterruptedException ex) {
            System.err.println(ex.getMessage());
            ex.printStackTrace();
        }
    }

    /**
     * Checks whether the last time step has been completed by
     * the frontend and the hardware.
     *
     * @return {@code true} if last time step has been completed; {@code false} otherwise.
     */
    private boolean isLastStepCompleted() {
        if (getMode() == RunningMode.DEPLOYMENT) {
            return frontendComm.isLastStepCompleted() && hardwareComm.isLastStepCompleted();
        } else {
            return frontendComm.isLastStepCompleted();
        }
    }

    // ===============================================================================================
    //
    // Communicator Listener Methods
    //

    /**
     * Returns the current state of this {@code Controller} object.
     *
     * @return the current {@code ServerState} of this {@code Controller}.
     */
    @Override
    public ServerState getState() {
        synchronized (lock) {
            return currentState;
        }
    }

    /**
     * Updates the current state of this {@code Controller} object.
     *
     * @param state the new {@code ServerState} to set.
     */
    private void setState(ServerState state) {
        synchronized (lock) {
            currentState = state;
        }
    }

    /**
     * Returns the current running mode of this {@code Controller} object.
     *
     * @return the current {@code RunningMode} of this {@code Controller}.
     */
    @Override
    public RunningMode getMode() {
        synchronized (lock) {
            return currentMode;
        }
    }

    /**
     * Updates the current running mode of this {@code Controller} object.
     *
     * @param mode the new {@code RunningMode} to set.
     */
    private void setMode(RunningMode mode) {
        synchronized (lock) {
            currentMode = mode;
        }
    }

    /**
     * Called when the frontend communicator receives a start message.
     *
     * @param mode the {@code RunningMode} of this new start.
     */
    @Override
    public void onStart(RunningMode mode) {
        setState(ServerState.RUNNING);
        setMode(mode);

        synchronized (warehouse) {
            Collection<Agent> agents = warehouse.getAgentList();

            for (Agent agent : agents) {
                agent.setListener(this);
            }

            if (mode == RunningMode.DEPLOYMENT) {
                for (Agent agent : agents) {
                    hardwareComm.registerAgent(agent);
                }

                hardwareComm.start();
                hardwareComm.configure();
            }

            warehouse.print();  // DEBUG
            warehouse.notify();
        }
    }

    /**
     * Called when the frontend communicator receives a stop message.
     */
    @Override
    public void onStop() {
        setState(ServerState.IDLE);

        if (getMode() == RunningMode.DEPLOYMENT) {
            synchronized (warehouse) {
                hardwareComm.close();
            }
        }
    }

    /**
     * Called when the frontend communicator receives a pause message.
     */
    @Override
    public void onPause() {
        setState(ServerState.PAUSE);

        if (getMode() == RunningMode.DEPLOYMENT) {
            synchronized (warehouse) {
                hardwareComm.pause();
            }
        }
    }

    /**
     * Called when the frontend communicator receives a resume message.
     */
    @Override
    public void onResume() {
        setState(ServerState.RUNNING);

        synchronized (warehouse) {
            if (getMode() == RunningMode.DEPLOYMENT) {
                hardwareComm.resume();
            }

            warehouse.notify();
        }
    }

    /**
     * Called when the communicator receives DONE on all the sent actions.
     */
    @Override
    public void onActionsDone() {
        if (isLastStepCompleted()) {
            synchronized (warehouse) {
                warehouse.notify();
            }
        }
    }

    /**
     * Called when the frontend communicator receives a new {@code Order}.
     *
     * @param order the newly issued {@code order}.
     */
    @Override
    public void onOrderIssued(Order order) {
        synchronized (warehouse) {
            // DEBUG
            System.out.println("Received " + order);
            System.out.println();

            order.setListener(this);
            warehouse.addOrder(order);
            warehouse.notify();
        }
    }

    /**
     * Called when the frontend or the hardware communicators receives an {@code Agent} activation.
     *
     * @param agent the activated {@code Agent}.
     */
    @Override
    public void onAgentActivated(Agent agent) {
        synchronized (warehouse) {
            // DEBUG
            System.out.println("Activating agent-" + agent.getId() + ".");
            System.out.println();

            agent.activate();
            warehouse.notify();
        }
    }

    /**
     * Called when the frontend or the hardware communicators receives an {@code Agent} deactivation.
     *
     * @param agent the deactivated {@code Agent}.
     */
    @Override
    public void onAgentDeactivated(Agent agent) {
        synchronized (warehouse) {
            // DEBUG
            System.out.println("Deactivating agent-" + agent.getId() + ".");
            System.out.println();

            agent.deactivate();
            warehouse.notify();
        }
    }

    /**
     * Called when the hardware communicator receives a change in the battery level
     * of an {@code Agent}.
     *
     * @param agent the {@code Agent}.
     * @param level the new battery level of this {@code Agent}.
     */
    @Override
    public void onAgentBatteryLevelChanged(Agent agent, int level) {
        synchronized (warehouse) {
            agent.setBatteryLevel(level);
        }
    }

    // ===============================================================================================
    //
    // Agent Listener Methods
    //

    /**
     * Called when an {@code Agent} has performed an action.
     *
     * @param agent  the {@code Agent}.
     * @param action the action done by this {@code Agent}.
     */
    @Override
    public void onAction(Agent agent, AgentAction action) {
        frontendComm.sendAgentAction(agent, action);

        if (getMode() == RunningMode.DEPLOYMENT) {
            hardwareComm.sendAgentAction(agent, action);
        }
    }

    /**
     * Called when an {@code Agent} has recovered from a blockage state.
     *
     * @param agent  the {@code Agent}.
     * @param action the action done by this {@code Agent}.
     */
    @Override
    public void onRecover(Agent agent, AgentAction action) {
        frontendComm.sendAgentRecoverAction(agent, action);

        if (getMode() == RunningMode.DEPLOYMENT) {
            hardwareComm.sendAgentRecoverAction(agent, action);
        }
    }

    /**
     * Called when the battery level of an {@code Agent} has changed.
     *
     * @param agent the {@code Agent}.
     * @param level the new battery level of this {@code Agent}.
     */
    @Override
    public void onBatteryLevelChange(Agent agent, int level) {
        frontendComm.sendAgentBatteryUpdatedLog(agent);
    }

    /**
     * Called when an {@code Agent} has been activated.
     *
     * @param agent the activated {@code Agent}.
     */
    @Override
    public void onActivate(Agent agent) {
        frontendComm.sendAgentControl(agent, false);
    }

    /**
     * Called when an {@code Agent} has been deactivated.
     *
     * @param agent the deactivated {@code Agent}.
     */
    @Override
    public void onDeactivate(Agent agent) {
        frontendComm.sendAgentControl(agent, true);
    }

    /**
     * Called when an {@code Agent} has been blocked.
     *
     * @param agent the blocked {@code Agent}.
     */
    @Override
    public void onBlock(Agent agent) {
        frontendComm.sendAgentStop(agent);

        if (getMode() == RunningMode.DEPLOYMENT) {
            hardwareComm.sendAgentStop(agent);
        }
    }

    /**
     * Called when a {@code Task} has been assigned to an {@code Agent}.
     *
     * @param agent the {@code Agent} assigned the task.
     * @param task  the assigned {@code Task}.
     */
    @Override
    public void onTaskAssign(Agent agent, Task task) {
        if (getMode() == RunningMode.DEPLOYMENT) {
            hardwareComm.sendAgentLightCommand(agent, HardwareConstants.LIGHT_BLUE, HardwareConstants.LIGHT_MODE_FLASH);
        }
    }

    /**
     * Called when an assigned {@code Task} to an {@code Agent} has been completed.
     *
     * @param agent the {@code Agent} assigned the task.
     * @param task  the completed {@code Task}.
     */
    @Override
    public void onTaskComplete(Agent agent, Task task) {
        if (getMode() == RunningMode.DEPLOYMENT) {
            hardwareComm.sendAgentLightCommand(agent, HardwareConstants.LIGHT_BLUE, HardwareConstants.LIGHT_MODE_OFF);
        }
    }

    // ===============================================================================================
    //
    // Order Listener Methods
    //

    // All these functions are being called from the controller main thread (i.e. Main Thread)

    /**
     * Called when an {@code Order} has just been started.
     * That, is when it is assigned its first sub task.
     *
     * @param order the started {@code Order}.
     */
    @Override
    public void onStart(Order order) {

    }

    /**
     * Called when a {@code Task} has been assigned to an {@code Order}.
     *
     * @param order the {@code Order}.
     * @param task  the assigned {@code Task}.
     */
    @Override
    public void onTaskAssign(Order order, Task task) {
        frontendComm.sendTaskAssignedLog(order, task);
    }

    /**
     * Called when an assigned {@code Task} for an {@code Order} has been completed.
     *
     * @param order the {@code Order}.
     * @param task  the completed {@code Task}.
     * @param items the map of add/removed items by the completed {@code Task}.
     */
    @Override
    public void onTaskComplete(Order order, Task task, Map<Item, Integer> items) {
        frontendComm.sendTaskCompletedLog(order, task, items);
    }

    /**
     * Called when an {@code Order} has just been fulfilled.
     * That, is when its last assigned sub task has been completed.
     *
     * @param order the fulfilled {@code Order}.
     */
    @Override
    public void onFulfill(Order order) {
        frontendComm.sendOrderFulfilledLog(order);
    }

    /**
     * Called when an {@code Order} has been dismissed from the system.
     *
     * @param order the dismissed {@code Order}.
     */
    @Override
    public void onDismiss(Order order) {

    }
}
