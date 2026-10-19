package immersive_aircraft.cobalt.registration;

import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.resources.Identifier;

import java.util.Map;

public abstract class CobaltFuelRegistry {
    public static CobaltFuelRegistry INSTANCE = null;
    private static volatile Map<Identifier, Integer> syncedFuelValues = Map.of();

    public static Map<Identifier, Integer> getSyncedFuelValues() {
        return syncedFuelValues;
    }

    public static void setSyncedFuelValues(Map<Identifier, Integer> values) {
        syncedFuelValues = Map.copyOf(values);
    }

    public abstract int get(ItemStack stack);

    public abstract int getFluidFuelTime(ItemStack stack);

    // Drain exactly one bucket, retaining the updated container in its inventory slot.
    public abstract int refuelFluid(Container inventory, int slot);
}
