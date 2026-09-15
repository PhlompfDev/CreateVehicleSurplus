package com.createvehiclesurplus.client;

import com.createvehiclesurplus.content.link.SidedLinkBehaviour;
import com.createvehiclesurplus.content.transmission.Role;
import com.createvehiclesurplus.content.transmission.TransmissionBlock;
import com.createvehiclesurplus.content.transmission.TransmissionBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.simibubi.create.AllPartialModels;
import com.simibubi.create.content.kinetics.base.KineticBlockEntityRenderer;
import com.simibubi.create.content.kinetics.transmission.SplitShaftBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.ValueBoxRenderer;
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
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import org.joml.Vector3f;

/**
 * Draws the frequency items in the sockets, and, when Flywheel is not drawing this level (backend
 * off, Ponder scenes), the same moving parts as {@link TransmissionVisual}: half shafts, layshaft
 * cluster and sliders, with Create's kinetic rotation transform. Everything goes into one solid
 * buffer: every texel is opaque, and Ponder's buffer source shares a single builder between render
 * types, so holding two consumers at once throws "Not building!".
 */
public class TransmissionRenderer extends KineticBlockEntityRenderer<SplitShaftBlockEntity> {

    public TransmissionRenderer(BlockEntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    protected void renderSafe(SplitShaftBlockEntity be, float partialTicks, PoseStack ms, MultiBufferSource buffer, int light, int overlay) {
        if (!(be instanceof TransmissionBlockEntity transmission) || !(be.getBlockState().getBlock() instanceof TransmissionBlock))
            return;
        renderFrequencies(transmission, ms, buffer, light, overlay);
        if (VisualizationManager.supportsVisualization(be.getLevel()))
            return;
        renderGearTrain(transmission, partialTicks, ms, buffer, light);
    }

    private static void renderFrequencies(TransmissionBlockEntity be, PoseStack ms, MultiBufferSource buffer, int light, int overlay) {
        BlockState state = be.getBlockState();
        for (Role role : Role.VALUES) {
            SidedLinkBehaviour link = be.link(role);
            if (link == null)
                continue;
            for (boolean first : Iterate.trueAndFalse) {
                ItemStack stack = link.getFrequency(first).getStack();
                if (stack.isEmpty())
                    continue;
                ms.pushPose();
                link.getSlot(first).transform(be.getLevel(), be.getBlockPos(), state, ms);
                ValueBoxRenderer.renderItemIntoValueBox(stack, ms, buffer, light, overlay);
                ms.popPose();
            }
        }
    }

    private static void renderGearTrain(TransmissionBlockEntity be, float partialTicks, PoseStack ms, MultiBufferSource buffer, int light) {
        BlockState state = be.getBlockState();
        Axis axis = state.getValue(TransmissionBlock.AXIS);
        Direction positive = Direction.get(Direction.AxisDirection.POSITIVE, axis);
        Direction input = TransmissionParts.inputEnd(be);
        float in = be.getSpeed();
        float out = be.outputSpeed();
        float time = AnimationTickHolder.getRenderTime(be.getLevel());
        float base = getRotationOffsetForPosition(be, be.getBlockPos(), axis);
        VertexConsumer solid = buffer.getBuffer(RenderType.solid());

        for (Direction end : Iterate.directionsInAxis(axis)) {
            float angle = radians(time * (end == input ? in : out) * 3f / 10 + base);
            kineticRotationTransform(CachedBuffers.partialFacing(AllPartialModels.SHAFT_HALF, state, end), be, axis, angle, light)
                    .renderInto(ms, solid);
        }
        float layAngle = radians(time * -in * 3f / 10 + base);
        draw(TransmissionParts.LAY_ROD, state, positive, TransmissionParts.onLayshaft(axis, 8), be, axis, layAngle, light, ms, solid);
        int live = TransmissionParts.slotOf(be);
        for (int slot = 0; slot < TransmissionParts.SLOTS; slot++) {
            draw(TransmissionParts.clusterGear(slot), state, positive, TransmissionParts.onLayshaft(axis, TransmissionParts.slotA(slot)), be, axis,
                    radians(time * -in * 3f / 10 + base + TransmissionParts.MESH_OFFSET), light, ms, solid);
            float engagement = be.engagement(slot, partialTicks);
            float speed = slot == live ? out : 0;
            draw(TransmissionParts.SLIDER[TransmissionParts.sizeOf(slot)], state, positive,
                    TransmissionParts.onShaft(axis, TransmissionParts.sliderA(slot, engagement)), be, axis,
                    radians(time * speed * 3f / 10 + base), light, ms, solid);
        }
    }

    private static void draw(PartialModel partial, BlockState state, Direction facing, Vector3f offset, TransmissionBlockEntity be,
                             Axis axis, float angle, int light, PoseStack ms, VertexConsumer consumer) {
        SuperByteBuffer buffer = CachedBuffers.partialFacing(partial, state, facing);
        buffer.translate(offset.x, offset.y, offset.z);
        kineticRotationTransform(buffer, be, axis, angle, light).renderInto(ms, consumer);
    }

    private static float radians(float degrees) {
        return (degrees % 360) / 180f * (float) Math.PI;
    }
}
