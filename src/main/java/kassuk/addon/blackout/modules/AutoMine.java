package kassuk.addon.blackout.modules;

import kassuk.addon.blackout.BlackOut;
import kassuk.addon.blackout.enums.RotationType;
import kassuk.addon.blackout.enums.SwingState;
import kassuk.addon.blackout.enums.SwingType;
import kassuk.addon.blackout.managers.Managers;
import kassuk.addon.blackout.utils.BOInvUtils;
import kassuk.addon.blackout.utils.OLEPOSSUtils;
import kassuk.addon.blackout.utils.SettingUtils;
import meteordevelopment.meteorclient.events.packets.PacketEvent;
import meteordevelopment.meteorclient.events.render.Render3DEvent;
import meteordevelopment.meteorclient.events.world.BlockUpdateEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.renderer.ShapeMode;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.friends.Friends;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.entity.EntityUtils;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import meteordevelopment.meteorclient.utils.render.color.Color;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import meteordevelopment.orbit.EventHandler;
import meteordevelopment.orbit.EventPriority;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.decoration.EndCrystalEntity;
import net.minecraft.entity.effect.StatusEffectUtil;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerInteractBlockC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerInteractEntityC2SPacket;
import net.minecraft.network.packet.c2s.play.UpdateSelectedSlotC2SPacket;
import net.minecraft.tag.FluidTags;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;

/*
Made by OLEPOSSU / Raksamies
*/

public class AutoMine extends Module {
    public AutoMine() {super(BlackOut.BLACKOUT, "AutoMine", "For the times your too lazy or bad to press your break bind");}
    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgValue = settings.createGroup("Value");
    private final SettingGroup sgSpeed = settings.createGroup("Speed");
    private final SettingGroup sgCrystal = settings.createGroup("Crystal");
    private final SettingGroup sgRender = settings.createGroup("Render");
    private final Setting<Boolean> pauseEat = sgGeneral.add(new BoolSetting.Builder()
        .name("Pause Eat")
        .description("Pauses when you are eating")
        .defaultValue(true)
        .build()
    );
    private final Setting<Boolean> resetOnSwitch = sgGeneral.add(new BoolSetting.Builder()
        .name("Reset On Switch")
        .description("Resets mining progress when switching (useful on strict)")
        .defaultValue(false)
        .build()
    );

    //  Value Page
    private final Setting<Integer> antiSurround = sgValue.add(new IntSetting.Builder()
        .name("Anti Surround Value")
        .description("0 = disabled")
        .defaultValue(0)
        .range(0, 100)
        .sliderRange(0, 100)
        .build()
    );
    private final Setting<Integer> surroundCev = sgValue.add(new IntSetting.Builder()
        .name("Surround Cev Value")
        .description("0 = disabled")
        .defaultValue(0)
        .range(0, 100)
        .sliderRange(0, 100)
        .build()
    );
    private final Setting<Integer> trapCev = sgValue.add(new IntSetting.Builder()
        .name("Trap Cev Value")
        .description("0 = disabled")
        .defaultValue(0)
        .range(0, 100)
        .sliderRange(0, 100)
        .build()
    );
    private final Setting<Integer> autoCity = sgValue.add(new IntSetting.Builder()
        .name("Auto City Value")
        .description("0 = disabled")
        .defaultValue(0)
        .range(0, 100)
        .sliderRange(0, 100)
        .build()
    );
    private final Setting<Integer> cev = sgValue.add(new IntSetting.Builder()
        .name("Cev Value")
        .description("0 = disabled")
        .defaultValue(0)
        .range(0, 100)
        .sliderRange(0, 100)
        .build()
    );
    private final Setting<Integer> antiBurrow = sgValue.add(new IntSetting.Builder()
        .name("Anti Burrow Value")
        .description("0 = disabled")
        .defaultValue(0)
        .range(0, 100)
        .sliderRange(0, 100)
        .build()
    );

    //  Speed Page
    private final Setting<Double> speed = sgSpeed.add(new DoubleSetting.Builder()
        .name("Speed")
        .description("Vanilla mining speed multiplier")
        .defaultValue(1)
        .range(0.1, 10)
        .sliderMax(10)
        .build()
    );
    private final Setting<Boolean> onGroundCheck = sgSpeed.add(new BoolSetting.Builder()
        .name("On Ground Check")
        .description("Divides mining speed by 5 when not on ground.")
        .defaultValue(true)
        .build()
    );
    private final Setting<Boolean> waterCheck = sgSpeed.add(new BoolSetting.Builder()
        .name("Water Check")
        .description("Divides mining speed by 5 when underwater.")
        .defaultValue(true)
        .build()
    );
    private final Setting<Boolean> hasteCheck = sgSpeed.add(new BoolSetting.Builder()
        .name("Haste Check")
        .description("More speed when you have haste effect.")
        .defaultValue(true)
        .build()
    );
    private final Setting<Boolean> mfCheck = sgSpeed.add(new BoolSetting.Builder()
        .name("Mining Fatigue Check")
        .description("Slower when you have mining fatigue.")
        .defaultValue(true)
        .build()
    );

