package org.cyclops.integratedterminals.inventory.container;

import net.minecraft.client.gui.IHasContainer;
import net.minecraft.client.gui.ScreenManager;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.util.text.ITextComponent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.cyclops.cyclopscore.client.gui.ScreenFactorySafe;
import org.cyclops.cyclopscore.config.extendedconfig.GuiConfig;
import org.cyclops.cyclopscore.inventory.container.ContainerTypeData;
import org.cyclops.integrateddynamics.api.part.PartPos;
import org.cyclops.integratedterminals.IntegratedTerminals;
import org.cyclops.integratedterminals.client.gui.container.ContainerScreenTerminalStorage;
import org.cyclops.integratedterminals.client.gui.container.ContainerScreenTerminalStorageCraftingOptionAmount;

/**
 * Config for {@link ContainerTerminalStorageCraftingOptionAmountPart}.
 * @author rubensworks
 */
public class ContainerTerminalStorageCraftingOptionAmountPartConfig extends GuiConfig<ContainerTerminalStorageCraftingOptionAmountPart> {

    public ContainerTerminalStorageCraftingOptionAmountPartConfig() {
        super(IntegratedTerminals._instance,
                "part_terminal_storage_crafting_option_amount_part",
                eConfig -> new ContainerTypeData<>(ContainerTerminalStorageCraftingOptionAmountPart::new));
    }

    @OnlyIn(Dist.CLIENT)
    @Override
    public <U extends Screen & IHasContainer<ContainerTerminalStorageCraftingOptionAmountPart>> ScreenManager.IScreenFactory<ContainerTerminalStorageCraftingOptionAmountPart, U> getScreenFactory() {
        // Does not compile when simplified with lambdas
        return new ScreenFactorySafe<>(new ScreenManager.IScreenFactory<ContainerTerminalStorageCraftingOptionAmountPart, ContainerScreenTerminalStorageCraftingOptionAmount<PartPos, ContainerTerminalStorageCraftingOptionAmountPart>>() {
            @Override
            public ContainerScreenTerminalStorageCraftingOptionAmount<PartPos, ContainerTerminalStorageCraftingOptionAmountPart> create(ContainerTerminalStorageCraftingOptionAmountPart p_create_1_, PlayerInventory p_create_2_, ITextComponent p_create_3_) {
                return new ContainerScreenTerminalStorageCraftingOptionAmount<>(p_create_1_, p_create_2_, p_create_3_);
            }
        });
    }

}
