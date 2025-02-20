package kassuk.addon.blackout.modules;

import kassuk.addon.blackout.BlackOut;
import kassuk.addon.blackout.BlackOutModule;
import meteordevelopment.meteorclient.events.entity.player.PlayerMoveEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.mixininterface.IVec3d;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.systems.modules.world.Timer;
import meteordevelopment.orbit.EventHandler;


public class FlightPlus extends BlackOutModule {

    public FlightPlus() {super(BlackOut.BLACKOUT, "Flight+", "KasumsSoft Flight");}
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<FlightMode> flyMode = sgGeneral.add(new EnumSetting.Builder<FlightMode>()
        .name("Rotation mode")
        .description(".")
        .defaultValue(FlightMode.Momentum)
        .build()
    );
    private final Setting<Boolean> useTimer = sgGeneral.add(new BoolSetting.Builder()
        .name("Use Timer")
        .description("Should we use timer")
        .defaultValue(true)
        .build()
    );
    private final Setting<Double> timer = sgGeneral.add(new DoubleSetting.Builder()
        .visible(useTimer::get)
        .name("Timer")
        .description("Speed but better")
        .defaultValue(1.088)
        .min(0)
        .sliderMax(10)
        .build()
    );
    private final Setting<Double> speed = sgGeneral.add(new DoubleSetting.Builder()
        .name("Speed")
        .description("Speed")
        .defaultValue(0.6)
        .min(0)
        .sliderMax(10)
        .build()
    );
    private final Setting<Double> ySpeed = sgGeneral.add(new DoubleSetting.Builder()
        .name("Y Speed")
        .description("DA Y SPEEDOS")
        .defaultValue(0.5)
        .min(0)
        .sliderMax(10)
        .build()
    );
    private final Setting<Double> antiKickDelay = sgGeneral.add(new DoubleSetting.Builder()
        .name("Antikick delay")
        .description("How long of an delay should be used")
        .defaultValue(10)
        .min(0)
        .sliderMax(100)
        .build()
    );
    private final Setting<Double> antiKickAmount = sgGeneral.add(new DoubleSetting.Builder()
        .name("Antikick amount")
        .description("How much to move")
        .defaultValue(1)
        .min(0)
        .sliderMax(10)
        .build()
    );
    private final Setting<Boolean> keepY = sgGeneral.add(new BoolSetting.Builder()
        .name("KeepY")
        .description("Should we try to keep the same y level when jump flying")
        .defaultValue(true)
        .build()
    );
    private final Setting<Double> glideAmount = sgGeneral.add(new DoubleSetting.Builder()
        .name("Glide amount")
        .description("How much to move")
        .defaultValue(0.2)
        .min(0)
        .sliderMax(1)
        .build()
    );


    public enum FlightMode {
        Momentum,
        Jump,
        Glide,
    }
    double startY = 0.0;
    int tick = 0;
    @Override
    public void onActivate() {
        if (mc.player != null && mc.world != null){
            startY = mc.player.getY();
            Modules.get().get(Timer.class).setOverride(timer.get());
        }
    }
    @EventHandler
    private void onMove(PlayerMoveEvent event){
        if (mc.player != null && mc.world != null){
            double[] result = getYaw(mc.player.input.movementForward, mc.player.input.movementSideways);
            float yaw = (float) result[0] + 90;
            double x = 0, y = tick % antiKickDelay.get() == 0 ? antiKickAmount.get() * -0.04 : 0, z = 0;
            if (flyMode.get().equals(FlightMode.Momentum)){
                if (mc.options.jumpKey.isPressed() && y == 0){
                    y = ySpeed.get();
                }
                else if (mc.options.sneakKey.isPressed()){
                    y = -ySpeed.get();
                }
                if (result[1] == 1){
                    x = Math.cos(Math.toRadians(yaw)) * speed.get();
                    z = Math.sin(Math.toRadians(yaw)) * speed.get();

                }
                ((IVec3d) event.movement).set(x, y, z);
            }
            if (flyMode.get().equals(FlightMode.Jump)){
                if (mc.options.jumpKey.wasPressed()){
                    mc.player.jump();
                    startY += 0.4;
                }
                if (mc.options.sneakKey.wasPressed() && !mc.options.sneakKey.isPressed()){
                    startY = mc.player.getY();
                }

                if (keepY.get() && mc.player.getY() <= startY && !mc.options.sneakKey.isPressed())
                    mc.player.jump();
                if (result[1] == 1){
                    x = Math.cos(Math.toRadians(yaw)) * speed.get();
                    z = Math.sin(Math.toRadians(yaw)) * speed.get();

                }
                ((IVec3d) event.movement).setXZ(x, z);
            }
            if (flyMode.get().equals(FlightMode.Glide)){
                if (!mc.player.isOnGround())
                    ((IVec3d) event.movement).set(mc.player.getVelocity().getX(),-glideAmount.get(), mc.player.getVelocity().getZ());

            }
        }
    }

    @EventHandler
    private void onTick(TickEvent.Pre event){
        tick++;

    }

    @Override
    public void onDeactivate(){
        if (mc.player != null && mc.world != null){
            Modules.get().get(Timer.class).setOverride(1);
        }
    }
    double[] getYaw(double f, double s) {
        double yaw = mc.player.getYaw();
        double move;
        if (f > 0) {
            move = 1;
            yaw += s > 0 ? -45 : s < 0 ? 45 : 0;
        } else if (f < 0) {
            move = 1;
            yaw += s > 0 ? -135 : s < 0 ? 135 : 180;
        } else {
            move = s != 0 ? 1 : 0;
            yaw += s > 0 ? -90 : s < 0 ? 90 : 0;
        }
        return new double[]{yaw, move};
    }
}
