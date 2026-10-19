package immersive_aircraft.client;

import com.mojang.blaze3d.platform.InputConstants;
import immersive_aircraft.Main;
import immersive_aircraft.config.Config;
import immersive_aircraft.mixin.client.KeyMappingAccessorMixin;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.function.Supplier;

public class KeyBindings {
    public static final List<KeyMapping> list = new LinkedList<>();
    private static final KeyMapping.Category CATEGORY = KeyMapping.Category.register(Identifier.fromNamespaceAndPath(Main.MOD_ID, "immersive_aircraft_tab"));
    private static final Set<InputConstants.Key> physicalKeys = new HashSet<>();
    private static final boolean useMultiKeys = Config.getInstance().useCustomKeybindSystem && Main.MOD_LOADER.equals("fabric");

    public static final KeyMapping left, right, forward, backward, up, down, throttleUp, throttleDown, pull, push;
    public static final KeyMapping dismount, boost, use, mouseControl, freeLook;

    static {
        left = newControlKey("control_left", InputConstants.KEY_A, () -> Minecraft.getInstance().options.keyLeft);
        right = newControlKey("control_right", InputConstants.KEY_D, () -> Minecraft.getInstance().options.keyRight);
        forward = newControlKey("control_forward", InputConstants.KEY_W, () -> Minecraft.getInstance().options.keyUp);
        backward = newControlKey("control_backward", InputConstants.KEY_S, () -> Minecraft.getInstance().options.keyDown);
        up = newControlKey("control_up", InputConstants.KEY_SPACE, () -> Minecraft.getInstance().options.keyJump);
        down = newControlKey("control_down", InputConstants.KEY_LSHIFT, () -> Minecraft.getInstance().options.keyShift);
        throttleUp = newControlKey("control_throttle_up", InputConstants.KEY_SPACE, () -> Minecraft.getInstance().options.keyJump);
        throttleDown = newControlKey("control_throttle_down", InputConstants.KEY_LSHIFT, () -> Minecraft.getInstance().options.keyShift);
        pull = newControlKey("control_pull", InputConstants.KEY_S, () -> Minecraft.getInstance().options.keyDown);
        push = newControlKey("control_push", InputConstants.KEY_W, () -> Minecraft.getInstance().options.keyUp);
        use = newControlKey("use", InputConstants.MOUSE_BUTTON_RIGHT, InputConstants.Type.MOUSE, () -> Minecraft.getInstance().options.keyUse);

        dismount = newKey("dismount", InputConstants.KEY_R);
        boost = newKey("boost", InputConstants.KEY_B);
        mouseControl = newKey("mouse_control", InputConstants.KEY_GRAVE);
        freeLook = newKey("free_look", InputConstants.KEY_LALT);
    }

    private static KeyMapping newControlKey(String name, int defaultKey, Supplier<KeyMapping> fallback) {
        return newControlKey(name, defaultKey, InputConstants.Type.KEYBOARD, fallback);
    }

    private static KeyMapping newControlKey(String name, int defaultKey, InputConstants.Type type, Supplier<KeyMapping> fallback) {
        String translationKey = "key.immersive_aircraft." + (useMultiKeys ? "multi_" : "fallback_") + name;
        KeyMapping key = useMultiKeys
                ? new MultiKeyMapping(translationKey, type, defaultKey, fallback, CATEGORY)
                : new FallbackKeyMapping(translationKey, InputConstants.Type.KEYBOARD, fallback, CATEGORY);
        list.add(key);
        return key;
    }

    private static KeyMapping newKey(String name, int code) {
        KeyMapping key = new KeyMapping(
                "key.immersive_aircraft." + name,
                InputConstants.Type.KEYBOARD,
                code,
                CATEGORY
        );
        list.add(key);
        return key;
    }

    public static InputConstants.Key getBoundKey(KeyMapping key) {
        if (key instanceof MultiKeyMapping multi) {
            return multi.getBoundKey();
        }
        if (key instanceof FallbackKeyMapping fallback) {
            return fallback.getBoundKey();
        }
        return ((KeyMappingAccessorMixin) key).getKey();
    }

    public static KeyMapping getFallbackKey(KeyMapping key) {
        if (key instanceof MultiKeyMapping multi) {
            return multi.getFallbackKey();
        }
        if (key instanceof FallbackKeyMapping fallback) {
            return fallback.getFallbackKey();
        }
        return null;
    }

    public static boolean isPhysicalDown(KeyMapping key) {
        return physicalKeys.contains(getBoundKey(key));
    }

    public static void setPhysicalKey(InputConstants.Key key, boolean pressed) {
        if (pressed) {
            physicalKeys.add(key);
        } else {
            physicalKeys.remove(key);
        }
    }

    public static void clearPhysicalKeys() {
        physicalKeys.clear();
    }

    public static void refreshPhysicalKeys(Iterable<KeyMapping> bindings) {
        for (KeyMapping binding : bindings) {
            InputConstants.Key key = getBoundKey(binding);
            if (key.getType() == InputConstants.Type.KEYBOARD && !key.equals(InputConstants.UNKNOWN)) {
                boolean pressed = InputConstants.isKeyDown(key.getValue());
                setPhysicalKey(key, pressed);
                if (binding instanceof MultiKeyMapping) {
                    binding.setDown(pressed);
                }
            }
        }
    }
}
