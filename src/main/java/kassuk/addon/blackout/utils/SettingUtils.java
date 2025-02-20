package kassuk.addon.blackout.utils;

import kassuk.addon.blackout.enums.RotationType;
import kassuk.addon.blackout.enums.SwingState;
import kassuk.addon.blackout.enums.SwingType;
import kassuk.addon.blackout.globalsettings.*;
import kassuk.addon.blackout.managers.RotationManager;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.utils.Utils;
import net.minecraft.block.BlockState;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;

import java.util.function.Predicate;
import static meteordevelopment.meteorclient.MeteorClient.mc;

public class SettingUtils extends Utils {

    private static final FacingSettings facing = Modules.get().get(FacingSettings.class);
    private static final RangeSettings range = Modules.get().get(RangeSettings.class);
    private static final RaytraceSettings raytrace = Modules.get().get(RaytraceSettings.class);
    private static final RotationSettings rotation = Modules.get().get(RotationSettings.class);
    private static final SwingSettings swing = Modules.get().get(SwingSettings.class);

    //  Range
    public static void registerAttack(Box bb) {range.registerAttack(bb);}
    public static double getPlaceRange() {
        return range.placeRange.get();
    }
    public static double getPlaceWallsRange() {
        return range.placeRangeWalls.get();
    }
    public static double getAttackRange() {
        return range.attackRange.get();
    }
    public static double getAttackWallsRange() {
        return range.attackRangeWalls.get();
    }
    public static double getMineRange() {
        return range.miningRange.get();
    }
    public static double getMineWallsRange() {
        return range.miningRangeWalls.get();
    }
    public static double placeRangeTo(BlockPos pos) {return range.placeRangeTo(pos, null);}
    public static boolean inPlaceRange(BlockPos pos) {return range.inPlaceRange(pos, null);}
    public static boolean inPlaceRange(BlockPos pos, Vec3d from) {
        return range.inPlaceRange(pos, from);
    }
    public static boolean inPlaceRangeNoTrace(BlockPos pos) {
        return range.inPlaceRangeNoTrace(pos, null);
    }
    public static boolean inPlaceRangeNoTrace(BlockPos pos, Vec3d from) {
        return range.inPlaceRangeNoTrace(pos, from);
    }
    public static boolean inAttackRange(Box bb) {
        return range.inAttackRange(bb, null);
    }
    public static boolean inAttackRange(Box bb, Vec3d from) {
        return range.inAttackRange(bb, from);
    }
    public static double mineRangeTo(BlockPos pos) {return range.miningRangeTo(pos, null);}
    public static boolean inMineRange(BlockPos pos) {
        return range.inMineRange(pos);
    }
    public static boolean inMineRangeNoTrace(BlockPos pos) {
        return range.inMineRangeNoTrace(pos);
    }
    public static boolean inAttackRangeNoTrace(Box bb, double eyeHeight, Vec3d feet) {return range.inAttackRangeNoTrace(bb, feet, null);}
    public static boolean inAttackRangeNoTrace(Box bb, double eyeHeight, Vec3d feet, Vec3d from) {return range.inAttackRangeNoTrace(bb, feet, from);}
    public static double attackRangeTo(Box bb, Vec3d feet) {return range.attackRangeTo(bb, feet, null, true);}


    //  Rotate
    public static boolean startMineRot() {
        return rotation.startMineRot();
    }
    public static boolean endMineRot() {
        return rotation.endMineRot();
    }
    public static boolean shouldGhostRotate() {return rotation.ghostRotation.get();}
    public static boolean shouldGhostCheck() {return rotation.rotationCheckMode.get() == RotationSettings.RotationCheckMode.Ghost;}
    public static boolean shouldVanillaRotate() {return rotation.vanillaRotation.get();}
    public static Vec3d getGhostRot(BlockPos pos, Vec3d vec) {return rotation.getGhostRot(pos, vec);}
    public static Vec3d getGhostRot(Box box, Vec3d vec) {return rotation.getGhostRot(box, vec);}
    public static boolean shouldRotate(RotationType type) {
        return rotation.shouldRotate(type);
    }
    public static boolean rotationCheckHistory(Box box, RotationType type) {return rotation.rotationCheckHistory(box, rotation.NCPRotation.get() ? rotation.NCPPackets.get() : rotation.getExisted(type) + 1);}
    public static boolean rotationCheck(Vec3d pPos, double yaw, double pitch, Box box, RotationType type, boolean ncp) {return rotation.rotationCheck(pPos, yaw, pitch, box, rotation.NCPRotation.get() ? rotation.NCPPackets.get() : rotation.getExisted(type), ncp);}
    public static boolean raytraceCheck(Vec3d vec, double yaw, double pitch, Box box) {return rotation.raytraceCheck(vec, yaw, pitch, box);}
    public static boolean raytraceCheck(Vec3d vec, double yaw, double pitch, BlockPos pos) {return rotation.raytraceCheck(vec, yaw, pitch, pos);}

    //  Swing
    public static void swing(SwingState state, SwingType type, Hand hand) {
        swing.swing(state, type, hand);
    }
    public static void mineSwing(SwingSettings.MiningSwingState state) {
        swing.mineSwing(state);
    }

    //  Facing
    public static PlaceData getPlaceData(BlockPos pos) {return facing.getPlaceData(pos, true);}
    public static PlaceData getPlaceDataAND(BlockPos pos, Predicate<BlockPos> predicate) {return facing.getPlaceDataAND(pos, predicate, true);}
    public static PlaceData getPlaceDataOR(BlockPos pos, Predicate<BlockPos> predicate) {return facing.getPlaceDataOR(pos, predicate, true);}
    public static PlaceData getPlaceData(BlockPos pos, boolean ignoreContainers) {return facing.getPlaceData(pos, ignoreContainers);}
    public static PlaceData getPlaceDataAND(BlockPos pos, Predicate<BlockPos> predicate, boolean ignoreContainers) {return facing.getPlaceDataAND(pos, predicate, ignoreContainers);}
    public static PlaceData getPlaceDataOR(BlockPos pos, Predicate<BlockPos> predicate, boolean ignoreContainers) {return facing.getPlaceDataOR(pos, predicate, ignoreContainers);}
    public static Direction getPlaceOnDirection(BlockPos pos) {
        return facing.getPlaceOnDirection(pos);
    }

    //  Raytracing
    public static boolean shouldPlaceTrace() {
        return raytrace.placeTrace.get();
    }
    public static boolean shouldAttackTrace() {return raytrace.attackTrace.get();}
    public static boolean placeTrace(BlockPos pos) {
        return !shouldPlaceTrace() || raytrace.placeTrace(pos);
    }
    public static boolean attackTrace(Box bb) {
        return !shouldAttackTrace() || raytrace.attackTrace(bb);
    }
}
