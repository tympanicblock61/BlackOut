package kassuk.addon.blackout.modules;

import kassuk.addon.blackout.BlackOut;
import kassuk.addon.blackout.enums.RotationType;
import kassuk.addon.blackout.enums.SwingState;
import kassuk.addon.blackout.enums.SwingType;
import kassuk.addon.blackout.managers.Managers;
import kassuk.addon.blackout.timers.BlockTimerList;
import kassuk.addon.blackout.utils.BOInvUtils;
import kassuk.addon.blackout.utils.OLEPOSSUtils;
import kassuk.addon.blackout.utils.SettingUtils;
import meteordevelopment.meteorclient.events.render.Render3DEvent;
import meteordevelopment.meteorclient.renderer.ShapeMode;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.entity.EntityUtils;
import meteordevelopment.meteorclient.utils.player.FindItemResult;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import meteordevelopment.meteorclient.utils.player.PlayerUtils;
import meteordevelopment.meteorclient.utils.render.color.Color;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.entity.EntityType;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.PlayerInteractBlockC2SPacket;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;

import java.util.ArrayList;
import java.util.List;

/*
Made by KassuK
Updated by OLEPOSSU
*/

public class SurroundPlus extends Module {
    public SurroundPlus() {super(BlackOut.BLACKOUT, "Surround+", "KasumsSoft surround");}
    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final Setting<Boolean> pauseEat = sgGeneral.add(new BoolSetting.Builder()
        .name("Pause Eat")
        .description("Pauses when you are eating")
        .defaultValue(true)
        .build()
    );
    private final Setting<SwitchMode> switchMode = sgGeneral.add(new EnumSetting.Builder<SwitchMode>()
        .name("Switch Mode")
        .description(".")
        .defaultValue(SwitchMode.SilentBypass)
        .build()
    );
    private final Setting<List<Block>> blocks = sgGeneral.add(new BlockListSetting.Builder()
        .name("Blocks")
        .description("Blocks to use.")
        .defaultValue(Blocks.OBSIDIAN, Blocks.CRYING_OBSIDIAN, Blocks.NETHERITE_BLOCK)
        .build()
    );
    private final Setting<Boolean> floor = sgGeneral.add(new BoolSetting.Builder()
        .name("Floor")
        .description("Places blocks under your feet.")
        .defaultValue(true)
        .build()
    );
    private final Setting<Double> placeDelay = sgGeneral.add(new DoubleSetting.Builder()
        .name("Place Delay")
        .description("Delay between places.")
        .defaultValue(0)
        .range(0, 10)
        .sliderRange(0, 10)
        .build()
    );
    private final Setting<Integer> places = sgGeneral.add(new IntSetting.Builder()
        .name("Places")
        .description("Blocks placed per place")
        .defaultValue(1)
        .range(1, 10)
        .sliderRange(1, 10)
        .build()
    );
    private final Setting<Double> delay = sgGeneral.add(new DoubleSetting.Builder()
        .name("Delay")
        .description("Delay between placing at each spot.")
        .defaultValue(0.3)
        .range(0, 10)
        .sliderRange(0, 10)
        .build()
    );
    private final Setting<ShapeMode> shapeMode = sgGeneral.add(new EnumSetting.Builder<ShapeMode>()
        .name("Shape Mode")
        .description(".")
        .defaultValue(ShapeMode.Both)
        .build()
    );
    private final Setting<SettingColor> lineColor = sgGeneral.add(new ColorSetting.Builder()
        .name("Line Color")
        .description("Color of the outlines")
        .defaultValue(new SettingColor(255, 0, 0, 150))
        .build()
    );
    private final Setting<SettingColor> sideColor = sgGeneral.add(new ColorSetting.Builder()
        .name("Side Color")
        .description(".")
        .defaultValue(new SettingColor(255, 0, 0, 50))
        .build()
    );
    public enum SwitchMode {
        Disabled,
        Normal,
        Silent,
        SilentBypass
    }
    BlockTimerList timers = new BlockTimerList();
    BlockPos startPos = null;
    double placeTimer = 0;
    int placesLeft = 0;
    List<BlockPos> render = new ArrayList<>();

