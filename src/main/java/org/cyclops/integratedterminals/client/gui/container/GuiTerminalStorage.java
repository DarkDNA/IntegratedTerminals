package org.cyclops.integratedterminals.client.gui.container;

import com.google.common.collect.Lists;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.renderer.RenderItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import org.cyclops.cyclopscore.client.gui.component.GuiScrollBar;
import org.cyclops.cyclopscore.client.gui.component.input.GuiArrowedListField;
import org.cyclops.cyclopscore.client.gui.container.GuiContainerExtended;
import org.cyclops.cyclopscore.helper.GuiHelpers;
import org.cyclops.cyclopscore.helper.L10NHelpers;
import org.cyclops.cyclopscore.helper.RenderHelpers;
import org.cyclops.cyclopscore.init.ModBase;
import org.cyclops.integrateddynamics.api.part.IPartContainer;
import org.cyclops.integrateddynamics.api.part.IPartType;
import org.cyclops.integrateddynamics.api.part.PartTarget;
import org.cyclops.integratedterminals.IntegratedTerminals;
import org.cyclops.integratedterminals.Reference;
import org.cyclops.integratedterminals.api.terminalstorage.ITerminalStorageSlot;
import org.cyclops.integratedterminals.api.terminalstorage.ITerminalStorageTabClient;
import org.cyclops.integratedterminals.inventory.container.ContainerTerminalStorage;
import org.lwjgl.opengl.GL11;

import java.io.IOException;
import java.util.List;

/**
 * @author rubensworks
 */
public class GuiTerminalStorage extends GuiContainerExtended {

    private static int TAB_OFFSET_X = 2;
    private static int TAB_WIDTH = 24;
    private static int TAB_UNSELECTED_HEIGHT = 21;
    private static int TAB_SELECTED_HEIGHT = 24;
    private static int TAB_ICON_OFFSET = 4;
    private static int TAB_UNSELECTED_TEXTURE_X = 0;
    private static int TAB_SELECTED_TEXTURE_X = 24;
    private static int TAB_UNSELECTED_TEXTURE_Y = 225;
    private static int TAB_SELECTED_TEXTURE_Y = 225;
    private static int SCROLL_X = 176;
    private static int SCROLL_Y = 39;
    private static int SCROLL_HEIGHT = 88;

    private static int SLOTS_OFFSET_X = 9;
    private static int SLOTS_OFFSET_Y = 39;

    private GuiArrowedListField<String> fieldChannel;
    private GuiScrollBar scrollBar;
    private int firstRow;

    public GuiTerminalStorage(EntityPlayer player, PartTarget target, IPartContainer partContainer, IPartType partType) {
        super(new ContainerTerminalStorage(player, target, partContainer, partType));
    }

    @Override
    public void initGui() {
        super.initGui();

        fieldChannel = new GuiArrowedListField<>(0, Minecraft.getMinecraft().fontRenderer, guiLeft + 36, guiTop + 25,
                42, 15, true, true, getContainer().getChannelStrings());
        fieldChannel.setMaxStringLength(15);
        fieldChannel.setVisible(true);
        fieldChannel.setTextColor(16777215);
        fieldChannel.setCanLoseFocus(true);
        fieldChannel.setEnabled(true);

        scrollBar = new GuiScrollBar(guiLeft + SCROLL_X, guiTop + SCROLL_Y, SCROLL_HEIGHT,
                firstRow -> this.firstRow = firstRow, getSlotVisibleRows()) {
            @Override
            public int getTotalRows() {
                ContainerTerminalStorage container = getContainer();
                int totalSlots = getSelectedClientTab().getSlotCount(container.getSelectedChannel());
                return totalSlots / getSlotRowLength();
            }
        };
    }

    @Override
    protected ResourceLocation constructResourceLocation() {
        return new ResourceLocation(Reference.MOD_ID, this.getGuiTexture());
    }

    @Override
    public String getGuiTexture() {
        return IntegratedTerminals._instance.getReferenceValue(ModBase.REFKEY_TEXTURE_PATH_GUI)
                + "part_terminal_storage.png";
    }

    @Override
    public int getBaseXSize() {
        return 196;
    }

    @Override
    public int getBaseYSize() {
        return 225;
    }

    @Override
    protected void drawGuiContainerBackgroundLayer(float f, int mouseX, int mouseY) {
        super.drawGuiContainerBackgroundLayer(f, mouseX, mouseY);
        fieldChannel.drawTextBox(Minecraft.getMinecraft(), mouseX, mouseY);
        drawTabsBackground();
        drawTabContents(getContainer().getSelectedTabIndex(), getContainer().getSelectedChannel(), DrawLayer.BACKGROUND,
                f, getGuiLeftTotal() + SLOTS_OFFSET_X, getGuiTopTotal() + SLOTS_OFFSET_Y, mouseX, mouseY);
        scrollBar.drawGuiContainerBackgroundLayer(f, mouseX, mouseY);
    }

