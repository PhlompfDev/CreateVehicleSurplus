package com.createvehiclesurplus.client;

import com.createvehiclesurplus.CreateVehicleSurplus;
import com.createvehiclesurplus.VehicleSurplusBlocks;
import com.createvehiclesurplus.content.transmission.ConfigureTransmissionPayload;
import com.createvehiclesurplus.content.transmission.GearSpeeds;
import com.createvehiclesurplus.content.transmission.TransmissionBlockEntity;
import com.simibubi.create.foundation.gui.AllGuiTextures;
import com.simibubi.create.foundation.gui.AllIcons;
import com.simibubi.create.foundation.gui.widget.IconButton;
import com.simibubi.create.foundation.gui.widget.ScrollInput;
import net.createmod.catnip.gui.AbstractSimiScreen;
import net.createmod.catnip.gui.element.GuiGameElement;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.Arrays;

/**
 * The four gear speeds, on Create's Sequenced Gearshift panel: one row per gear with a Create
 * {@link ScrollInput} (Shift scrolls faster, as in Create's own screens). Sent when it closes.
 */
public class TransmissionScreen extends AbstractSimiScreen {
    /** Shift-scroll step; plain scrolling moves by 1 RPM. */
    private static final int SHIFT_STEP = 10;
    /** The panel has five row slots; the fifth stays empty. */
    private static final int ROWS = 5;

    private final AllGuiTextures background = AllGuiTextures.SEQUENCER;
    private final ItemStack renderedItem = VehicleSurplusBlocks.TRANSMISSION.asStack();
    private final TransmissionBlockEntity be;
    private final int[] speeds;

    public TransmissionScreen(TransmissionBlockEntity be) {
        super(Component.translatable(CreateVehicleSurplus.ID + ".gui.transmission.title"));
        this.be = be;
        this.speeds = be.speeds().toArray();
    }

    public static void open(TransmissionBlockEntity be) {
        Minecraft.getInstance().setScreen(new TransmissionScreen(be));
    }

    @Override
    protected void init() {
        setWindowSize(background.getWidth(), background.getHeight());
        setWindowOffset(-20, 0);
        super.init();
        int x = guiLeft + 30;
        int y = guiTop + 20;
        int rowHeight = AllGuiTextures.SEQUENCER_EMPTY.getHeight();
        for (int gear = 0; gear < GearSpeeds.GEARS; gear++) {
            int index = gear;
            addRenderableWidget(new ScrollInput(x + 58, y + rowHeight * gear, 28, 18)
                    .withRange(1, GearSpeeds.CEILING + 1)
                    .withShiftStep(SHIFT_STEP)
                    .titled(Component.translatable(CreateVehicleSurplus.ID + ".gui.transmission.gear", gear + 1))
                    .calling(state -> speeds[index] = state)
                    .setState(speeds[gear]));
        }
        IconButton confirm = new IconButton(guiLeft + background.getWidth() - 33, guiTop + background.getHeight() - 24, AllIcons.I_CONFIRM);
        confirm.withCallback(this::onClose);
        addRenderableWidget(confirm);
    }

    @Override
    protected void renderWindow(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
        int x = guiLeft;
        int y = guiTop;
        background.render(graphics, x, y);
        for (int row = 0; row < ROWS; row++) {
            AllGuiTextures rowTexture = row < GearSpeeds.GEARS ? AllGuiTextures.SEQUENCER_DELAY : AllGuiTextures.SEQUENCER_EMPTY;
            rowTexture.render(graphics, x, y + 16 + rowTexture.getHeight() * row);
        }
        for (int gear = 0; gear < GearSpeeds.GEARS; gear++) {
            int yOffset = AllGuiTextures.SEQUENCER_EMPTY.getHeight() * gear;
            label(graphics, 36, yOffset - 1, Component.translatable(CreateVehicleSurplus.ID + ".gui.transmission.gear", gear + 1));
            String text = String.valueOf(speeds[gear]);
            label(graphics, 90 + (12 - font.width(text) / 2), yOffset - 1, Component.literal(text));
        }
        graphics.drawString(font, title, x + (background.getWidth() - 8) / 2 - font.width(title) / 2, y + 4, 0x592424, false);
        GuiGameElement.of(renderedItem).<GuiGameElement.GuiRenderBuilder>at(x + background.getWidth() + 6, y + background.getHeight() - 56, 100)
                .scale(5)
                .render(graphics);
    }

    private void label(GuiGraphics graphics, int x, int y, Component text) {
        graphics.drawString(font, text, guiLeft + x, guiTop + 26 + y, 0xFFFFEE);
    }

    @Override
    public void removed() {
        PacketDistributor.sendToServer(new ConfigureTransmissionPayload(be.getBlockPos(), Arrays.stream(speeds).boxed().toList()));
    }
}
