package de.thexxturboxx.blockhelper;

import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.FMLLog;
import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.SidedProxy;
import cpw.mods.fml.common.event.FMLPostInitializationEvent;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import cpw.mods.fml.common.network.IPacketHandler;
import cpw.mods.fml.common.network.NetworkMod;
import cpw.mods.fml.common.network.PacketDispatcher;
import cpw.mods.fml.common.network.Player;
import de.thexxturboxx.blockhelper.api.BlockHelperBlockState;
import de.thexxturboxx.blockhelper.api.BlockHelperEntityState;
import de.thexxturboxx.blockhelper.api.BlockHelperModSupport;
import de.thexxturboxx.blockhelper.client.BlockHelperOverlay;
import de.thexxturboxx.blockhelper.i18n.I18n;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.logging.Logger;
import net.minecraft.block.Block;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.network.INetworkManager;
import net.minecraft.network.packet.Packet250CustomPayload;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.StringTranslate;
import net.minecraft.world.World;
import net.minecraftforge.common.DimensionManager;

import static de.thexxturboxx.blockhelper.BlockHelper.CHANNEL;

@Mod(modid = BlockHelper.MOD_ID, name = BlockHelper.NAME, version = BlockHelper.VERSION, acceptedMinecraftVersions = BlockHelper.MC_VERSION)
@NetworkMod(channels = {CHANNEL}, packetHandler = BlockHelper.class)
public class BlockHelper implements IPacketHandler {

    public static final String PACKAGE = "de.thexxturboxx.blockhelper.";
    public static final String MOD_ID = "mod_BlockHelper";
    public static final String NAME = "Block Helper";
    public static final String VERSION = "1.2.0-pre3";
    public static final String MC_VERSION = "1.5.2";
    public static final String CHANNEL = "BlockHelperInfo";
    public static BlockHelper INSTANCE;
    public static final Logger LOGGER = Logger.getLogger(NAME);
    public static final MopType[] MOP_TYPES = MopType.values();
    public static boolean isClient;

    @SidedProxy(clientSide = PACKAGE + "BlockHelperClientProxy", serverSide = PACKAGE + "BlockHelperCommonProxy")
    public static BlockHelperCommonProxy proxy;

    public BlockHelper() {
        LOGGER.setParent(FMLLog.getLogger());
    }

    @Mod.PostInit
    public void postInit(FMLPostInitializationEvent e) {
        INSTANCE = this;
        proxy.load(this);
    }

    @Override
    public void onPacketData(INetworkManager manager, Packet250CustomPayload packetGot, Player player) {
        try {
            if (packetGot.channel.equals(CHANNEL)) {
                ByteArrayInputStream isRaw = new ByteArrayInputStream(packetGot.data);
                DataInputStream is = new DataInputStream(isRaw);
                ByteArrayOutputStream buffer = new ByteArrayOutputStream();
                DataOutputStream os = new DataOutputStream(buffer);
                try {
                    if (isClient && FMLCommonHandler.instance().getEffectiveSide().isClient()) {
                        try {
                            BlockHelperOverlay.getInstance().setData(((PacketClient) PacketCoder.decode(is)).data);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    } else if (FMLCommonHandler.instance().getEffectiveSide().isServer()) {
                        PacketInfo pi = null;
                        try {
                            pi = (PacketInfo) PacketCoder.decode(is);
                        } catch (IOException ignored) {
                        }

                        PacketClient info = new PacketClient();

                        StringTranslate translator = player instanceof EntityPlayer
                                ? ((EntityPlayer) player).getTranslator()
                                : StringTranslate.getInstance();

                        if (pi != null && pi.mop != null) {
                            World w = DimensionManager.getProvider(pi.dimId).worldObj;
                            if (pi.mt == MopType.ENTITY) {
                                Entity en = w.getEntityByID(pi.entityId);
                                if (en != null) {
                                    if (BlockHelperCommonProxy.showHealth) {
                                        try {
                                            info.add(((EntityLiving) en).getHealth() + " \\u2665 / "
                                                     + ((EntityLiving) en).getMaxHealth() + " \\u2665");
                                        } catch (Throwable ignored) {
                                        }
                                    }

                                    BlockHelperModSupport.addInfo(new BlockHelperEntityState(translator, w, en), info);
                                }
                            } else if (pi.mt == MopType.BLOCK) {
                                int x = pi.mop.blockX;
                                int y = pi.mop.blockY;
                                int z = pi.mop.blockZ;
                                TileEntity te = w.getBlockTileEntity(x, y, z);
                                int id = w.getBlockId(x, y, z);
                                if (id > 0) {
                                    int meta = w.getBlockMetadata(x, y, z);
                                    Block b = Block.blocksList[id];
                                    BlockHelperModSupport.addInfo(
                                            new BlockHelperBlockState(translator, w, pi.mop, b, te, id, meta), info);
                                }
                            } else {
                                return;
                            }
                        } else {
                            info.add(I18n.format(translator, "server_side_error"));
                            info.add(I18n.format(translator, "version_mismatch"));
                        }

                        try {
                            PacketCoder.encode(os, info);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }

                        byte[] fieldData = buffer.toByteArray();
                        Packet250CustomPayload packet = new Packet250CustomPayload();
                        packet.channel = CHANNEL;
                        packet.data = fieldData;
                        packet.length = fieldData.length;
                        PacketDispatcher.sendPacketToPlayer(packet, player);
                    }
                } finally {
                    os.close();
                    buffer.close();
                    is.close();
                    isRaw.close();
                }
            }
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }

}