    @Override
    protected void drawGuiContainerForegroundLayer(int mouseX, int mouseY) {
        super.drawGuiContainerForegroundLayer(mouseX, mouseY);
        drawTabsForeground(mouseX, mouseY);
        drawTabContents(getContainer().getSelectedTabIndex(), getContainer().getSelectedChannel(), DrawLayer.FOREGROUND,
                0, SLOTS_OFFSET_X, SLOTS_OFFSET_Y, mouseX, mouseY);
        drawActiveStorageSlotItem(mouseX, mouseY);
    }

    @Override
    protected void drawCurrentScreen(int mouseX, int mouseY, float partialTicks) {
        super.drawCurrentScreen(mouseX, mouseY, partialTicks);
        scrollBar.drawCurrentScreen(mouseX, mouseY, partialTicks);
    }

    @Override
    protected ContainerTerminalStorage getContainer() {
        return (ContainerTerminalStorage) super.getContainer();
    }

    @Override
    public void handleMouseInput() throws IOException {
        super.handleMouseInput();
        scrollBar.handleMouseInput();
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
        // Select a tab
        if (mouseButton == 0
                && mouseY < getGuiTop() + TAB_UNSELECTED_HEIGHT
                && mouseX > getGuiLeft() + TAB_OFFSET_X && mouseX <= getGuiLeft() + (TAB_WIDTH * getContainer().getTabsClientCount())) {
            // Save tab index
            getContainer().setSelectedTabIndex((mouseX - TAB_OFFSET_X - getGuiLeft()) / TAB_WIDTH);

            // Reset scrollbar
            scrollBar.scrollTo(0);

            // Reset active slot
            getSelectedClientTab().resetActiveSlot();

            return;
        }

        // Update channel when changing channel field
        if (this.fieldChannel.mouseClicked(mouseX, mouseY, mouseButton)) {
            int channel;
            try {
                channel = Integer.parseInt(this.fieldChannel.getActiveElement());
            } catch (NumberFormatException e) {
                channel = -1;
            }
            getContainer().setSelectedChannel(channel);
            scrollBar.scrollTo(0); // Reset scrollbar
            return;
        }

        // Handle clicks on storage slots
        int slot = getStorageSlotIndexAtPosition(mouseX, mouseY);
        Slot playerSlot = getSlotUnderMouse();
        boolean hasClickedOutside = this.hasClickedOutside(mouseX, mouseY, this.guiLeft, this.guiTop);
        if (getSelectedClientTab().handleClick(getContainer().getSelectedChannel(), slot, mouseButton,
                hasClickedOutside, playerSlot != null ? playerSlot.getSlotIndex() : -1)) {
            return;
        }

        super.mouseClicked(mouseX, mouseY, mouseButton);
    }

    private int getStorageSlotIndexAtPosition(int mouseX, int mouseY) {
        if (mouseX >= getGuiLeftTotal() + SLOTS_OFFSET_X
                && mouseX < getGuiLeftTotal() + SLOTS_OFFSET_X + getSlotRowLength() * GuiHelpers.SLOT_SIZE - 1
                && mouseY >= getGuiTopTotal() + SLOTS_OFFSET_Y
                && mouseY < getGuiTopTotal() + SLOTS_OFFSET_Y + getSlotVisibleRows() * GuiHelpers.SLOT_SIZE) {
            if ((mouseX - getGuiLeftTotal() - SLOTS_OFFSET_X) % GuiHelpers.SLOT_SIZE < GuiHelpers.SLOT_SIZE_INNER
                    && (mouseY - getGuiTopTotal() - SLOTS_OFFSET_Y) % GuiHelpers.SLOT_SIZE < GuiHelpers.SLOT_SIZE_INNER) {
                return ((mouseX - getGuiLeftTotal() - SLOTS_OFFSET_X) / GuiHelpers.SLOT_SIZE)
                        + ((mouseY - getGuiTopTotal() - SLOTS_OFFSET_Y) / GuiHelpers.SLOT_SIZE) * getSlotRowLength();
            }
        }

        return -1;
    }

