package com.createvehiclesurplus.client.ponder;

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
        scene.world().rotateSection(bike, LEAN, 0, 0, 20);
        scene.idle(40);
        scene.world().rotateSection(bike, -LEAN, 0, 0, 20);
        scene.idle(40);

        scene.overlay().showText(60)
                .placeNearTarget()
                .pointAt(util.vector().topOf(gimbal))
                .text("Heavier vehicles need more than one. Redstone switches it off");
        scene.idle(70);
    }
}
