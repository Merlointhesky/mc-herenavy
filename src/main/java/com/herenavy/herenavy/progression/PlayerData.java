package com.herenavy.herenavy.progression;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public final class PlayerData {

    private final UUID uuid;
    private int level;
    private int exp;
    private final Set<String> discoveredBiomes;

    // Personal navigation settings preferences
    private boolean showArrow;
    private boolean showTrail;
    private String compassStyle;

    public PlayerData(UUID uuid) {
        this.uuid = uuid;
        this.level = 1;
        this.exp = 0;
        this.discoveredBiomes = new HashSet<>();
        // Set standard defaults
        this.showArrow = true;
        this.showTrail = true;
        this.compassStyle = "ACTIONBAR";
    }

    public boolean isShowArrow() {
        return showArrow;
    }

    public void setShowArrow(boolean showArrow) {
        this.showArrow = showArrow;
    }

    public boolean isShowTrail() {
        return showTrail;
    }

    public void setShowTrail(boolean showTrail) {
        this.showTrail = showTrail;
    }

    public String getCompassStyle() {
        return compassStyle;
    }

    public void setCompassStyle(String compassStyle) {
        this.compassStyle = compassStyle.toUpperCase();
    }

    public UUID getUuid() {
        return uuid;
    }

    public int getLevel() {
        return level;
    }

    public void setLevel(int level) {
        this.level = level;
    }

    public int getExp() {
        return exp;
    }

    public void setExp(int exp) {
        this.exp = exp;
    }

    public void addExp(int amount) {
        this.exp += amount;
    }

    public Set<String> getDiscoveredBiomes() {
        return discoveredBiomes;
    }

    public boolean hasDiscoveredBiome(String biomeKey) {
        return discoveredBiomes.contains(biomeKey.toLowerCase());
    }

    public boolean discoverBiome(String biomeKey) {
        return discoveredBiomes.add(biomeKey.toLowerCase());
    }
}