    @Override
    public void onActivate() {
        super.onActivate();
        if (mc.player == null || mc.world == null) {toggle();}
        startPos = mc.player.getBlockPos();
        placesLeft = places.get();
        placeTimer = 0;
        render = new ArrayList<>();
    }

    @EventHandler
    private void onRender(Render3DEvent event){
        placeTimer = Math.min(placeDelay.get(), placeTimer + event.frameTime);
        if (placeTimer >= placeDelay.get()) {
            placesLeft = places.get();
            placeTimer = 0;
        }
        render.forEach(item -> event.renderer.box(OLEPOSSUtils.getBox(item), sideColor.get(), lineColor.get(), shapeMode.get(), 0));
        update();
    }

    void update() {
        if (mc.player != null && mc.world != null) {

            //Check if player has moved
            if (!mc.player.getBlockPos().equals(startPos)) {
                toggle();
                return;
            }

            List<BlockPos> placements = check();

            FindItemResult hotbar = InvUtils.findInHotbar(item -> item.getItem() instanceof BlockItem && blocks.get().contains(((BlockItem) item.getItem()).getBlock()));
            FindItemResult inventory = InvUtils.find(item -> item.getItem() instanceof BlockItem && blocks.get().contains(((BlockItem) item.getItem()).getBlock()));
            Hand hand = isValid(Managers.HOLDING.getStack()) ? Hand.MAIN_HAND : isValid(mc.player.getOffHandStack()) ? Hand.OFF_HAND : null;

            if ((hand != null || (switchMode.get() == SwitchMode.SilentBypass && inventory.slot() >= 0) || ((switchMode.get() == SwitchMode.Silent ||switchMode.get() == SwitchMode.Normal) && hotbar.slot() >= 0)) &&
                (!pauseEat.get() || !mc.player.isUsingItem()) && placesLeft > 0 && !placements.isEmpty()) {


                List<BlockPos> toPlace = new ArrayList<>();
                for (BlockPos placement : placements) {
                    if (toPlace.size() < placesLeft && canPlace(placement)) {
                        toPlace.add(placement);
                    }
                }

                if (!toPlace.isEmpty()) {
                    int obsidian = hand == Hand.MAIN_HAND ? Managers.HOLDING.getStack().getCount() :
                        hand == Hand.OFF_HAND ? mc.player.getOffHandStack().getCount() : -1;

                    if (hand == null) {
                        switch (switchMode.get()) {
                            case Silent, Normal -> {
                                obsidian = hotbar.count();
                                InvUtils.swap(hotbar.slot(), true);
                            }
                            case SilentBypass -> {
                                obsidian = BOInvUtils.invSwitch(inventory.slot()) ? inventory.count() : -1;
                            }
                        }
                    }
                    if (obsidian >= 0) {
                        for (int i = 0; i < Math.min(obsidian, toPlace.size()); i++) {
                            place(toPlace.get(i));
                        }
                        if (hand == null) {
                            switch (switchMode.get()) {
                                case Silent -> {
                                    InvUtils.swapBack();
                                }
                                case SilentBypass -> {
                                    BOInvUtils.swapBack();
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    boolean canPlace(BlockPos pos) {
        Direction[] dir = SettingUtils.getPlaceDirection(pos);
        if (dir[0] == null && dir[1] == null) {return false;}
        return true;
    }

    void place(BlockPos toPlace) {
        Direction[] result = SettingUtils.getPlaceDirection(toPlace);
        if (result[0] != null || result[1] != null) {
            timers.add(toPlace, delay.get());
            placeTimer = 0;
            placesLeft--;
            if (result[1] != null) {
                if (SettingUtils.shouldRotate(RotationType.Placing)) {
                    Managers.ROTATION.start(toPlace, 1);
                }

                SettingUtils.swing(SwingState.Pre, SwingType.Placing);

                mc.player.networkHandler.sendPacket(new PlayerInteractBlockC2SPacket(Hand.MAIN_HAND,
                    new BlockHitResult(new Vec3d(toPlace.getX() + 0.5, toPlace.getY() + 0.5, toPlace.getZ() + 0.5),
                        result[1], toPlace, false), 0));

                SettingUtils.swing(SwingState.Post, SwingType.Placing);

                if (SettingUtils.shouldRotate(RotationType.Placing)) {
                    Managers.ROTATION.end(toPlace);
                }
            } else {
                if (SettingUtils.shouldRotate(RotationType.Placing)) {
                    Managers.ROTATION.start(toPlace.offset(result[0]), 1);
                }

                SettingUtils.swing(SwingState.Pre, SwingType.Placing);

                mc.player.networkHandler.sendPacket(new PlayerInteractBlockC2SPacket(Hand.MAIN_HAND,
                    new BlockHitResult(new Vec3d(toPlace.offset(result[0]).getX() + 0.5, toPlace.offset(result[0]).getY() + 0.5, toPlace.offset(result[0]).getZ() + 0.5),
                        result[0].getOpposite(), toPlace.offset(result[0]), false), 0));

                SettingUtils.swing(SwingState.Post, SwingType.Placing);

                if (SettingUtils.shouldRotate(RotationType.Placing)) {
                    Managers.ROTATION.end(toPlace.offset(result[0]));
                }
            }
        }
    }

    boolean isValid(ItemStack item) {
        return item.getItem() instanceof BlockItem && blocks.get().contains(((BlockItem) item.getItem()).getBlock());
    }

    List<BlockPos> check() {
        List<BlockPos> list = new ArrayList<>();
        List<BlockPos> renders = new ArrayList<>();
        List<BlockPos> blocks = getBlocks(getSize());
        if (mc.player != null && mc.world != null) {
            for (BlockPos position : blocks) {
                if (mc.world.getBlockState(position).getBlock().equals(Blocks.AIR)) {
                    if (!timers.contains(position) && !EntityUtils.intersectsWithEntity(OLEPOSSUtils.getBox(position), entity -> !entity.isSpectator() && entity.getType() != EntityType.ITEM)) {
                        list.add(position);
                    }
                    renders.add(position);
                }
            }
        }
        render = renders;
        return list;
    }

    int[] getSize() {
        if (mc.player == null || mc.world == null) {return new int[]{0, 0, 0, 0};}

        Vec3d offset = mc.player.getPos().add(-mc.player.getBlockX(), -mc.player.getBlockY(), -mc.player.getBlockZ());
        return new int[]{offset.x < 0.3 ? -1 : 0, offset.x > 0.7 ? 1 : 0, offset.z < 0.3 ? -1 : 0, offset.z > 0.7 ? 1 : 0};
    }

    List<BlockPos> getBlocks(int[] size) {
        List<BlockPos> list = new ArrayList<>();
        if (mc.player != null && mc.world != null) {
            BlockPos pPos = mc.player.getBlockPos();
            for (int x = size[0] - 1; x <= size[1] + 1; x++) {
                for (int z = size[2] - 1; z <= size[3] + 1; z++) {
                    boolean isX = x == size[0] - 1 || x == size[1] + 1;
                    boolean isZ = z == size[2] - 1 || z == size[3] + 1;
                    boolean ignore = (isX && !isZ ? !air(pPos.add(OLEPOSSUtils.closerToZero(x), 0, z)) :
                        !isX && isZ && !air(pPos.add(x, 0, OLEPOSSUtils.closerToZero(z)))) && !(x == 0 && z == 0);
                    if (isX != isZ && !ignore) {
                        list.add(pPos.add(x, 0, z));
                    } else if (!isX && !isZ && floor.get() && air(pPos.add(x, 0, z))) {
                        list.add(pPos.add(x, -1, z));
                    }
                }
            }
        }
        return list;
    }

    boolean air(BlockPos pos) {return mc.world.getBlockState(pos).getBlock().equals(Blocks.AIR);}

    int[] findObby() {
        int num = 0;
        int slot = 0;
        if (mc.player != null) {
            for (int i = 0; i < 9; i++) {
                ItemStack stack = mc.player.getInventory().getStack(i);
                if (stack.getCount() > num && stack.getItem().equals(Items.OBSIDIAN)) {
                    num = stack.getCount();
                    slot = i;
                }
            }
        }
        return new int[] {slot, num};
    }
}
