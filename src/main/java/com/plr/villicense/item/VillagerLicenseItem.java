package com.plr.villicense.item;

import com.google.common.collect.Lists;
import com.plr.villicense.Villicense;
import com.plr.villicense.client.VillicenseClient;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.village.poi.PoiType;
import net.minecraft.world.entity.ai.village.poi.PoiTypes;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;

public class VillagerLicenseItem extends Item {
    public VillagerLicenseItem() {
        super(new Properties().stacksTo(1).rarity(Rarity.RARE));
    }

    @Override
    public InteractionResult interactLivingEntity(ItemStack itemStack, Player player, LivingEntity livingEntity, InteractionHand hand) {
        if (player.level().isClientSide() || hand != InteractionHand.MAIN_HAND || !(livingEntity instanceof Villager villager)) {
            return InteractionResult.PASS;
        }
        final ItemStack stack = player.getMainHandItem();
        final CompoundTag tag = stack.getOrCreateTag();
        tag.remove("confirm");

        if (!player.isShiftKeyDown()) {
            for (Component c : villagerInfo(villager)) {
                player.sendSystemMessage(c);
            }
            return InteractionResult.SUCCESS;
        }

        if (tag.contains("pos")) {
            final GlobalPos target = readBlockPos(tag);
            if (link(player, villager, target)) {
                stack.setTag(new CompoundTag());
            }
        } else {
            tag.putInt("entity_id", villager.getId());
            tag.putString("dim", player.level().dimension().location().toString());
            player.sendSystemMessage(Component.translatable("msg.villicense.villager.set", villager.getId()));
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        final CompoundTag tag = stack.getOrCreateTag();
        if (tag.contains("pos")) {
            final BlockPos pos = BlockPos.of(tag.getLong("pos"));
            final Component blockName = blockNameAt(tag);
            tooltip.add(Component.translatable("tooltip.villicense.block.set",
                    Component.translatable("format.villicense.name_pos", blockName,
                            Component.translatable("chat.coordinates", pos.getX(), pos.getY(), pos.getZ())
                                    .withStyle(ChatFormatting.GREEN))));
        } else {
            tooltip.add(Component.translatable("tooltip.villicense.block.not_set").withStyle(ChatFormatting.GRAY));
        }

        final boolean hasEntity = tag.contains("entity_id");
        tooltip.add(Component.translatable(
                hasEntity ? "tooltip.villicense.villager.set" : "tooltip.villicense.villager.not_set")
                .withStyle(hasEntity ? ChatFormatting.YELLOW : ChatFormatting.GRAY));

        for (int i = 0; i < 5; i++) {
            tooltip.add(Component.translatable("tooltip.villicense.%s".formatted(i), VillicenseClient.getName4CrouchingKey()));
        }
        super.appendHoverText(stack, level, tooltip, flag);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        if (context.getHand() != InteractionHand.MAIN_HAND) return InteractionResult.PASS;
        final Level level = context.getLevel();
        if (level.isClientSide()) return InteractionResult.PASS;
        final Player player = context.getPlayer();
        if (player == null) return InteractionResult.PASS;
        final ItemStack stack = player.getMainHandItem();
        final CompoundTag tag = stack.getOrCreateTag();

        if (player.isShiftKeyDown()) {
            tag.remove("confirm");
            final BlockPos pos = context.getClickedPos();
            if (!isValidBlock(level, pos)) {
                player.sendSystemMessage(Component.translatable("msg.villicense.block.invalid"));
                return InteractionResult.SUCCESS;
            }
            if (tag.contains("entity_id")) {
                final String entityDim = tag.getString("dim");
                final String currentDim = level.dimension().location().toString();
                if (!entityDim.isEmpty() && !entityDim.equals(currentDim)) {
                    player.sendSystemMessage(Component.translatable("msg.villicense.dim.different", entityDim, currentDim));
                    return InteractionResult.SUCCESS;
                }
                final Entity e = level.getEntity(tag.getInt("entity_id"));
                if (e instanceof Villager v && link(player, v, GlobalPos.of(level.dimension(), pos))) {
                    stack.setTag(new CompoundTag());
                    return InteractionResult.SUCCESS;
                }
            }
            tag.putLong("pos", pos.asLong());
            tag.putString("dim", level.dimension().location().toString());
            tag.putString("block", BuiltInRegistries.BLOCK.getKey(level.getBlockState(pos).getBlock()).toString());
            player.sendSystemMessage(Component.translatable("msg.villicense.block.set",
                    level.getBlockState(pos).getBlock().getName(),
                    Component.translatable("chat.coordinates", pos.getX(), pos.getY(), pos.getZ())));
        } else {
            if (tag.getBoolean("confirm")) {
                stack.setTag(new CompoundTag());
                player.sendSystemMessage(Component.translatable("msg.villicense.cancelled"));
            } else {
                tag.putBoolean("confirm", true);
                player.sendSystemMessage(Component.translatable("msg.villicense.cancellation.confirm"));
            }
        }
        return InteractionResult.SUCCESS;
    }

    /** Reconstruct the tooltip block name from the stored registry key. */
    private static Component blockNameAt(CompoundTag tag) {
        if (tag.contains("block")) {
            ResourceLocation key = ResourceLocation.tryParse(tag.getString("block"));
            if (key != null && BuiltInRegistries.BLOCK.containsKey(key)) {
                return Component.translatable(BuiltInRegistries.BLOCK.get(key).getDescriptionId());
            }
        }
        return Component.translatable("msg.villicense.none");
    }

    private static GlobalPos readBlockPos(CompoundTag tag) {
        final BlockPos pos = BlockPos.of(tag.getLong("pos"));
        final String dimName = tag.getString("dim");
        final ResourceKey<Level> dim = ResourceLocation.tryParse(dimName) != null
                ? ResourceKey.create(net.minecraft.core.registries.Registries.DIMENSION, ResourceLocation.tryParse(dimName))
                : Level.OVERWORLD;
        return GlobalPos.of(dim, pos);
    }

    private static List<MutableComponent> villagerInfo(Villager villager) {
        final List<MutableComponent> list = Lists.newArrayList();
        list.add(Component.translatable("msg.villicense.villager.info.title"));
        list.add(Component.translatable("msg.villicense.villager.info.uuid", villager.getUUID().toString()));
        list.add(Component.translatable("msg.villicense.villager.info.profession",
                Component.translatable(professionKey(villager))));
        list.add(Component.translatable("msg.villicense.villager.info.job_site", villagerMemory(villager, MemoryModuleType.JOB_SITE)));
        list.add(Component.translatable("msg.villicense.villager.info.bed", villagerMemory(villager, MemoryModuleType.HOME)));
        return list;
    }

    private static String professionKey(Villager villager) {
        return villager.getType().getDescriptionId() + "."
                + BuiltInRegistries.VILLAGER_PROFESSION.getKey(villager.getVillagerData().getProfession()).getPath();
    }

    private static MutableComponent villagerMemory(Villager villager, MemoryModuleType<GlobalPos> module) {
        var mem = villager.getBrain().getMemory(module);
        if (mem.isEmpty()) return Component.translatable("msg.villicense.none");
        final BlockPos pos = mem.get().pos();
        final Component posComp = Component.translatable("chat.coordinates", pos.getX(), pos.getY(), pos.getZ())
                .withStyle(style -> style.withColor(ChatFormatting.GREEN)
                        .withClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND,
                                "/tp @s " + pos.getX() + " " + pos.getY() + " " + pos.getZ()))
                        .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.translatable("chat.coordinates.tooltip"))));
        return Component.translatable("format.villicense.name_pos",
                villager.level().getBlockState(pos).getBlock().getName(), posComp);
    }

    private static boolean link(Player player, Villager villager, GlobalPos target) {
        if (!(player.level() instanceof ServerLevel level)) return false;
        final ResourceLocation targetDim = target.dimension().location();
        final ResourceLocation currentDim = level.dimension().location();
        if (!targetDim.equals(currentDim)) {
            player.sendSystemMessage(Component.translatable("msg.villicense.dim.different",
                    targetDim.toString(), currentDim.toString()));
            return false;
        }
        final BlockPos pos = target.pos();
        if (!isValidBlock(level, pos)) {
            player.sendSystemMessage(Component.translatable("msg.villicense.block.may_have_changed",
                    level.getBlockState(pos).getBlock().getName()));
            return false;
        }
        final BlockState state = level.getBlockState(pos);
        if (state.is(BlockTags.BEDS)) {
            villager.getBrain().setMemory(MemoryModuleType.HOME, GlobalPos.of(level.dimension(), pos));
            villager.refreshBrain(level);
            player.sendSystemMessage(Component.translatable("msg.villicense.bed.set",
                    Component.translatable("chat.coordinates", pos.getX(), pos.getY(), pos.getZ())));
            return true;
        }

        final Optional<Holder<PoiType>> poi = PoiTypes.forState(state);
        if (poi.isEmpty()) return false;
        final AtomicBoolean done = new AtomicBoolean(false);
        BuiltInRegistries.VILLAGER_PROFESSION.stream()
                .filter(p -> p.heldJobSite().test(poi.get()))
                .findFirst()
                .ifPresent(p -> {
                    villager.setVillagerData(villager.getVillagerData().setProfession(p));
                    villager.getBrain().setMemory(MemoryModuleType.JOB_SITE, GlobalPos.of(level.dimension(), pos));
                    villager.refreshBrain(level);
                    done.set(true);
                    player.sendSystemMessage(Component.translatable("msg.villicense.profession.set",
                            Component.translatable(professionKey(villager))));
                });
        return done.get();
    }

    private static boolean isValidBlock(Level level, BlockPos pos) {
        final BlockState state = level.getBlockState(pos);
        if (state.is(BlockTags.BEDS)) return true;
        final Optional<Holder<PoiType>> poi = PoiTypes.forState(state);
        if (poi.isEmpty()) return false;
        return poi.get().unwrapKey().filter(getAvailablePoiTypes()::contains).isPresent();
    }

    private static List<ResourceKey<PoiType>> availablePoi;

    /** All POI types that at least one villager profession treats as a valid job site. */
    private static List<ResourceKey<PoiType>> getAvailablePoiTypes() {
        if (availablePoi == null) {
            final List<ResourceKey<PoiType>> list = Lists.newArrayList();
            BuiltInRegistries.POINT_OF_INTEREST_TYPE.holders()
                    .filter(holder -> BuiltInRegistries.VILLAGER_PROFESSION.stream()
                            .anyMatch(p -> p.heldJobSite().test(holder)))
                    .forEach(holder -> holder.unwrapKey().ifPresent(list::add));
            availablePoi = list;
        }
        return availablePoi;
    }
}
