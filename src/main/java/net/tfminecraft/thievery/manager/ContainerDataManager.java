package net.tfminecraft.thievery.manager;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import net.tfminecraft.thievery.data.ContainerData;

import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Location;

import java.io.*;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class ContainerDataManager {

    private final File dataFolder;
    private final Gson gson = new Gson();

    private static class ContainerDataJson {
        UUID owner;
        Map<UUID, String> accessMap = new HashMap<>();
    }

    public ContainerDataManager() {
        this.dataFolder = new File("plugins/Thievery/data");
        if (!dataFolder.exists()) {
            dataFolder.mkdirs();
        }
    }

    public boolean deleteContainerData(Location location) {
        File file = getFileForLocation(location);
        return file.exists() && file.delete();
    }

    public ContainerData loadContainerData(Location location) {
        File file = getFileForLocation(location);
        if (!file.exists()) {
            return new ContainerData(location); // fresh
        }

        try (Reader reader = new FileReader(file)) {
            ContainerDataJson json = gson.fromJson(reader, ContainerDataJson.class);

            ContainerData data = new ContainerData(location);
            data.setOwner(json.owner);
            data.setAccessMap(json.accessMap);
            return data;
        } catch (IOException e) {
            Bukkit.getLogger().warning("Failed to load container data for " + location + ": " + e.getMessage());
            return new ContainerData(location);
        }
    }

    public void saveContainerData(ContainerData data) {
        File file = getFileForLocation(data.getLocation());
        File parent = file.getParentFile();
        if (!parent.exists()) parent.mkdirs();

        try (Writer writer = new FileWriter(file)) {
            ContainerDataJson json = new ContainerDataJson();
            json.owner = data.getOwner();
            json.accessMap = data.getAccessMap();
            gson.toJson(json, writer);
        } catch (IOException e) {
            Bukkit.getLogger().warning("Failed to save container data for " + data.getLocation() + ": " + e.getMessage());
        }
    }

    private File getFileForLocation(Location loc) {
        Chunk chunk = loc.getChunk();
        String chunkFolder = chunk.getX() + "_" + chunk.getZ();
        String fileName = loc.getBlockX() + "_" + loc.getBlockY() + "_" + loc.getBlockZ() + ".json";
        return new File(new File(dataFolder, chunkFolder), fileName);
    }
}
