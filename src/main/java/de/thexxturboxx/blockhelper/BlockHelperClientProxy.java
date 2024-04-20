package de.thexxturboxx.blockhelper;

import cpw.mods.fml.common.registry.TickRegistry;
import cpw.mods.fml.relauncher.Side;
import de.thexxturboxx.blockhelper.client.BlockHelperOverlay;
import de.thexxturboxx.blockhelper.integration.nei.ModIdentifier;
import net.minecraft.client.Minecraft;
import net.minecraft.client.settings.KeyBinding;
import org.lwjgl.input.Keyboard;

import java.lang.reflect.Array;

public class BlockHelperClientProxy extends BlockHelperCommonProxy {

    public static double size;
    public static double sizeInv;
    public static int background;
    public static int gradient1;
    public static int gradient2;
    public static boolean fixerNotify;
    public static boolean showItemId;
    public static boolean showHarvest;
    public static boolean showBreakProg;
    public static boolean showMod;
    public static boolean showBlock;
    public static boolean shouldHideFromDebug;
    public static KeyBinding showHide;

    @Override
    public void load(BlockHelper instance) {
        super.load(instance);
        BlockHelper.isClient = true;
        ModIdentifier.load();
        size = cfg.get("General", "Size", 1D, "Size factor for the tooltip").getDouble(1);
        background = parseUnsignedInt(cfg.get("General", "BackgroundColor",
                "cc100010", "Background Color Hex Code").getString(), 16);
        gradient1 = parseUnsignedInt(cfg.get("General", "BorderColor1",
                "cc5000ff", "Border Color Hex Code 1").getString(), 16);
        gradient2 = parseUnsignedInt(cfg.get("General", "BorderColor2",
                "cc28007f", "Border Color Hex Code 2").getString(), 16);
        fixerNotify = cfg.get("General", "NotifyAboutFixers", true,
                "Notifies about the nice Fixer mods :)").getBoolean(true);
        showItemId = cfg.get("General", "ShowItemID", true,
                "Shows the Item ID in the HUD").getBoolean(true);
        showHarvest = cfg.get("General", "ShowHarvestability", true,
                "Shows the current block harvestability in the HUD").getBoolean(true);
        showBreakProg = cfg.get("General", "ShowBreakProgression", true,
                "Shows the current block break progression in the HUD").getBoolean(true);
        showMod = cfg.get("General", "ShowMod", true,
                "Shows the mod of the current block in the HUD").getBoolean(true);
        showBlock = cfg.get("General", "ShowBlockInHud", true,
                "Renders the current block in the HUD").getBoolean(true);
        shouldHideFromDebug = cfg.get("General", "ShouldHideFromDebug", true,
                "Hides the HUD if the debug screen (F3) is shown").getBoolean(true);
        cfg.save();
        sizeInv = 1 / size;
        showHide = new KeyBinding("blockhelper.key_show_hide", Keyboard.KEY_NUMPAD0);
        // Register Keybinds using 1.7.10 method
        Minecraft.getMinecraft().gameSettings.keyBindings = add(Minecraft.getMinecraft().gameSettings.keyBindings, showHide);
        TickRegistry.registerTickHandler(new BlockHelperOverlay(), Side.CLIENT);

    }

    public static <T> T[] add(final T[] array, final T element) {
        final Class<?> type;
        if (array != null) {
            type = array.getClass().getComponentType();
        } else if (element != null) {
            type = element.getClass();
        } else {
            throw new IllegalArgumentException("Arguments cannot both be null");
        }
        @SuppressWarnings("unchecked") // type must be T
        final T[] newArray = (T[]) copyArrayGrow(array, type);
        newArray[newArray.length - 1] = element;
        return newArray;
    }

    private static Object copyArrayGrow(final Object array, final Class<?> newArrayComponentType) {
        if (array != null) {
            final int arrayLength = Array.getLength(array);
            final Object newArray = Array.newInstance(array.getClass().getComponentType(), arrayLength + 1);
            System.arraycopy(array, 0, newArray, 0, arrayLength);
            return newArray;
        }
        return Array.newInstance(newArrayComponentType, 1);
    }

    /**
     * This method is copied from JDK 8, because it isn't available in JDK 7 or less.
     *
     * @param s     The string to parse.
     * @param radix The radix to parse with.
     * @return The parsed unsigned integer.
     * @throws NumberFormatException Some parsing error occurred.
     */
    public static int parseUnsignedInt(String s, int radix) throws NumberFormatException {
        if (s == null) {
            throw new NumberFormatException("null");
        }

        int len = s.length();
        if (len > 0) {
            char firstChar = s.charAt(0);
            if (firstChar == '-') {
                throw new NumberFormatException(String.format("Illegal leading minus sign "
                        + "on unsigned string %s.", s));
            } else {
                if (len <= 5 || (radix == 10 && len <= 9)) {
                    return Integer.parseInt(s, radix);
                } else {
                    long ell = Long.parseLong(s, radix);
                    if ((ell & 0xffffffff00000000L) == 0) {
                        return (int) ell;
                    } else {
                        throw new NumberFormatException(String.format("String value %s exceeds "
                                + "range of unsigned int.", s));
                    }
                }
            }
        } else {
            throw new NumberFormatException("For input string: \"" + s + "\"");
        }
    }
}