    //  Crystal Page
    private final Setting<Boolean> crystal = sgCrystal.add(new BoolSetting.Builder()
        .name("Crystal")
        .description("Requires crystal to be placed to end mining.")
        .defaultValue(true)
        .build()
    );
    private final Setting<Boolean> placeCrystal = sgCrystal.add(new BoolSetting.Builder()
        .name("Place Crystal")
        .description("Places crystal before ending the mining.")
        .defaultValue(true)
        .build()
    );
    private final Setting<Boolean> expCrystal = sgCrystal.add(new BoolSetting.Builder()
        .name("Explode Crystal")
        .description("Explodes a crystal before ending the mining.")
        .defaultValue(true)
        .build()
    );
    private final Setting<Double> placeDelay = sgCrystal.add(new DoubleSetting.Builder()
        .name("Place Delay")
        .description("Delay between crystal places")
        .defaultValue(0.3)
        .range(0, 1)
        .sliderRange(0, 1)
        .build()
    );

    //  Render Page
    private final Setting<Boolean> smooth = sgRender.add(new BoolSetting.Builder()
        .name("Smooth Color")
        .description("Smoothly changes the color.")
        .defaultValue(true)
        .build()
    );
    private final Setting<ShapeMode> shapeMode = sgRender.add(new EnumSetting.Builder<ShapeMode>()
        .name("Shape Mode")
        .description(".")
        .defaultValue(ShapeMode.Both)
        .build()
    );
    private final Setting<SettingColor> lineStartColor = sgRender.add(new ColorSetting.Builder()
        .name("Line Start Color")
        .description(".")
        .defaultValue(new SettingColor(255, 0, 0, 0))
        .build()
    );
    private final Setting<SettingColor> lineEndColor = sgRender.add(new ColorSetting.Builder()
        .name("Line End Color")
        .description(".")
        .defaultValue(new SettingColor(255, 0, 0, 255))
        .build()
    );
    private final Setting<SettingColor> startColor = sgRender.add(new ColorSetting.Builder()
        .name("Start Color")
        .description(".")
        .defaultValue(new SettingColor(255, 0, 0, 0))
        .build()
    );
    private final Setting<SettingColor> endColor = sgRender.add(new ColorSetting.Builder()
        .name("End Color")
        .description(".")
        .defaultValue(new SettingColor(255, 0, 0, 50))
        .build()
    );

    double progress;
    BlockPos targetPos;
    BlockPos crystalPos;
    int targetValue;
    int lastValue;
    double timer = 0;
    double miningFor;

