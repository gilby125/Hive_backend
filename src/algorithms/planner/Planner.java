package algorithms.planner;

import models.agents.Agent;
import models.maps.*;
import models.maps.utils.Dimensions;
import models.maps.utils.Position;

import models.warehouses.Warehouse;
import utils.Constants;
import utils.Constants.*;
import utils.Utility;

import java.util.*;


/**
 * This {@code Planner} class contains some static method for multi-agent path planning algorithms.
 */
public class Planner {


    /**
     * Routes the given {@code Agent} one step towards its target location.
     *
     * @param agent the {@code Agent} to be routed.
     * @param map   the {@code MapGrid} of the {@code Warehouse}.
     */
    public static boolean route(Agent agent, MapGrid map) {
        return route(agent, agent, map);
    }

    /**
     * Routes the given {@code Agent} one step towards its target location.
     * <p>
     * This is a recursive function that keep displacing other agents away in order for the
     * main {@code Agent} to find its path.
     * <p>
     * This function is to be initially called passing the same {@code Agent} for both
     * parameters {@code agent} and {@code mainAgent}.
     *
     * @param agent     the {@code Agent} to be displaced for the main {@code Agent}.
     * @param mainAgent the main {@code Agent} to be routed.
     * @param map       the {@code MapGrid} of the {@code Warehouse}.
     */
    private static boolean route(Agent agent, Agent mainAgent, MapGrid map) {
        // Return if this agent has higher priority than the agent needed to be moved
        if (agent.compareTo(mainAgent) > 0) {
            return false;
        }

        // Return if this agent has already been displaced by a higher priority agent,
        // or it has been tried to displace it
        if (agent.isAlreadyMoved()) {
            return false;
        }

        // Update agent action time to avoid infinite loop
        agent.updateLastActionTime();

        // Get current agent and guide maps
        Position cur = agent.getPosition();
        GuideGrid guide = agent.getGuideMap();

        // Get list of valid directions
        List<Direction> dirs;

        if (guide != null) {
            dirs = guide.getGuideDirections(cur);
        } else {
            dirs = map.getEmptyDirections(cur);
        }

        //
        // Iterate over all guide directions
        //
        for (Direction dir : dirs) {
            // Get next position
            Position nxt = map.next(cur, dir);

            // Skip moving away from the target if this is our main agent
            if (agent == mainAgent) {
                // Guide map should never be null here
                if (guide != null && guide.getDistance(nxt) >= guide.getDistance(cur)) {
                    continue;
                }
            }

            // Get agent in the next cell
            Agent nxtAgent = map.get(nxt).getAgent();

            // Move the agent if free cell or we manged to get a blank in the next position
            if (nxtAgent == null || route(nxtAgent, mainAgent, map)) {
                agent.move(dir);
                return true;
            }
        }

        return false;
    }

    /**
     * Runs a BFS algorithms on the given grid to compute the guide maps
     * to the given destination position.
     *
     * @param map         the map grid to compute upon.
     * @param dst         the destination position.
     * @param accessTypes the accessible {@code CellType} to set.
     *
     * @return a 2D {@code GuideCell} array representing the guide map to reach the destination.
     */
    public static GuideCell[][] bfs(MapGrid map, Position dst, CellType... accessTypes) {
        // Initialize BFS algorithm requirements
        Dimensions dim = map.getDimensions();
        Queue<Position> q = new LinkedList<>();
        GuideCell[][] ret = GuideCell.allocate2D(dim.rows, dim.cols);

        // Add BFS base case
        q.add(dst);
        ret[dst.row][dst.col].setDistance(0);

        // Keep expanding all cells in the maps
        while (!q.isEmpty()) {
            // Get current node and its distance to the destination
            Position cur = q.poll();
            int dis = ret[cur.row][cur.col].getDistance();

            // Expanding in all directions
            for (Direction dir : Direction.values()) {
                // Get previous position
                Position prv = map.previous(cur, dir);

                // Continue if not accessible cell
                if (!map.isAccessible(prv, accessTypes)) {
                    continue;
                }

                // Continue if already visited
                if (ret[prv.row][prv.col].isReachable()) {
                    continue;
                }

                // Add expanded cell to the queue and update its guide values
                q.add(prv);
                ret[prv.row][prv.col].setDistance(dis + 1);
            }
        }

        return ret;
    }


    /**
     * Routes the given {@code Agent} one step towards its target location
     * using the given pre-planned action.
     *
     * @param agent  the {@code Agent} to be routed.
     * @param action the {@code AgentAction} to apply.
     */
    public static boolean route(Agent agent, AgentAction action) {
        // Global information about the warehouse
        Warehouse warehouse = Warehouse.getInstance();
        MapGrid map = warehouse.getMap();
        TimeGrid timeMap = warehouse.getTimeMap();
        long time = warehouse.getTime();

        // Agent current position information
        Position curPos = agent.getPosition();
        MapCell curCell = map.get(curPos);

        // Rotation actions are easy
        if (action != AgentAction.MOVE) {
            timeMap.clearAt(curPos, time);
            agent.move(action);
            return true;
        }

        // Agent next position information
        Position nxtPos = Utility.nextPos(curPos, agent.getDirection());
        MapCell nxtCell = map.get(nxtPos);
        Agent a = nxtCell.getAgent();

        // Check if the can move to the next cell
        if (a == null || slide(a, agent)) {
            timeMap.clearAt(curPos, time);
            curCell.setAgent(null);
            nxtCell.setAgent(agent);
            agent.setPosition(nxtPos);
            agent.move(action);
            return true;
        }

        // Cannot move the agent
        agent.dropPlan();
        return false;
    }

