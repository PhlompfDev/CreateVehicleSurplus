package com.createvehiclesurplus.client.ponder;

import com.createvehiclesurplus.content.transmission.Drive;
import com.createvehiclesurplus.content.transmission.GearSpeeds;
import com.createvehiclesurplus.content.transmission.TransmissionBlock;
import com.createvehiclesurplus.content.transmission.TransmissionBlockEntity;
import com.simibubi.create.foundation.ponder.CreateSceneBuilder;
import net.createmod.ponder.api.PonderPalette;
import net.createmod.ponder.api.scene.SceneBuilder;
import net.createmod.ponder.api.scene.SceneBuildingUtil;
import net.createmod.ponder.api.scene.Selection;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.NbtUtils;

/**
 * Ponder storyboard for the Transmission. The Ponder level never ticks the server side, so each
 * change is acted out: set the gear and drive properties, set the output speed, point at the output.
 */
public class TransmissionScenes {
    private static final float INPUT = 16;
    private static final GearSpeeds DEFAULTS = new GearSpeeds();

    public static void transmission(SceneBuilder builder, SceneBuildingUtil util) {
        CreateSceneBuilder scene = new CreateSceneBuilder(builder);
        scene.title("transmission", "Driving with the Transmission");
        scene.configureBasePlate(0, 0, 5);
        scene.showBasePlate();
        scene.idle(5);

        BlockPos box = util.grid().at(2, 1, 2);
        BlockPos outEnd = util.grid().at(4, 1, 2);
        Selection input = util.select().fromTo(0, 1, 2, 1, 1, 2);
        Selection boxSel = util.select().position(box);
        Selection output = util.select().fromTo(3, 1, 2, 4, 1, 2);
        Selection upLever = util.select().position(2, 2, 2);
        Selection forwardLever = util.select().position(2, 1, 3);
        Selection reverseLever = util.select().position(2, 1, 1);

        scene.world().setKineticSpeed(input, INPUT);
        scene.world().setKineticSpeed(boxSel, INPUT);
        scene.world().setKineticSpeed(output, 0);
        // Ponder never propagates rotation, so tell the box where its power comes from: the gear
        // train puts the input pinion at the source end and the output shaft only turns in gear.
        scene.world().modifyBlockEntityNBT(boxSel, TransmissionBlockEntity.class,
                nbt -> nbt.put("Source", NbtUtils.writeBlockPos(box.west())));
        scene.world().showSection(input, Direction.EAST);
        scene.idle(10);
        scene.world().showSection(boxSel, Direction.DOWN);
        scene.idle(10);
        scene.world().showSection(output, Direction.WEST);
        scene.idle(10);

        scene.overlay().showText(80).attachKeyFrame().placeNearTarget().pointAt(util.vector().topOf(box))
                .text("Each of the Transmission's four gears is a set output speed, whatever the input");
        scene.idle(90);
        scene.overlay().showText(60).placeNearTarget().pointAt(util.vector().centerOf(outEnd))
                .text("With no signal it is in Neutral: the output is disconnected");
        scene.idle(70);

        scene.world().showSection(forwardLever, Direction.NORTH);
        scene.idle(10);
        scene.world().toggleRedstonePower(forwardLever);
        setState(scene, box, output, outEnd, 0, Drive.FORWARD);
        scene.overlay().showText(70).attachKeyFrame().placeNearTarget().pointAt(util.vector().blockSurface(box, Direction.SOUTH))
                .text("While the Forward face is powered, the output turns the input's way");
        scene.idle(80);

        scene.world().showSection(upLever, Direction.DOWN);
        scene.idle(10);
        scene.overlay().showText(80).attachKeyFrame().placeNearTarget().pointAt(util.vector().topOf(box.above()))
                .text("A pulse on the Up or Down face shifts one gear. Holding it keeps shifting");
        scene.idle(30);
        pulse(scene, upLever, box, output, outEnd, 1);
        pulse(scene, upLever, box, output, outEnd, 2);
        scene.idle(30);

        scene.world().toggleRedstonePower(forwardLever);
        scene.world().showSection(reverseLever, Direction.SOUTH);
        scene.idle(10);
        scene.world().toggleRedstonePower(reverseLever);
        setState(scene, box, output, outEnd, 2, Drive.REVERSE);
        scene.overlay().showText(70).attachKeyFrame().placeNearTarget().pointAt(util.vector().blockSurface(box, Direction.NORTH))
                .text("While the Reverse face is powered, it turns the other way");
        scene.idle(80);
        scene.world().toggleRedstonePower(reverseLever);
        setState(scene, box, output, outEnd, 2, Drive.NEUTRAL);
        scene.idle(20);

        scene.overlay().showText(70).attachKeyFrame().placeNearTarget().pointAt(util.vector().blockSurface(box, Direction.NORTH))
                .text("Every long face also has built-in Redstone Link slots");
        scene.idle(80);
        scene.overlay().showText(70).placeNearTarget().pointAt(util.vector().topOf(box))
                .text("Shift-right-click it with an empty hand to set the four gear speeds");
        scene.idle(80);
        scene.overlay().showText(80).colored(PonderPalette.BLUE).placeNearTarget().pointAt(util.vector().topOf(box))
                .text("With CC: Tweaked installed, a computer can drive it as a 'transmission' peripheral");
        scene.idle(90);
        scene.overlay().showText(80).placeNearTarget().pointAt(util.vector().blockSurface(box, Direction.EAST))
                .text("Use a Wrench on a shaft end to turn the faces around the shaft");
        scene.idle(90);
    }

    /** One lever pulse on the Up face: on, shift, off. */
    private static void pulse(CreateSceneBuilder scene, Selection lever, BlockPos box, Selection output, BlockPos outEnd, int gear) {
        scene.world().toggleRedstonePower(lever);
        setState(scene, box, output, outEnd, gear, Drive.FORWARD);
        scene.idle(15);
        scene.world().toggleRedstonePower(lever);
        scene.idle(15);
    }

    /** The output follows the input's direction forward and opposes it in reverse. */
    private static void setState(CreateSceneBuilder scene, BlockPos box, Selection output, BlockPos outEnd, int gear, Drive drive) {
        scene.world().modifyBlock(box, state -> state.setValue(TransmissionBlock.GEAR, gear).setValue(TransmissionBlock.DRIVE, drive), false);
        scene.world().setKineticSpeed(output, Math.signum(INPUT) * drive.sign() * DEFAULTS.get(gear));
        if (drive != Drive.NEUTRAL)
            scene.effects().rotationDirectionIndicator(outEnd);
        scene.idle(10);
    }
}
