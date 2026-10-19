package immersive_aircraft.neoforge.cobalt.registration;

import immersive_aircraft.cobalt.registration.CobaltFuelRegistry;
import immersive_aircraft.util.Utils;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.component.CookingFuel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.FurnaceBlockEntity;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.level.storage.loot.providers.number.ints.ResolvableInt;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.loot.NeoForgeLootContextParams;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.FluidType;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.access.ItemAccess;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.item.VanillaContainerWrapper;
import net.neoforged.neoforge.transfer.transaction.Transaction;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class CobaltFuelRegistryImpl extends CobaltFuelRegistry {
    public CobaltFuelRegistryImpl() {
        INSTANCE = this;
    }

    public static void setFuelValues(ServerLevel level) {
        Map<Identifier, Integer> values = new HashMap<>();
        if (level != null) {
            // Preserve the old item-based, normal-furnace fuel registry. The
            // 26.3 providers are server data, so resolve a snapshot on reload
            // and synchronize it rather than evaluating server loot on a client.
            // This furnace is never placed in or ticked by the world.
            var furnace = new FurnaceBlockEntity(BlockPos.ZERO, Blocks.FURNACE.defaultBlockState());
            furnace.setLevel(level);
            for (Item item : BuiltInRegistries.ITEM) {
                ItemStack stack = item.getDefaultInstance();
                if (stack.isEmpty() || !stack.has(DataComponents.COOKING_FUEL)) continue;
                var params = new LootParams.Builder(level)
                        .withParameter(LootContextParams.BLOCK_STATE, furnace.getBlockState())
                        .withParameter(LootContextParams.BLOCK_ENTITY, furnace)
                        .withParameter(LootContextParams.CONTAINER, furnace)
                        .withParameter(LootContextParams.ORIGIN, Vec3.atCenterOf(BlockPos.ZERO))
                        .withParameter(NeoForgeLootContextParams.QUERIED_STACK, stack)
                        .create(LootContextParamSets.CONTAINER_PROCESS);
                int duration = ResolvableInt.getFromItem(stack, DataComponents.COOKING_FUEL,
                        CookingFuel::burnTime, new LootContext.Builder(params).create(Optional.empty()), 0);
                if (duration > 0) values.put(BuiltInRegistries.ITEM.getKey(item), duration);
            }
        }
        setSyncedFuelValues(values);
    }

    @Override
    public int get(ItemStack stack) {
        return getSyncedFuelValues().getOrDefault(BuiltInRegistries.ITEM.getKey(stack.getItem()), 0);
    }

    @Override
    public int getFluidFuelTime(ItemStack stack) {
        return refuelFluid(new SimpleContainer(stack.copy()), 0, true);
    }

    @Override
    public int refuelFluid(Container inventory, int slot) {
        return refuelFluid(inventory, slot, false);
    }

    private int refuelFluid(Container inventory, int slot, boolean simulate) {
        ItemAccess access = ItemAccess.forHandlerIndex(VanillaContainerWrapper.of(inventory), slot);
        ResourceHandler<FluidResource> handler = inventory.getItem(slot).getCapability(Capabilities.Fluid.ITEM, access);
        if (handler == null) {
            return 0;
        }
        for (int tank = 0; tank < handler.size(); tank++) {
            FluidResource fluid = handler.getResource(tank);
            int time = fluid.isEmpty() ? 0 : Utils.getFluidFuelTime(fluid.getFluid());
            if (time > 0) {
                try (Transaction transaction = Transaction.open(Transaction.getCurrentOpenedTransaction())) {
                    if (handler.extract(fluid, FluidType.BUCKET_VOLUME, transaction) == FluidType.BUCKET_VOLUME) {
                        if (!simulate) {
                            transaction.commit();
                        }
                        return time;
                    }
                }
            }
        }
        return 0;
    }
}