    /**
     * Tries sliding the given {@code Agent} in order to bring a blank location
     * for the main {@code Agent} to move into.
     *
     * @param slidingAgent the {@code Agent} to slide away.
     * @param mainAgent    the main {@code Agent}.
     *
     * @return {@code true} if sliding is done successfully; {@code false} otherwise.
     */
    private static boolean slide(Agent slidingAgent, Agent mainAgent) {
        return false;
    }

    /**
     * Plans a sequence of actions to be done by the given {@code Agent} to reach
     * its target.
     *
     * @param agent the {@code Agent} to plan for.
     * @param dst   the target position of the {@code Agent}.
     *
     * @return a sequence of {@code AgentAction}; or {@code null} if currently unreachable.
     */
    public static Stack<AgentAction> plan(Agent agent, Position dst) {
        // Initialize planning algorithm
        Warehouse warehouse = Warehouse.getInstance();
        MapGrid map = warehouse.getMap();
        TimeGrid timeMap = warehouse.getTimeMap();
        PlanNode.initializes(map.getRows(), map.getCols(), 4, dst);

        // Create the planning queue and add the initial state
        PriorityQueue<PlanNode> q = new PriorityQueue<>();
        q.add(new PlanNode(
                agent.getPosition(),
                agent.getDirection(),
                AgentAction.MOVE,       // The initial action is not significant but it cannot be AgentAction.NOTHING
                warehouse.getTime()
        ));

        //
        // Keep exploring states until the target is found
        //
        while (!q.isEmpty()) {
            // Get the current best node in the queue
            PlanNode cur = q.remove();

            // Skip visited states
            if (cur.isVisited()) {
                continue;
            }

            // Mark current state as visited
            cur.visit();

            //
            // Try exploring further states by trying all possible actions
            //
            for (AgentAction action : Constants.MOVE_ACTIONS) {
                // Get next state after doing the current action
                PlanNode nxt = cur.next(action);

                // Skip out of bound or visited states
                if (!map.isInBound(nxt.pos) || nxt.isVisited()) {
                    continue;
                }

                // Get the agent that is planned to be in this state
                Agent a = timeMap.getAgentAt(nxt.pos, nxt.time);

                // If there is an agent with higher priority then skip this state as well
                if (a != null && agent.getPriority() < a.getPriority()) {
                    continue;
                }

                // Check if target has been reached
                if (dst.equals(nxt.pos)) {
                    return constructPlan(agent, nxt);
                }

                // Skip states having facilities
                if (!map.isEmpty(nxt.pos)) {
                    continue;
                }

                // Add expanded state
                q.add(nxt);
            }
        }

        // No path has been found
        return null;
    }

    /**
     * Constructs the sequence of actions leading to the target after
     * finishing the planning, and update the timeline map of the {@code Warehouse}
     * in accordance.
     *
     * @param agent the {@code Agent} to plan for.
     * @param node  the target state {@code PlanNode}.
     *
     * @return a sequence of {@code AgentAction} to reach the given state.
     */
    private static Stack<AgentAction> constructPlan(Agent agent, PlanNode node) {
        TimeGrid timeMap = Warehouse.getInstance().getTimeMap();

        // Prepare the stack of actions
        Stack<AgentAction> ret = new Stack<>();

        //
        // Keep moving backward until reaching the position of the agent
        //
        while (true) {
            // Get the agent that is planned to be in this state
            Agent a = timeMap.getAgentAt(node.pos, node.time);

            // If there is an agent then it must be with lower priority
            // So drop its plan for now
            if (a != null) {
                a.dropPlan();
            }

            // Update timeline
            timeMap.setAgentAt(node.pos, node.time, agent);

            // Stop when reaching the initial position of the agent
            if (agent.getPosition().equals(node.pos)) {
                break;
            }

            // Add the action and proceed to previous state
            ret.add(node.action);
            node = node.previous(node.action);
        }

        return ret;
    }

    /**
     * Drops the current plan of the given {@code Agent} and updates the
     * timeline map of the {@code Warehouse} in accordance.
     *
     * @param agent   the {@code Agent} to drop its plan.
     * @param actions the plan of the agent.
     */
    public static void dropPlan(Agent agent, Stack<AgentAction> actions) {
        TimeGrid timeMap = Warehouse.getInstance().getTimeMap();

        // Create a node with the current state of the agent
        PlanNode node = new PlanNode(
                agent.getPosition(),
                agent.getDirection(),
                AgentAction.MOVE,
                Warehouse.getInstance().getTime()
        );

        //
        // Keep dropping the plan one action at a time
        //
        while (true) {
            // Clear the time slot of the agent in the current state
            timeMap.clearAt(node.pos, node.time);

            // Stop when no further actions in the plan
            if (actions.isEmpty()) {
                break;
            }

            // Go to the next state in the plan
            node = node.next(actions.pop());
        }
    }
}