package net.tfminecraft.thievery.data;



import java.util.HashMap;

import java.util.Map;

import java.util.UUID;



public class PlayerTargetData {



    private UUID victimId;

    private Map<UUID, String> robberyAccessMap = new HashMap<>();

    private Map<UUID, String> pickpocketAccessMap = new HashMap<>();



    public PlayerTargetData(UUID victimId) {

        this.victimId = victimId;

    }



    public UUID getVictimId() {

        return victimId;

    }



    public void setVictimId(UUID victimId) {

        this.victimId = victimId;

    }



    public Map<UUID, String> getRobberyAccessMap() {

        return robberyAccessMap;

    }



    public void setRobberyAccessMap(Map<UUID, String> robberyAccessMap) {

        this.robberyAccessMap = robberyAccessMap != null ? robberyAccessMap : new HashMap<>();

    }



    public Map<UUID, String> getPickpocketAccessMap() {

        return pickpocketAccessMap;

    }



    public void setPickpocketAccessMap(Map<UUID, String> pickpocketAccessMap) {

        this.pickpocketAccessMap = pickpocketAccessMap != null ? pickpocketAccessMap : new HashMap<>();

    }



    public void updateRobberyAccess(UUID attackerUuid, String date) {

        robberyAccessMap.put(attackerUuid, date);

    }



    public void updatePickpocketAccess(UUID attackerUuid, long epochMs) {

        pickpocketAccessMap.put(attackerUuid, Long.toString(epochMs));

    }



    /** @deprecated use getRobberyAccessMap */

    @Deprecated

    public Map<UUID, String> getAccessMap() {

        return robberyAccessMap;

    }



    /** @deprecated use setRobberyAccessMap */

    @Deprecated

    public void setAccessMap(Map<UUID, String> accessMap) {

        setRobberyAccessMap(accessMap);

    }



    /** @deprecated use updateRobberyAccess */

    @Deprecated

    public void updateAccess(UUID attackerUuid, String date) {

        updateRobberyAccess(attackerUuid, date);

    }

}

