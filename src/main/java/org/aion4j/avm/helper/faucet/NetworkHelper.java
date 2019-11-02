package org.aion4j.avm.helper.faucet;

import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.JsonNode;
import com.mashape.unirest.http.Unirest;
import org.aion4j.avm.helper.api.Log;
import org.aion4j.avm.helper.faucet.model.Network;
import org.aion4j.avm.helper.remote.RemoteAVMNode;
import org.aion4j.avm.helper.util.StringUtils;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class NetworkHelper {
    private final static String NETWORK_DETAILS_URL = "https://bloxbean.github.io/aion4j-release/networks.json";
    private Log log;

    public NetworkHelper(Log log) {
        this.log = log;
    }

    public Network getNetworkFromWeb3RpcUrl(String nodeUrl) {
        if(StringUtils.isEmpty(nodeUrl))
            return null;

        if(StringUtils.isEmpty(nodeUrl))
            return null;

        RemoteAVMNode remoteAVMNode = new RemoteAVMNode(nodeUrl, log);
        String genesisHash = remoteAVMNode.getGenesisBlockHash();

        if(StringUtils.isEmpty(genesisHash))
            return null;

        List<Network> networks = getNetworks();
        if(networks == null)
            return null;

        for(Network network: networks) {
            if(genesisHash.equals(network.getGenesisHash()))
                return network;
        }
        return null;
    }

    public List<Network> getNetworks() {
        JsonNode networksJson = readContentFromNetwork();
        try {
            if (networksJson != null) {
                return parseNetworks(networksJson);
            }
        } catch (Exception e) {
            //Failed to get the network details from the url. Let's try from local cache.
            log.debug("Error fetching network details : " + NETWORK_DETAILS_URL, e);
        }

        try {
            networksJson = readFromLocalResource();
            if (networksJson != null) {
                return parseNetworks(networksJson);
            }
        } catch (Exception ex) {
            log.error("Unable to get network details" , ex);
        }

        return Collections.EMPTY_LIST;
    }

    private List<Network> parseNetworks(JsonNode jsonNode) {
        JSONArray networksJson = jsonNode.getObject().getJSONArray("networks");

        List<Network> networks = new ArrayList<>();
        for(int i=0; i< networksJson.length();i++) {
            JSONObject networkNode = networksJson.getJSONObject(i);

            String id = networkNode.getString("id");
            String genesisHash = networkNode.getString("genesisHash");
            String networkDesc = networkNode.getString("network");
            String faucetContract = networkNode.getString("faucetContract");

            Network network = new Network(id, genesisHash, networkDesc, faucetContract);
            networks.add(network);
        }

        return networks;
    }

    private JsonNode readContentFromNetwork() {
        try {
            Unirest.setTimeouts(2000, 4000);
            HttpResponse<JsonNode> response = Unirest.get(NETWORK_DETAILS_URL)
                    .header("accept", "application/json")
                    .asJson();

            if(response.getStatus() != 200) {
                log.debug("Unable to fetch network details from : " + NETWORK_DETAILS_URL + "  httpstatus: " + response.getStatus());
                return null;
            }

            if (response != null)
                return response.getBody();
            else
                return null;
        } catch (Exception e) {
            log.debug("Unable to fetch network details from : " + NETWORK_DETAILS_URL, e);
            log.warn("Unable to fetch network details from : " + NETWORK_DETAILS_URL);
            log.info("Let's try to get network details from local cache");
            return null;
        }
    }

    private JsonNode readFromLocalResource() {
        InputStream inputStream = this.getClass().getResourceAsStream("/networks.json");
        String content = null;
        try {
            content = readFromInputStream(inputStream);
            JsonNode jsonNode = new JsonNode(content);
            return jsonNode;
        } catch (IOException e) {
            log.error("Error reading network details from local cache", e);
            return null;
        }
    }

    private String readFromInputStream(InputStream inputStream)
            throws IOException {
        StringBuilder resultStringBuilder = new StringBuilder();
        try (BufferedReader br
                     = new BufferedReader(new InputStreamReader(inputStream))) {
            String line;
            while ((line = br.readLine()) != null) {
                resultStringBuilder.append(line).append("\n");
            }
        }
        return resultStringBuilder.toString();
    }
}
