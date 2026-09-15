package com.createvehiclesurplus.client;

import com.createvehiclesurplus.content.gimbal.GimbalControllerBlock;
import com.createvehiclesurplus.content.gimbal.GimbalControllerBlockEntity;
import com.createvehiclesurplus.content.gimbal.GimbalPose;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.simibubi.create.content.kinetics.base.HorizontalAxisKineticBlock;
import com.simibubi.create.content.kinetics.base.KineticBlockEntityRenderer;
import dev.engine_room.flywheel.api.visualization.VisualizationManager;
import dev.engine_room.flywheel.lib.model.baked.PartialModel;
import net.createmod.catnip.animation.AnimationTickHolder;
import net.createmod.catnip.data.Iterate;
import net.createmod.catnip.render.CachedBuffers;
import net.createmod.catnip.render.SuperByteBuffer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Draws the same moving parts as {@link GimbalVisual} when Flywheel is not drawing this level
 * (backend off, Ponder scenes): shaft stubs with Create's kinetic rotation, then the frames and
 * rotor posed by {@link GimbalPose}. Everything goes into one solid buffer: every texel is opaque,
 * and Ponder's buffer source shares a single builder between render types.
 */
public class GimbalRenderer extends KineticBlockEntityRenderer<GimbalControllerBlockEntity> {

    public GimbalRenderer(BlockEntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    protected void renderSafe(GimbalControllerBlockEntity be, float partialTicks, PoseStack ms, MultiBufferSource buffer, int light, int overlay) {
        if (VisualizationManager.supportsVisualization(be.getLevel()))
            return;
        BlockState state = be.getBlockState();
        if (!(state.getBlock() instanceof GimbalControllerBlock))
            return;
        Axis axis = state.getValue(HorizontalAxisKineticBlock.HORIZONTAL_AXIS);
        VertexConsumer solid = buffer.getBuffer(RenderType.solid());

        float angle = getAngleForBe(be, be.getBlockPos(), axis);
        for (Direction end : Iterate.directionsInAxis(axis))
            kineticRotationTransform(CachedBuffers.partialFacing(GimbalParts.SHAFT_END, state, end), be, axis, angle, light).renderInto(ms, solid);

        GimbalPose pose = GimbalPose.of(be, partialTicks, AnimationTickHolder.getRenderTime(be.getLevel()));
        float yaw = GimbalParts.yawDegrees(axis);
        placed(GimbalParts.OUTER_FRAME, state, yaw).rotateXCenteredDegrees(pose.outerDegrees()).light(light).renderInto(ms, solid);
        placed(GimbalParts.INNER_FRAME, state, yaw).rotateXCenteredDegrees(pose.outerDegrees()).rotateZCenteredDegrees(pose.innerDegrees())
                .light(light).renderInto(ms, solid);
        placed(GimbalParts.ROTOR, state, yaw).rotateXCenteredDegrees(pose.outerDegrees()).rotateZCenteredDegrees(pose.innerDegrees())
                .rotateYCenteredDegrees(pose.rotorDegrees()).light(light).renderInto(ms, solid);
    }

    private static SuperByteBuffer placed(PartialModel partial, BlockState state, float yaw) {
        return CachedBuffers.partial(partial, state).rotateYCenteredDegrees(yaw);
    }
}
