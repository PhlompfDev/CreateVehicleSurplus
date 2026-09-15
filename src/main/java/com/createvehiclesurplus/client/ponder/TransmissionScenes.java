package com.createvehiclesurplus.client.ponder;

import com.createvehiclesurplus.content.transmission.Drive;
import com.createvehiclesurplus.content.transmission.GearSpeeds;
import com.createvehiclesurplus.content.transmission.TransmissionBlock;
import com.createvehiclesurplus.content.transmission.TransmissionBlockEntity;
import com.simibubi.create.content.redstone.analogLever.AnalogLeverBlockEntity;
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
 * shift is acted out: set the gear property, set the output speed, point at the output.
 */
public class TransmissionScenes {
    private static final float SPEED = 32;

    public static void transmission(SceneBuilder builder, SceneBuildingUtil util) {
        CreateSceneBuilder scene = new CreateSceneBuilder(builder);
        scene.title("transmission", "Shifting gears with the Transmission");
        scene.configureBasePlate(0, 0, 5);
        scene.showBasePlate();
        scene.idle(5);

        BlockPos box = util.grid().at(2, 1, 2);
        BlockPos outEnd = util.grid().at(4, 1, 2);
        Selection input = util.select().fromTo(0, 1, 2, 1, 1, 2);
        Selection boxSel = util.select().position(box);
        Selection output = util.select().fromTo(3, 1, 2, 4, 1, 2);
        Selection upLever = util.select().position(2, 2, 2);
        Selection analogLever = util.select().position(2, 1, 3);
        Selection neutralLever = util.select().position(2, 1, 1);

        scene.world().setKineticSpeed(input, SPEED);
        scene.world().setKineticSpeed(boxSel, SPEED);
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

        scene.overlay().showText(70).attachKeyFrame().placeNearTarget().pointAt(util.vector().topOf(box))
                .text("The Transmission sits inline on a shaft and changes the speed passed through it");
        scene.idle(80);
        scene.overlay().showText(60).placeNearTarget().pointAt(util.vector().centerOf(outEnd))
                .text("It starts in Neutral: the output is disconnected");
        scene.idle(70);
        scene.overlay().showText(80).attachKeyFrame().placeNearTarget().pointAt(util.vector().blockSurface(box, Direction.NORTH))
                .text("Each long face takes a frequency like a Redstone Link: right-click its two slots with items");
        scene.idle(90);

        scene.world().showSection(upLever, Direction.DOWN);
        scene.idle(10);
        scene.overlay().showText(60).attachKeyFrame().placeNearTarget().pointAt(util.vector().topOf(box.above()))
                .text("A pulse on the Up face shifts one gear up");
        scene.idle(30);
        shift(scene, upLever, box, output, outEnd, 0);
        shift(scene, upLever, box, output, outEnd, 1);
        scene.overlay().showText(90).placeNearTarget().pointAt(util.vector().centerOf(outEnd))
                .text("The Down face works the same way. Every gear is a reduction: 1/4, 1/2, 3/4 and 1:1, plus Reverse");
        scene.idle(100);

        scene.world().showSection(analogLever, Direction.NORTH);
        scene.idle(10);
        scene.overlay().showText(80).attachKeyFrame().placeNearTarget().pointAt(util.vector().blockSurface(box, Direction.SOUTH))
                .text("The Analog face picks a gear from signal strength and overrides the pulses");
        scene.idle(40);
        scene.world().modifyBlockEntityNBT(analogLever, AnalogLeverBlockEntity.class, nbt -> nbt.putInt("State", 15));
        setState(scene, box, output, outEnd, 3, Drive.FORWARD);
        scene.idle(50);

        scene.world().showSection(neutralLever, Direction.SOUTH);
        scene.idle(10);
        scene.world().toggleRedstonePower(neutralLever);
        setState(scene, box, output, outEnd, 0, Drive.NEUTRAL);
        scene.overlay().showText(80).attachKeyFrame().colored(PonderPalette.RED).placeNearTarget()
                .pointAt(util.vector().blockSurface(box, Direction.NORTH))
                .text("Holding the Neutral face locks the output in Neutral");
        scene.idle(90);
        scene.overlay().showText(80).placeNearTarget().pointAt(util.vector().topOf(box))
                .text("Lower gears turn the output slower and put less load on the engine");
        scene.idle(90);
        scene.overlay().showText(80).colored(PonderPalette.BLUE).placeNearTarget().pointAt(util.vector().topOf(box))
                .text("With CC: Tweaked installed, a computer can drive it as a 'transmission' peripheral");
        scene.idle(90);
        scene.overlay().showText(80).placeNearTarget().pointAt(util.vector().blockSurface(box, Direction.EAST))
                .text("Use a Wrench on a shaft end to turn the faces around the shaft");
        scene.idle(90);
    }

    private static final GearSpeeds DEFAULTS = new GearSpeeds();

    /** One lever pulse on the Up face: on, shift, off. */
    private static void shift(CreateSceneBuilder scene, Selection lever, BlockPos box, Selection output, BlockPos outEnd, int gear) {
        scene.world().toggleRedstonePower(lever);
        setState(scene, box, output, outEnd, gear, Drive.FORWARD);
        scene.idle(15);
        scene.world().toggleRedstonePower(lever);
        scene.idle(15);
    }

    private static void setState(CreateSceneBuilder scene, BlockPos box, Selection output, BlockPos outEnd, int gear, Drive drive) {
        scene.world().modifyBlock(box, state -> state.setValue(TransmissionBlock.GEAR, gear).setValue(TransmissionBlock.DRIVE, drive), false);
        scene.world().setKineticSpeed(output, drive.sign() * DEFAULTS.get(gear));
        if (drive != Drive.NEUTRAL)
            scene.effects().rotationDirectionIndicator(outEnd);
        scene.idle(10);
    }
}