    protected void drawTabsBackground() {
        int offsetX = TAB_OFFSET_X;
        int i = 0;

        // Draw channels label
        drawString(fontRenderer, L10NHelpers.localize("gui.integratedterminals.terminal_storage.channel"), getGuiLeft() + 8, getGuiTop() + 26, 16777215);

        // Draw all tabs next to each other horizontally
        for (ITerminalStorageTabClient tab : getContainer().getTabsClient()) {
            boolean selected = i == getContainer().getSelectedTabIndex();
            int x = getGuiLeft() + offsetX;
            int y = getGuiTop();
            int width = TAB_WIDTH;
            int height = selected ? TAB_SELECTED_HEIGHT : TAB_UNSELECTED_HEIGHT;
            int textureX = selected ? TAB_SELECTED_TEXTURE_X : TAB_UNSELECTED_TEXTURE_X;
            int textureY = selected ? TAB_SELECTED_TEXTURE_Y : TAB_UNSELECTED_TEXTURE_Y;

            // Draw background
            this.mc.renderEngine.bindTexture(this.texture);
            this.drawTexturedModalRect(x, y, textureX, textureY, width, height);

            // Draw icon
            ItemStack icon = tab.getIcon();
            RenderItem renderItem = Minecraft.getMinecraft().getRenderItem();
            GlStateManager.pushMatrix();
            GlStateManager.enableBlend();
            GlStateManager.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
            RenderHelper.enableGUIStandardItemLighting();
            GlStateManager.enableRescaleNormal();
            GL11.glEnable(GL11.GL_DEPTH_TEST);
            renderItem.renderItemAndEffectIntoGUI(icon, x + TAB_ICON_OFFSET, y + TAB_ICON_OFFSET);
            RenderHelper.disableStandardItemLighting();
            GlStateManager.popMatrix();
            GL11.glDisable(GL11.GL_DEPTH_TEST);

            offsetX += width;
            i++;
        }
    }

    protected int getSlotVisibleRows() {
        return 5;
    }

    protected int getSlotRowLength() {
        return 9;
    }

    protected int getSelectedFirstRow() {
        return firstRow;
    }

    protected void drawTabContents(int tabIndex, int channel, DrawLayer layer,
                                   float partialTick, int x, int y, int mouseX, int mouseY) {
        ITerminalStorageTabClient tab = getClientTab(tabIndex);

        // Draw status string
        drawCenteredString(fontRenderer, tab.getStatus(channel), x + 80, y + 92, 16777215);
        GlStateManager.color(1, 1, 1);

        // Draw slots
        int rowLength = getSlotRowLength();
        int limit = getSlotVisibleRows() * rowLength;
        int offset = getSelectedFirstRow() * rowLength;
        List<ITerminalStorageSlot> slots = tab.getSlots(channel, offset, limit);
        int slotX = x;
        int slotY = y;
        int slotI = 0;
        for (ITerminalStorageSlot slot : slots) {
            if (layer == DrawLayer.BACKGROUND) {
                // highlight slot on hover
                RenderHelpers.bindTexture(this.texture);
                if (RenderHelpers.isPointInRegion(slotX, slotY, GuiHelpers.SLOT_SIZE_INNER, GuiHelpers.SLOT_SIZE_INNER, mouseX, mouseY)) {
                    drawRect(slotX, slotY, slotX + GuiHelpers.SLOT_SIZE_INNER, slotY + GuiHelpers.SLOT_SIZE_INNER, -2130706433);
                }
            }
            slot.drawGuiContainerLayer(this, layer, partialTick, slotX, slotY, mouseX, mouseY, tab, channel, null);
            if (++slotI >= rowLength) {
                slotX = x;
                slotY += GuiHelpers.SLOT_SIZE;
                slotI = 0;
            } else {
                slotX += GuiHelpers.SLOT_SIZE;
            }
        }
    }

    private void drawActiveStorageSlotItem(int mouseX, int mouseY) {
        ITerminalStorageTabClient<?> tab = getSelectedClientTab();
        int slotId = tab.getActiveSlotId();
        if (slotId >= 0) {
            int maxQuantity = tab.getActiveSlotQuantity();
            ITerminalStorageSlot slot = tab.getSlots(getContainer().getSelectedChannel(), slotId, 1).get(0);
            RenderHelpers.bindTexture(this.texture);
            GlStateManager.color(1, 1, 1, 1);
            slot.drawGuiContainerLayer(this, DrawLayer.BACKGROUND, 0,
                    mouseX - this.guiLeft - GuiHelpers.SLOT_SIZE_INNER / 4, mouseY - this.guiTop - GuiHelpers.SLOT_SIZE_INNER / 4,
                    mouseX, mouseY, tab, getContainer().getSelectedChannel(), GuiHelpers.quantityToScaledString(maxQuantity));
        }
    }

    protected ITerminalStorageTabClient<?> getClientTab(int tabIndex) {
        return Lists.newArrayList(getContainer().getTabsClient()).get(tabIndex);
    }

    protected ITerminalStorageTabClient<?> getSelectedClientTab() {
        return getClientTab(getContainer().getSelectedTabIndex());
    }

    protected void drawTabsForeground(int mouseX, int mouseY) {
        if (mouseY < getGuiTop() + TAB_UNSELECTED_HEIGHT
                && mouseX > getGuiLeft() + TAB_OFFSET_X && mouseX <= getGuiLeft() + (TAB_WIDTH * getContainer().getTabsClientCount())) {
            int tabIndex = (mouseX - TAB_OFFSET_X - getGuiLeft()) / TAB_WIDTH;
            ITerminalStorageTabClient tab = getClientTab(tabIndex);
            this.drawTooltip(tab.getTooltip(), mouseX - getGuiLeft(), mouseY - getGuiTop());
        }
    }

    /**
     * The layer to draw on.
     */
    public static enum DrawLayer {
        BACKGROUND,
        FOREGROUND
    }
}
