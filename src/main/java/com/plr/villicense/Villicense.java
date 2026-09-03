package com.plr.villicense;

import com.plr.villicense.item.VillagerLicenseItem;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Mod(Villicense.MOD_ID)
public class Villicense {
    public static final String MOD_ID = "villicense";
    public static final Logger LOGGER = LoggerFactory.getLogger(Villicense.MOD_ID);

    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(ForgeRegistries.ITEMS, MOD_ID);

    public static final RegistryObject<VillagerLicenseItem> VILLAGER_LICENSE =
            ITEMS.register("villager_license", VillagerLicenseItem::new);

    public Villicense(IEventBus modEventBus) {
        ITEMS.register(modEventBus);
        // BuildCreativeModeTabContentsEvent is an IModBusEvent, so register on the mod bus.
        modEventBus.addListener(this::buildCreativeTabContents);
    }

    /** Add the license to the "Tools & Utilities" creative tab. */
    private void buildCreativeTabContents(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.TOOLS_AND_UTILITIES) {
            event.accept(VILLAGER_LICENSE);
        }
    }
}
