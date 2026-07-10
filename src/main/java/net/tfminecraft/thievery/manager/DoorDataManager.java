package net.tfminecraft.thievery.manager;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Location;

import com.google.gson.Gson;

import net.tfminecraft.thievery.data.DoorData;

public class DoorDataManager {

    private final File dataFolder;
    private final Gson gson = new Gson();

    private static class DoorDataJson {
        String key;
        double strength;
        String ownerUUID;
        Long unlockExpiryMs;
    }

    public DoorDataManager() {
        this.dataFolder = new File("plugins/Thievery/door-data");
        if (!dataFolder.exists()) {
            dataFolder.mkdirs();
        }
    }

    public DoorData loadDoorData(Location location) {
        File file = getFileForLocation(location);
        if (!file.exists()) return null;

        try (Reader reader = new FileReader(file)) {
            DoorDataJson json = gson.fromJson(reader, DoorDataJson.class);
            UUID ownerUUID = json.ownerUUID != null ? UUID.fromString(json.ownerUUID) : null;
            DoorData data = new DoorData(location, json.key, json.strength, ownerUUID);
            data.setUnlockExpiryMs(json.unlockExpiryMs);
            return data;
        } catch (IOException e) {
            Bukkit.getLogger().warning("Failed to load door data for " + location + ": " + e.getMessage());
            return null;
        }
    }

    public void saveDoorData(DoorData data) {
        File file = getFileForLocation(data.getLocation());
        File parent = file.getParentFile();
        if (!parent.exists()) parent.mkdirs();

        try (Writer writer = new FileWriter(file)) {
            DoorDataJson json = new DoorDataJson();
            json.key = data.getKey();
            json.strength = data.getStrength();
            json.ownerUUID = data.getOwnerUUID() != null ? data.getOwnerUUID().toString() : null;
            json.unlockExpiryMs = data.getUnlockExpiryMs();
            gson.toJson(json, writer);
        } catch (IOException e) {
            Bukkit.getLogger().warning("Failed to save door data for " + data.getLocation() + ": " + e.getMessage());
        }
    }

    public boolean deleteDoorData(Location location) {
        File file = getFileForLocation(location);
        return file.exists() && file.delete();
    }

    private File getFileForLocation(Location loc) {
        Chunk chunk = loc.getChunk();
        String chunkFolder = chunk.getX() + "_" + chunk.getZ();
        String fileName = loc.getBlockX() + "_" + loc.getBlockY() + "_" + loc.getBlockZ() + ".json";
        return new File(new File(dataFolder, chunkFolder), fileName);
    }
}
