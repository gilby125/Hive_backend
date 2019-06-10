package server;

import models.agents.Agent;

import utils.Constants;
import utils.Constants.*;

import org.json.JSONArray;
import org.json.JSONObject;


/**
 * This {@code ServerDecoder} class contains useful static functions to encode
 * messages from the backend to be sent to the frontend.
 */
public class ServerEncoder {

    public static JSONObject encodeUpdateMsg(long time, JSONArray actions, JSONArray logs, JSONArray statistics) {
        JSONObject data = new JSONObject();
        data.put(ServerConstants.KEY_TIME_STEP, time);
        data.put(ServerConstants.KEY_ACTIONS, actions);
        data.put(ServerConstants.KEY_LOGS, logs);
        data.put(ServerConstants.KEY_STATISTICS, statistics);
        return encodeMsg(ServerConstants.TYPE_UPDATE, data);
    }

    public static JSONObject encodeAgentAction(Agent agent, AgentAction action) {
        JSONObject data = new JSONObject();
        data.put(ServerConstants.KEY_ID, agent.getId());
        data.put(ServerConstants.KEY_ROW, agent.getRow());
        data.put(ServerConstants.KEY_COL, agent.getCol());
        return encodeMsg(ServerUtility.actionToServerType(action), data);
    }

    public static JSONObject encodeStatistics(int key, double value) {
        JSONObject ret = new JSONObject();
        ret.put(ServerConstants.KEY_TYPE, key);
        ret.put(ServerConstants.KEY_DATA, value);
        return ret;
    }

    public static JSONObject encodeAckMsg(int type, int status, String msg) {
        JSONObject data = new JSONObject();
        data.put(ServerConstants.KEY_STATUS, status);
        data.put(ServerConstants.KEY_MSG, msg);
        return encodeMsg(type, data);
    }

    public static JSONObject encodeMsg(int type, JSONObject data) {
        JSONObject ret = new JSONObject();
        ret.put(ServerConstants.KEY_TYPE, type);
        ret.put(ServerConstants.KEY_DATA, data);
        return ret;
    }
}