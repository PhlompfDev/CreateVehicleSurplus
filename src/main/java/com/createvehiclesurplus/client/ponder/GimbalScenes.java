package com.createvehiclesurplus.client.ponder;

import com.createvehiclesurplus.content.gimbal.GimbalControllerBlockEntity;
import com.simibubi.create.foundation.ponder.CreateSceneBuilder;
import net.createmod.ponder.api.PonderPalette;
import net.createmod.ponder.api.element.ElementLink;
import net.createmod.ponder.api.element.WorldSectionElement;
import net.createmod.ponder.api.scene.SceneBuilder;
import net.createmod.ponder.api.scene.SceneBuildingUtil;
import net.createmod.ponder.api.scene.Selection;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;

/**
 * Ponder storyboard for the Gimbal Controller. The Ponder level has no physics, so the bike is
 * an independent section that is rotated by hand: tipped over without the gimbal, held level
 * with it, then leaned a little to show that leaning is still allowed.
 */
public class GimbalScenes {
    private static final float SPEED = 32;
    private static final int TIP = 70;
    private static final int LEAN = 25;

    public static void gimbalController(SceneBuilder builder, SceneBuildingUtil util) {
        CreateSceneBuilder scene = new CreateSceneBuilder(builder);
        scene.title("gimbal_controller", "Balancing vehicles with the Gimbal Controller");
        scene.configureBasePlate(0, 0, 5);
        scene.showBasePlate();
        scene.idle(5);

        BlockPos gimbal = util.grid().at(2, 2, 2);
        Selection bikeSel = util.select().fromTo(0, 1, 0, 4, 3, 4);
        ElementLink<WorldSectionElement> bike = scene.world().showIndependentSection(bikeSel, Direction.DOWN);
        scene.world().configureCenterOfRotation(bike, util.vector().centerOf(2, 1, 2));
        scene.idle(15);

        scene.overlay().showText(70)
                .attachKeyFrame()
                .placeNearTarget()
                .pointAt(util.vector().topOf(gimbal))
                .text("Two-wheeled Aeronautics vehicles fall over as soon as they turn");
        scene.idle(30);
        scene.world().rotateSection(bike, TIP, 0, 0, 25);
        scene.idle(60);
        scene.world().rotateSection(bike, -TIP, 0, 0, 15);
        scene.idle(20);

        scene.world().setKineticSpeed(bikeSel, SPEED);
        // The Ponder level runs no physics: tell the block entity it is on a vehicle so the gyroscope animates.
        scene.world().modifyBlockEntity(gimbal, GimbalControllerBlockEntity.class, be -> be.setOnShip(true));
        scene.effects().rotationSpeedIndicator(gimbal);
        scene.overlay().showText(80)
                .colored(PonderPalette.GREEN)
                .attachKeyFrame()
                .placeNearTarget()
                .pointAt(util.vector().topOf(gimbal))
                .text("A turning Gimbal Controller, placed along the vehicle's forward axis, keeps it upright");
        scene.idle(90);

        scene.overlay().showText(80)
                .attachKeyFrame()
                .placeNearTarget()
                .pointAt(util.vector().topOf(gimbal))
                .text("It still leans into turns, like a real bike");
        scene.idle(10);
        // The gyroscope's outer frame stays level while the bike rolls; the inner frame shows the effort.
        scene.world().rotateSection(bike, LEAN, 0, 0, 20);
        feedLean(scene, gimbal, 0, LEAN, 0.8f);
        scene.world().modifyBlockEntity(gimbal, GimbalControllerBlockEntity.class, be -> be.setEffort(0.2));
        scene.idle(40);
        scene.world().rotateSection(bike, -LEAN, 0, 0, 20);
        feedLean(scene, gimbal, LEAN, 0, -0.8f);
        scene.world().modifyBlockEntity(gimbal, GimbalControllerBlockEntity.class, be -> be.setEffort(0));
        scene.idle(40);

        scene.overlay().showText(60)
                .placeNearTarget()
                .pointAt(util.vector().topOf(gimbal))
                .text("Heavier vehicles need more than one. Redstone switches it off");
        scene.idle(70);
    }

    /**
     * Walks the block entity's reported lean from {@code from} to {@code to} in four steps over the
     * 20 ticks the section takes to roll, with {@code effort} applied while it moves, so the
     * gyroscope's frame tracks the bike's roll. The client-side easing smooths the steps.
     */
    private static void feedLean(CreateSceneBuilder scene, BlockPos gimbal, float from, float to, float effort) {
        for (int step = 1; step <= 4; step++) {
            float lean = from + (to - from) * step / 4f;
            scene.world().modifyBlockEntity(gimbal, GimbalControllerBlockEntity.class, be -> {
                be.setLean(lean);
                be.setEffort(effort);
            });
            scene.idle(5);
        }
    }
}
