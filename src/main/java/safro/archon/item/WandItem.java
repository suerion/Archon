package safro.archon.item;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.item.TooltipContext;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.sound.SoundCategory;
import net.minecraft.text.Text;
import net.minecraft.util.*;
import net.minecraft.world.World;
import safro.archon.Archon;
import safro.archon.api.Element;
import safro.archon.api.Spell;
import safro.archon.enchantment.ArcaneEnchantment;
import safro.archon.registry.SpellRegistry;
import safro.archon.util.ArchonUtil;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class WandItem extends Item {
    private final Element type;

    public WandItem(Element element, Settings settings) {
        super(settings);
        this.type = element;
    }

    public Element getElement() {
        return this.type;
    }

    public TypedActionResult<ItemStack> use(World world, PlayerEntity player, Hand hand) {
        ItemStack stack = player.getStackInHand(hand);

        if (!world.isClient) {
            if (getSpells(player).isEmpty()) {
                player.sendMessage(Text.translatable("text.archon.invalid_spell").formatted(Formatting.RED), true);
                return TypedActionResult.pass(stack);
            }

            Spell current = getCurrentSpell(stack, player);
            if (player.isSneaking()) {
                this.cycleSpells(stack, player);
                player.sendMessage(Text.translatable(getCurrentSpell(stack, player).getTranslationKey()).formatted(Formatting.GREEN), true);
                return TypedActionResult.success(stack);

            } else if (current != null && !current.isBlockCasted() && current.canCast(world, player, stack)) {
                current.cast(world, player, stack);
                ArcaneEnchantment.applyArcane(player, stack, current.getManaCost());

                if (current.getCastSound() != null) {
                    world.playSound(null, player.getBlockPos(), current.getCastSound(), SoundCategory.PLAYERS, 0.9F, 1.0F);
                }
                return TypedActionResult.success(stack);
            }
        }
        return TypedActionResult.pass(stack);
    }

    public ActionResult useOnBlock(ItemUsageContext context) {
        ItemStack stack = context.getStack();
        PlayerEntity player = context.getPlayer();
        Spell current = getCurrentSpell(stack, player);

        if (current != null && current.isBlockCasted() && ArchonUtil.canRemoveMana(player, current.getManaCost())) {
            if (current.castOnBlock(context.getWorld(), context.getBlockPos(), player, stack) == ActionResult.SUCCESS) {
                ArcaneEnchantment.applyArcane(player, stack, current.getManaCost());

                if (current.getCastSound() != null) {
                    context.getWorld().playSound(null, player.getBlockPos(), current.getCastSound(), SoundCategory.PLAYERS, 0.9F, 1.0F);
                }
                return ActionResult.SUCCESS;
            }
        }
        return ActionResult.PASS;
    }

    public int getEnchantability() {
        return 1;
    }

    @Nullable
    public Spell getCurrentSpell(ItemStack stack, PlayerEntity player) {
        if (stack.getOrCreateSubNbt(Archon.MODID).contains("CurrentSpell")) {
            String name = stack.getOrCreateSubNbt(Archon.MODID).getString("CurrentSpell");
            return SpellRegistry.REGISTRY.get(new Identifier(name));
        }

        if (getSpells(player).isEmpty()) return null;
        Spell spell = getSpells(player).get(0);
        stack.getOrCreateSubNbt(Archon.MODID).putString("CurrentSpell", SpellRegistry.REGISTRY.getId(spell).toString());
        return spell;
    }

    public void cycleSpells(ItemStack stack, PlayerEntity player) {
        if (getSpells(player).size() > 1) {
            List<Spell> spells = ArchonUtil.getSpells(player);
            do {
                Collections.rotate(spells, 1);
                stack.getOrCreateSubNbt(Archon.MODID).putString("CurrentSpell", SpellRegistry.REGISTRY.getId(spells.get(0)).toString());
            } while (getCurrentSpell(stack, player).getElement() != this.getElement());
        }
    }

    public ArrayList<Spell> getSpells(PlayerEntity player) {
        ArrayList<Spell> list = new ArrayList<>();
        for (Spell spell : ArchonUtil.getSpells(player)) {
            if (spell.getElement() == this.getElement()) {
                list.add(spell);
            }
        }
        return list;
    }

    @Environment(EnvType.CLIENT)
    @Override
    public void appendTooltip(ItemStack stack, @Nullable World world, List<Text> tooltip, TooltipContext context) {
        if (stack.getOrCreateSubNbt(Archon.MODID).contains("CurrentSpell")) {
            String name = stack.getOrCreateSubNbt(Archon.MODID).getString("CurrentSpell");
            Spell spell = SpellRegistry.REGISTRY.get(new Identifier(name));
            tooltip.add(Text.translatable(spell.getTranslationKey()).formatted(Formatting.GRAY));
            tooltip.add(ArchonUtil.createManaText(spell.getManaCost(), false));
        } else
            tooltip.add(Text.translatable("text.archon.none").formatted(Formatting.GRAY));
    }
}
