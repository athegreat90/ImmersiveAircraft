package immersive_aircraft.neoforge;

import immersive_aircraft.cobalt.registration.CobaltFuelRegistry;
import immersive_aircraft.config.Config;
import immersive_aircraft.data.VehicleDataLoader;
import immersive_aircraft.neoforge.cobalt.registration.CobaltFuelRegistryImpl;
import immersive_aircraft.network.s2c.AircraftDataMessage;
import io.netty.buffer.Unpooled;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.testframework.junit.EphemeralTestServerProvider;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(EphemeralTestServerProvider.class)
class Local263FuelTest {
    @Test
    void simulatedFluidDrainPreservesTheBucketAndRealDrainCommitsOnce(MinecraftServer server) {
        server.executeBlocking(() -> {
            Config config = Config.getInstance();
            Map<String, Integer> original = config.fluidFuelList;
            try {
                config.fluidFuelList = Map.of("minecraft:lava", 20000);
                var inventory = new SimpleContainer(new ItemStack(Items.LAVA_BUCKET));
                assertEquals(20000, CobaltFuelRegistry.INSTANCE.getFluidFuelTime(inventory.getItem(0)));
                assertTrue(inventory.getItem(0).is(Items.LAVA_BUCKET));
                assertEquals(1, inventory.getItem(0).getCount());
                assertEquals(20000, CobaltFuelRegistry.INSTANCE.refuelFluid(inventory, 0));
                assertTrue(inventory.getItem(0).is(Items.BUCKET));
                assertEquals(1, inventory.getItem(0).getCount());
                assertEquals(0, CobaltFuelRegistry.INSTANCE.refuelFluid(inventory, 0));
                assertTrue(inventory.getItem(0).is(Items.BUCKET));
            } finally {
                config.fluidFuelList = original;
            }
        });
    }

    @Test
    void fuelSnapshotSurvivesItsActualPacketCodec(MinecraftServer server) {
        server.executeBlocking(() -> {
            var previousFuels = CobaltFuelRegistry.getSyncedFuelValues();
            var previousClientData = new HashMap<>(VehicleDataLoader.CLIENT_REGISTRY);
            var expected = Map.of(Identifier.withDefaultNamespace("coal"), 1600);
            var buffer = new FriendlyByteBuf(Unpooled.buffer());
            try {
                CobaltFuelRegistry.setSyncedFuelValues(expected);
                new AircraftDataMessage().encode(buffer);
                CobaltFuelRegistry.setSyncedFuelValues(Map.of());
                // Receive has no player-dependent logic; this checks the codec,
                // not a real client connection or its handshake.
                new AircraftDataMessage(buffer).receive(null);
                assertEquals(expected, CobaltFuelRegistry.getSyncedFuelValues());
                assertEquals(0, buffer.readableBytes());
            } finally {
                buffer.release();
                CobaltFuelRegistry.setSyncedFuelValues(previousFuels);
                VehicleDataLoader.CLIENT_REGISTRY.clear();
                VehicleDataLoader.CLIENT_REGISTRY.putAll(previousClientData);
            }
        });
    }

    @Test
    void fuelCodecRejectsOversizedUnknownDuplicateAndNonpositiveEntries(MinecraftServer server) {
        server.executeBlocking(() -> {
            malformed(buffer -> buffer.writeVarInt(BuiltInRegistries.ITEM.size() + 1));
            malformed(buffer -> {
                buffer.writeVarInt(1);
                buffer.writeIdentifier(Identifier.fromNamespaceAndPath("local_port_test", "not_registered"));
                buffer.writeVarInt(1600);
            });
            malformed(buffer -> {
                buffer.writeVarInt(1);
                buffer.writeIdentifier(Identifier.withDefaultNamespace("coal"));
                buffer.writeVarInt(0);
            });
            malformed(buffer -> {
                buffer.writeVarInt(2);
                for (int i = 0; i < 2; i++) {
                    buffer.writeIdentifier(Identifier.withDefaultNamespace("coal"));
                    buffer.writeVarInt(1600);
                }
            });
        });
    }

    private static void malformed(Consumer<FriendlyByteBuf> writeFuelEntries) {
        var buffer = new FriendlyByteBuf(Unpooled.buffer());
        try {
            buffer.writeInt(0); // No vehicle entries; exercise just the fuel section.
            writeFuelEntries.accept(buffer);
            assertThrows(IllegalArgumentException.class, () -> new AircraftDataMessage(buffer));
        } finally {
            buffer.release();
        }
    }

}