    @Override
    public void onActivate() {
        super.onActivate();
        timer = 0;
        progress = 0;
        targetPos = null;
        crystalPos = null;
        targetValue = -1;
        lastValue = -1;
    }
    @EventHandler(priority = EventPriority.HIGHEST)
    private void onTick(TickEvent.Pre event) {
        if (targetPos != null) {
            SettingUtils.swing(SwingState.Pre, SwingType.Mining);
            if (SettingUtils.shouldRotate(RotationType.Breaking)) {
                Managers.ROTATION.start(OLEPOSSUtils.getBox(targetPos), 9);
            }
        }
    }
    @EventHandler(priority = EventPriority.HIGHEST)
    private void onTick(TickEvent.Post event) {
        if (targetPos != null) {
            SettingUtils.swing(SwingState.Post, SwingType.Mining);
        }
    }
    @EventHandler(priority = EventPriority.HIGHEST)
    private void onSwitch(PacketEvent.Send event) {
        if (event.packet instanceof UpdateSelectedSlotC2SPacket && resetOnSwitch.get() && targetPos != null) {
            targetPos = null;
            crystalPos = null;
            progress = 0;
            miningFor = 0;
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    private void onRender(Render3DEvent event) {
        if (mc.player == null || mc.world == null) {return;}

        calc();
        timer = Math.min(placeDelay.get(), timer + event.frameTime);

        if (targetPos != null) {
            miningFor += event.frameTime;
            double r = 1 - progress;
            progress = Math.min(1, progress + event.frameTime * getMineTicks(targetPos) * 20);
            //Render
            Vec3d v = OLEPOSSUtils.getMiddle(targetPos);
            double p = 0.5 - r * r * r / 2;
            int[] cl = getColor(lineStartColor.get(), lineEndColor.get(), smooth.get() ? p * 2 : Math.floor(p));
            int[] c = getColor(startColor.get(), endColor.get(), smooth.get() ? p * 2 : Math.ceil(p));

            Box toRender = new Box(v.x - p, v.y - p, v.z - p, v.x + p, v.y + p, v.z + p);
            event.renderer.box(toRender, new Color(cl[0], cl[1], cl[2],
                    (int) Math.floor(c[3] / 5f)),
                new Color(c[0], c[1], c[2], c[3]), shapeMode.get(), 0);

            //Other Stuff
            if (!SettingUtils.inPlaceRange(targetPos)) {
                calc();
            }
            if (progress >= 1 && (!pauseEat.get() || !mc.player.isUsingItem())) {
                if (crystal.get() && crystalPos != null) {
                    Entity at = isAt(targetPos, crystalPos);
                    Hand hand = getHand(Items.END_CRYSTAL);
                    int cSlot = InvUtils.findInHotbar(itemStack -> itemStack.getItem() == Items.END_CRYSTAL).slot();
                    if (at != null) {
                        int holding = 0;
                        if (holdingBest(targetPos)) {
                            holding = 1;
                        }
                        else if (BOInvUtils.invSwitch(fastestSlot(targetPos))){
                            holding = 2;
                        }
                        if (holding == 0) {return;}
                        end(targetPos);
                        if (holding == 2) {
                            BOInvUtils.swapBack();
                        }
                        if (expCrystal.get() && at instanceof EndCrystalEntity) {
                            attackID(at.getId(), at.getPos());
                        }
                        targetPos = null;
                        crystalPos = null;
                    } else if ((cSlot >= 0 || hand != null) && timer >= placeDelay.get() && placeCrystal.get()
                        && placeCrystal.get() && !EntityUtils.intersectsWithEntity(new Box(crystalPos), entity -> !entity.isSpectator())) {
                        timer = 0;
                        if (SettingUtils.shouldRotate(RotationType.Crystal)) {
                            Managers.ROTATION.start(crystalPos.down(), 9);
                        }

                        int holding = 0;
                        if (holdingBest(targetPos)) {
                            holding = 1;
                        }
                        else if (BOInvUtils.invSwitch(cSlot)){
                            holding = 2;
                        }
                        if (holding == 0) {return;}


                        SettingUtils.swing(SwingState.Pre, SwingType.Crystal);

                        mc.player.networkHandler.sendPacket(new PlayerInteractBlockC2SPacket(hand == null ? Hand.MAIN_HAND : hand,
                            new BlockHitResult(OLEPOSSUtils.getMiddle(crystalPos.down()), Direction.UP, crystalPos.down(), false), 0));

                        SettingUtils.swing(SwingState.Post, SwingType.Crystal);

                        if (holding == 2) {
                            BOInvUtils.swapBack();
                        }

                        if (SettingUtils.shouldRotate(RotationType.Crystal)) {
                            Managers.ROTATION.end(crystalPos.down());
                        }
                    }
                } else {
                    int holding = 0;
                    if (holdingBest(targetPos)) {
                        holding = 1;
                    }
                    else if (BOInvUtils.invSwitch(fastestSlot(targetPos))){
                        holding = 2;
                    }
                    if (holding == 0) {return;}
                    end(targetPos);
                    if (holding == 2) {
                        BOInvUtils.swapBack();
                    }
                    targetPos = null;
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    private void onBlock(BlockUpdateEvent event) {
        if (mc.player != null && mc.world != null && targetPos != null) {
            if (event.newState.getBlock() != Blocks.AIR && event.oldState.getBlock() == Blocks.AIR) {
                if (event.pos == targetPos) {
                    progress = 0;
                }
            }
        }
    }

    void attackID(int id, Vec3d pos) {
        EndCrystalEntity en = new EndCrystalEntity(mc.world, pos.x, pos.y, pos.z);
        en.setId(id);
        Box box = new Box(pos.getX() - 1, pos.getY(), pos.getZ() - 1, pos.getX() + 1, pos.getY() + 2, pos.getZ() + 1);

        if (SettingUtils.shouldRotate(RotationType.Attacking)) {
            Managers.ROTATION.start(box, 9);
        }

        SettingUtils.swing(SwingState.Post, SwingType.Attacking);

        mc.getNetworkHandler().sendPacket(PlayerInteractEntityC2SPacket.attack(en, mc.player.isSneaking()));

        SettingUtils.swing(SwingState.Post, SwingType.Attacking);

        if (SettingUtils.shouldRotate(RotationType.Attacking)) {
            Managers.ROTATION.end(box);
        }
    }

    Entity isAt(BlockPos pos, BlockPos crystalPos) {
        for (Entity en : mc.world.getEntities()) {
            if ((en.getBlockPos().equals(crystalPos) && en.getType().equals(EntityType.END_CRYSTAL)) ||
                (!(en instanceof ItemEntity) &&  en.getBoundingBox().intersects(OLEPOSSUtils.getBox(pos)))) {
                return en;
            }
        }
        return null;
    }

    void calc() {
        lastValue = targetValue;
        BlockPos[] pos = getPos();
        BlockPos pos1 = pos[0];
        BlockPos pos2 = pos[1];
        boolean valid = targetPos == null;
        if (!valid) {
            valid = getBlock(targetPos) == Blocks.AIR;
            if (!valid && crystalPos != null) {
                valid = getBlock(crystalPos) != Blocks.AIR || !OLEPOSSUtils.isCrystalBlock(getBlock(crystalPos.down()));
            }
        }
        if (pos1 != null) {
            if ((!pos1.equals(targetPos) && (targetValue == -1 || (targetValue > lastValue)) && SettingUtils.inPlaceRange(pos1)) || valid) {
                progress = 0;
                miningFor = 0;
                targetPos = pos1;
                crystalPos = pos2;
                start(targetPos);
            }
        } else {
            reset();
        }
    }

    void reset() {
        targetPos = null;
        lastValue = -1;
        targetValue = -1;
    }

    BlockPos[] getPos() {
        int value = 0;
        BlockPos closest = null;
        BlockPos crystal = null;
        for (PlayerEntity pl : mc.world.getPlayers()) {
            if (pl != mc.player && !Friends.get().isFriend(pl)) {
                BlockPos pos = pl.getBlockPos();
                for (Direction dir : OLEPOSSUtils.horizontals) {
                    // Anti Surround
                    if (valueCheck(value, antiSurround.get(), pos.offset(dir), closest)
                        && getBlock(pos.offset(dir)) != Blocks.AIR) {
                        value = antiSurround.get();
                        closest = pos.offset(dir);
                        crystal = null;
                    }

                    // Surround Cev
                    if (valueCheck(value, surroundCev.get(), pos.offset(dir), closest) &&
                        getBlock(pos.offset(dir)) == Blocks.OBSIDIAN && getBlock(pos.offset(dir).up()) == Blocks.AIR) {
                        value = surroundCev.get();
                        closest = pos.offset(dir);
                        crystal = pos.offset(dir).up();
                    }
                    // Trap Cev
                    if (valueCheck(value, trapCev.get(), pos.offset(dir).up(), closest) &&
                        getBlock(pos.offset(dir).up()) == Blocks.OBSIDIAN && getBlock(pos.offset(dir).up(2)) != Blocks.AIR) {
                        value = trapCev.get();
                        closest = pos.offset(dir).up();
                        crystal = pos.offset(dir).up(2);
                    }
                    // Auto City
                    if (valueCheck(value, autoCity.get(), pos.offset(dir), closest) &&
                        getBlock(pos.offset(dir)) != Blocks.AIR && getBlock(pos.offset(dir).offset(dir)) == Blocks.AIR &&
                        OLEPOSSUtils.isCrystalBlock(getBlock(pos.offset(dir).offset(dir).down()))) {
                        value = autoCity.get();
                        closest = pos.offset(dir);
                        crystal = pos.offset(dir).offset(dir);
                    }
                    // Cev
                    if (valueCheck(value, cev.get(), pos.up(2), closest)
                        && getBlock(pos.up(2)) == Blocks.OBSIDIAN && getBlock(pos.up(3)) == Blocks.AIR) {
                        value = cev.get();
                        closest = pos.up(2);
                        crystal = pos.up(3);
                    }
                    // Anti Burrow
                    if (valueCheck(value, antiBurrow.get(), pos, closest)
                        && getBlock(pl.getBlockPos()) != Blocks.AIR) {
                        value = antiBurrow.get();
                        closest = pl.getBlockPos();
                        crystal = null;
                    }
                }
            }
        }
        targetValue = value;
        return new BlockPos[] {closest, crystal};
    }

    boolean valueCheck(int currentValue, int value, BlockPos pos, BlockPos closest) {
        if (value == 0) {return false;}
        boolean rur;
        if (closest == null) {
            rur = true;
        } else {
            rur = OLEPOSSUtils.distance(OLEPOSSUtils.getMiddle(pos), mc.player.getEyePos()) <
                OLEPOSSUtils.distance(OLEPOSSUtils.getMiddle(closest), mc.player.getEyePos());
        }
        return ((currentValue <= value && rur) || currentValue < value) && SettingUtils.inPlaceRange(pos);
    }

    Block getBlock(BlockPos pos) {
        return mc.world.getBlockState(pos).getBlock();
    }

    Hand getHand(Item item) {
        if (mc.player.getOffHandStack().getItem() == item) {
            return Hand.OFF_HAND;
        } else if (Managers.HOLDING.isHolding(Items.END_CRYSTAL)) {
            return Hand.MAIN_HAND;
        }
        return null;
    }


    boolean holdingBest(BlockPos pos) {
        return miningFor >= getItemMineTicks(pos, Managers.HOLDING.getSlot()) / 20;
    }


    void start(BlockPos pos) {
        if (SettingUtils.startMineRot()) {
            Managers.ROTATION.start(pos, 9);
        }
        mc.getNetworkHandler().sendPacket(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.START_DESTROY_BLOCK,
            pos, Direction.UP));
        if (SettingUtils.startMineRot()) {
            Managers.ROTATION.end(pos);
        }
    }

    void end(BlockPos pos) {
        if (SettingUtils.endMineRot()) {
            Managers.ROTATION.start(pos, 9);
        }
        mc.getNetworkHandler().sendPacket(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.STOP_DESTROY_BLOCK,
            pos, Direction.UP));
        if (SettingUtils.endMineRot()) {
            Managers.ROTATION.end(pos);
        }
    }

    float getTime(BlockPos pos, int slot) {
        BlockState state = mc.world.getBlockState(pos);
        float f = state.getHardness(mc.world, pos);
        if (f == -1.0F) {
            return 0.0F;
        } else {
            int i = !state.isToolRequired() || mc.player.getInventory().getStack(slot).isSuitableFor(state) ? 30 : 100;
            return getSpeed(state, slot) / f / (float) i;
        }
    }

    float getMineTicks(BlockPos pos) {
        int fastest = fastestSlot(pos);
        if (fastest != -1) {
            return (float) (getTime(pos ,fastest) * speed.get());
        }
        return -1;
    }
    float getItemMineTicks(BlockPos pos, int slot) {
        return (float) (1 / getTime(pos, slot) * speed.get());
    }
    float getSpeed(BlockState state, int slot) {
        ItemStack stack = mc.player.getInventory().getStack(slot);
        float f = mc.player.getInventory().getStack(slot).getMiningSpeedMultiplier(state);
        if (f > 1.0) {
            int i = EnchantmentHelper.getLevel(Enchantments.EFFICIENCY, stack);
            if (i > 0 && !stack.isEmpty()) {
                f += (float)(i * i + 1);
            }
        }

        if (hasteCheck.get() && StatusEffectUtil.hasHaste(mc.player)) {
            f *= 1.0 + (float)(StatusEffectUtil.getHasteAmplifier(mc.player) + 1) * 0.2F;
        }

        if (mfCheck.get() && mc.player.hasStatusEffect(StatusEffects.MINING_FATIGUE)) {
            f *= Math.pow(0.3, mc.player.getStatusEffect(StatusEffects.MINING_FATIGUE).getAmplifier() + 1);
        }

        if (waterCheck.get() && mc.player.isSubmergedIn(FluidTags.WATER) && !EnchantmentHelper.hasAquaAffinity(mc.player)) {
            f /= 5.0;
        }

        if (onGroundCheck.get() && !mc.player.isOnGround()) {
            f /= 5.0;
        }

        return f;
    }

    int fastestSlot(BlockPos pos) {
        int slot = -1;
        if (mc.player == null || mc.world == null) {return -1;}
        for (int i = 0; i < 35; i++) {
            if (slot == -1 || (mc.player.getInventory().getStack(i).getMiningSpeedMultiplier(mc.world.getBlockState(pos)) >
                mc.player.getInventory().getStack(slot).getMiningSpeedMultiplier(mc.world.getBlockState(pos)))) {
                slot = i;
            }
        }
        return slot;
    }

    int[] getColor(Color start, Color end, double progress) {
        double r = (end.r - start.r) * progress;
        double g = (end.g - start.g) * progress;
        double b = (end.b - start.b) * progress;
        double a = (end.a - start.a) * progress;
        return new int[] {
            (int) Math.round(start.r + r),
            (int) Math.round(start.g + g),
            (int) Math.round(start.b + b),
            (int) Math.round(start.a + a)};
    }
}
