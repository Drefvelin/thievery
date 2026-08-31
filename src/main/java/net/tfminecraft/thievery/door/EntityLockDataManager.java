package net.tfminecraft.thievery.door;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.util.UUID;

import org.bukkit.Bukkit;

import com.google.gson.Gson;

import net.tfminecraft.thievery.Thievery;

public class EntityLockDataManager {

    private final File dataFolder;
    private final Gson gson = new Gson();

    private static class EntityLockDataJson {
        UUID owner;
        LockState lockState = LockState.PRIVATE;
    }

    public EntityLockDataManager() {
        this.dataFolder = new File(Thievery.getInstance().getDataFolder(), "entities");
        if (!dataFolder.exists()) {
            dataFolder.mkdirs();
        }
    }

    public EntityLockData load(UUID entityId) {
        if (entityId == null) {
            return new EntityLockData(null);
        }
        File file = getFile(entityId);
        if (!file.exists()) {
            return new EntityLockData(entityId);
        }
        try (Reader reader = new FileReader(file)) {
            EntityLockDataJson json = gson.fromJson(reader, EntityLockDataJson.class);
            EntityLockData data = new EntityLockData(entityId);
            if (json != null) {
                data.setOwner(json.owner);
                data.setLockState(json.lockState);
            }
            return data;
        } catch (IOException e) {
            Bukkit.getLogger().warning("Failed to load entity lock data for " + entityId + ": " + e.getMessage());
            return new EntityLockData(entityId);
        }
    }

    public void save(EntityLockData data) {
        if (data == null || data.getEntityId() == null) {
            return;
        }
        File file = getFile(data.getEntityId());
        File parent = file.getParentFile();
        if (!parent.exists()) {
            parent.mkdirs();
        }
        try (Writer writer = new FileWriter(file)) {
            EntityLockDataJson json = new EntityLockDataJson();
            json.owner = data.getOwner();
            json.lockState = data.getLockState();
            gson.toJson(json, writer);
        } catch (IOException e) {
            Bukkit.getLogger().warning("Failed to save entity lock data for " + data.getEntityId() + ": " + e.getMessage());
        }
    }

    public boolean delete(UUID entityId) {
        if (entityId == null) {
            return false;
        }
        File file = getFile(entityId);
        return file.exists() && file.delete();
    }

    private File getFile(UUID entityId) {
        return new File(dataFolder, entityId.toString() + ".json");
    }
}
