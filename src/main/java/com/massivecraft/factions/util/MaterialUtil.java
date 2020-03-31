package com.massivecraft.factions.util;

import org.bukkit.Material;

import java.util.Arrays;

public class MaterialUtil {

    private static Material[] byId;

    static {
        byId = new Material[0];
        for (Material material : Material.values()) {
            int id = material.getId();
            if (byId.length <= id) {
                byId = Arrays.copyOfRange(byId, 0, id + 2);
            }
            byId[id] = material;
        }
    }

    public static Material getMaterialById(int id) {
        // I believe the first statement should be (id < byId.length) but this is the original logic
        return byId.length > id && id >= 0 ? byId[id] : null;
    }


}
