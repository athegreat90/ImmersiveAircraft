package immersive_aircraft.network.s2c;

import immersive_aircraft.cobalt.network.Message;
import immersive_aircraft.cobalt.registration.CobaltFuelRegistry;
import net.minecraft.core.registries.BuiltInRegistries;
import immersive_aircraft.data.VehicleDataLoader;
import immersive_aircraft.entity.misc.VehicleData;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Player;

import java.util.HashMap;
import java.util.Map;

public class AircraftDataMessage extends Message {
    private final Map<Identifier, VehicleData> data;
    private final Map<Identifier, Integer> fuelValues;

    public AircraftDataMessage() {
        this.data = VehicleDataLoader.REGISTRY;
        this.fuelValues = CobaltFuelRegistry.getSyncedFuelValues();
    }

    public AircraftDataMessage(FriendlyByteBuf buffer) {
        data = new HashMap<>();

        int dataCount = buffer.readInt();
        for (int i = 0; i < dataCount; i++) {
            Identifier identifier = buffer.readIdentifier();
            data.put(identifier, new VehicleData(buffer));
        }
        fuelValues = new HashMap<>();
        int fuelCount = buffer.readVarInt();
        if (fuelCount < 0 || fuelCount > BuiltInRegistries.ITEM.size()) {
            throw new IllegalArgumentException("Invalid synchronized fuel count: " + fuelCount);
        }
        for (int i = 0; i < fuelCount; i++) {
            Identifier item = buffer.readIdentifier();
            int duration = buffer.readVarInt();
            if (duration <= 0 || !BuiltInRegistries.ITEM.containsKey(item)
                    || fuelValues.putIfAbsent(item, duration) != null) {
                throw new IllegalArgumentException("Invalid synchronized fuel entry: " + item);
            }
        }
    }

    @Override
    public void encode(FriendlyByteBuf buffer) {
        buffer.writeInt(data.size());

        for (Identifier identifier : data.keySet()) {
            buffer.writeIdentifier(identifier);
            data.get(identifier).encode(buffer);
        }
        buffer.writeVarInt(fuelValues.size());
        fuelValues.forEach((item, duration) -> {
            buffer.writeIdentifier(item);
            buffer.writeVarInt(duration);
        });
    }

    @Override
    public void receive(Player player) {
        VehicleDataLoader.CLIENT_REGISTRY.clear();
        VehicleDataLoader.CLIENT_REGISTRY.putAll(data);
        CobaltFuelRegistry.setSyncedFuelValues(fuelValues);
    }
}
