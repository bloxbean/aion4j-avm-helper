package org.aion4j.avm.helper.faucet.model;

public class Network {
    private String id;
    private String genesisHash;
    private String network;
    private String faucetContract;

    public Network(String id, String genesisHash, String network, String faucetContract) {
        this.id = id;
        this.genesisHash = genesisHash;
        this.network = network;
        this.faucetContract = faucetContract;
    }

    public String getId() {
        return id;
    }

    public String getGenesisHash() {
        return genesisHash;
    }

    public String getNetwork() {
        return network;
    }

    public String getFaucetContract() {
        return faucetContract;
    }
}
