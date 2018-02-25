package thebetweenlands.common.item.herblore;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import com.google.common.collect.Lists;

import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.client.resources.I18n;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.ai.attributes.AttributeModifier;
import net.minecraft.entity.ai.attributes.IAttribute;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.SoundEvents;
import net.minecraft.item.EnumAction;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.potion.Potion;
import net.minecraft.potion.PotionEffect;
import net.minecraft.stats.StatList;
import net.minecraft.util.ActionResult;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumHand;
import net.minecraft.util.NonNullList;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.StringUtils;
import net.minecraft.util.Tuple;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;
import thebetweenlands.client.handler.ItemTooltipHandler;
import thebetweenlands.client.tab.BLCreativeTabs;
import thebetweenlands.common.entity.projectiles.EntityElixir;
import thebetweenlands.common.herblore.elixir.ElixirEffectRegistry;
import thebetweenlands.common.herblore.elixir.ElixirRecipe;
import thebetweenlands.common.herblore.elixir.ElixirRecipes;
import thebetweenlands.common.herblore.elixir.effects.ElixirEffect;
import thebetweenlands.common.item.ITintedItem;
import thebetweenlands.common.registries.ItemRegistry;
import thebetweenlands.util.TranslationHelper;

public class ItemElixir extends Item implements ITintedItem, ItemRegistry.IBlockStateItemModelDefinition {
	private final List<ElixirEffect> effects = new ArrayList<>();

	public ItemElixir() {
		this.effects.addAll(ElixirEffectRegistry.getEffects());

		this.setCreativeTab(BLCreativeTabs.HERBLORE);
		this.setMaxStackSize(1);
		this.setHasSubtypes(true);
		this.setMaxDamage(0);

	}

	private ElixirEffect getElixirByID(int id) {
		for(ElixirEffect effect : this.effects) {
			if(id == effect.getID()) return effect;
		}
		return null;
	}

	private ElixirEffect getElixirFromItem(ItemStack stack) {
		return this.getElixirByID(stack.getItemDamage() / 2);
	}

	@Override
	public int getColorMultiplier(ItemStack stack, int tintIndex) {
		if (tintIndex <= 0) {
			ElixirEffect effect = this.getElixirFromItem(stack);
			if (effect != null) {
				ElixirRecipe recipe = ElixirRecipes.getFromEffect(effect);
				if (recipe != null) {
					return recipe.infusionFinishedColor;
				}
			}
		}
		return -1;
	}

	@Override
	public void getSubItems(CreativeTabs tab, NonNullList<ItemStack> items) {
		if (this.isInCreativeTab(tab)) {
			for (ElixirEffect effect : this.effects) {
				items.add(new ItemStack(this, 1, effect.getID() * 2));
				items.add(new ItemStack(this, 1, effect.getID() * 2 + 1));
			}
		}
	}

	@Override
	public String getUnlocalizedName(ItemStack stack) {
		try {
			return "item.thebetweenlands." + this.getElixirFromItem(stack).getEffectName();
		} catch (Exception e) {
			return "item.thebetweenlands.unknown";
		}
	}

	@Override
	public String getItemStackDisplayName(ItemStack stack) {
		if(I18n.hasKey(stack.getUnlocalizedName() + ".name")) {
			return I18n.format(stack.getUnlocalizedName() + ".name", TranslationHelper.translateToLocal(this.getElixirFromItem(stack).getEffectName()));
		}
		return I18n.format("item.thebetweenlands.bl.elixir.name", TranslationHelper.translateToLocal(this.getElixirFromItem(stack).getEffectName()));
	}

	@Override
	public EnumAction getItemUseAction(ItemStack stack) {
		if(stack.getTagCompound() != null && stack.getTagCompound().hasKey("throwing") && stack.getTagCompound().getBoolean("throwing")) {
			return EnumAction.BOW;
		}
		return EnumAction.DRINK;
	}

	@Override
	public void onPlayerStoppedUsing(ItemStack stack, World world, EntityLivingBase entityLiving, int timeLeft) {
		if(stack.getTagCompound() != null && stack.getTagCompound().hasKey("throwing") && stack.getTagCompound().getBoolean("throwing")) {
			if (!((EntityPlayer)entityLiving).capabilities.isCreativeMode) {
				stack.shrink(1);
				if(stack.isEmpty()) {
					((EntityPlayer) entityLiving).inventory.deleteStack(stack);
				}
			}
			world.playSound((EntityPlayer)entityLiving, entityLiving.getPosition(), SoundEvents.ENTITY_ARROW_SHOOT, SoundCategory.PLAYERS,0.5F, 0.4F / (itemRand.nextFloat() * 0.4F + 0.8F));
			if (!world.isRemote) {
				int useCount = this.getMaxItemUseDuration(stack) - timeLeft;
				EntityElixir elixir = new EntityElixir(world, entityLiving, stack);
				float strength = Math.min(0.2F + useCount / 20.0F, 1.0F);
				elixir.shoot(entityLiving, ((EntityPlayer)entityLiving).rotationPitch, ((EntityPlayer)entityLiving).rotationYaw, -20.0F, strength, 1.0F);
				world.spawnEntity(elixir);
			}
		}
	}

	/**
	 * Creates an item stack with the specified effect, duration, strength and vial type.
	 * Vial types: 0 = green, 1 = orange
	 * @param effect
	 * @param duration
	 * @param strength
	 * @param vialType
	 * @return
	 */
	public ItemStack getElixirItem(ElixirEffect effect, int duration, int strength, int vialType) {
		ItemStack elixirStack = new ItemStack(this, 1, effect.getID() * 2 + vialType);
		NBTTagCompound elixirData = new NBTTagCompound();
		elixirData.setInteger("duration", duration);
		elixirData.setInteger("strength", strength);
		if(elixirStack.getTagCompound() == null) elixirStack.setTagCompound(new NBTTagCompound());
		elixirStack.getTagCompound().setTag("elixirData", elixirData);
		return elixirStack;
	}

	@Override
	public int getMaxItemUseDuration(ItemStack stack) {
		if(stack.getTagCompound() != null && stack.getTagCompound().hasKey("throwing") && stack.getTagCompound().getBoolean("throwing")) {
			return 100000;
		}
		return 32;
	}

	@Override
	public ActionResult<ItemStack> onItemRightClick(World worldIn, EntityPlayer playerIn, EnumHand handIn) {
		ItemStack stack = playerIn.getHeldItem(handIn);
		if(stack.getTagCompound() == null) {
			stack.setTagCompound(new NBTTagCompound());
		}
		stack.getTagCompound().setBoolean("throwing", playerIn.isSneaking());
		playerIn.setActiveHand(handIn);
		return ActionResult.newResult(EnumActionResult.SUCCESS, stack);
	}

	@Override
	public ItemStack onItemUseFinish(ItemStack stack, World world, EntityLivingBase entityLiving) {
		EntityPlayer entityplayer = entityLiving instanceof EntityPlayer ? (EntityPlayer)entityLiving : null;
		if (entityplayer == null || !entityplayer.capabilities.isCreativeMode) {
			stack.shrink(1);
		}

		if (entityplayer instanceof EntityPlayerMP) {
			CriteriaTriggers.CONSUME_ITEM.trigger((EntityPlayerMP)entityplayer, stack);
		}

		if (!world.isRemote) {
			ElixirEffect effect = this.getElixirFromItem(stack);
			int duration = this.getElixirDuration(stack);
			int strength = this.getElixirStrength(stack);
			entityplayer.addPotionEffect(effect.createEffect(duration == -1 ? 1200 : duration, strength == -1 ? 0 : strength));
		}

		if (entityplayer != null) {
			entityplayer.addStat(StatList.getObjectUseStats(this));
		}

		//Add empty dentrothyst vial
		if (entityplayer == null || !entityplayer.capabilities.isCreativeMode) {
			if (stack.isEmpty()) {
				return ItemRegistry.DENTROTHYST_VIAL.createStack(stack.getItemDamage() % 2 == 0 ? 1 : 2);
			}
			if (entityplayer != null) {
				entityplayer.inventory.addItemStackToInventory(ItemRegistry.DENTROTHYST_VIAL.createStack(stack.getItemDamage() % 2 == 0 ? 1 : 2));
			}
		}

		return stack;
	}

	public void applyEffect(ItemStack stack, EntityLivingBase entity, double modifier) {
		ElixirEffect effect = this.getElixirFromItem(stack);
		int strength = this.getElixirStrength(stack);
		int duration = this.getElixirDuration(stack);
		entity.addPotionEffect(effect.createEffect((int)(duration * modifier), strength));
	}

	public PotionEffect createPotionEffect(ItemStack stack, double modifier) {
		ElixirEffect effect = this.getElixirFromItem(stack);
		int strength = this.getElixirStrength(stack);
		int duration = this.getElixirDuration(stack);
		return effect.createEffect((int)(duration * modifier), strength);
	}

	public int getElixirDuration(ItemStack stack) {
		if(stack.getTagCompound() != null && stack.getTagCompound().hasKey("elixirData")) {
			NBTTagCompound elixirData = stack.getTagCompound().getCompoundTag("elixirData");
			return elixirData.getInteger("duration");
		}
		return 1200;
	}

	public int getElixirStrength(ItemStack stack) {
		if(stack.getTagCompound() != null && stack.getTagCompound().hasKey("elixirData")) {
			NBTTagCompound elixirData = stack.getTagCompound().getCompoundTag("elixirData");
			return elixirData.getInteger("strength");
		}
		return 0;
	}

	@Override
	public void addInformation(ItemStack stack, World worldIn, List<String> tooltip, ITooltipFlag flagIn) {
		PotionEffect effect = this.createPotionEffect(stack, 1.0D);

		String potencyStr;
		if(I18n.hasKey("bl.elixir.potency." + effect.getAmplifier())) {
			potencyStr = I18n.format("bl.elixir.potency." + effect.getAmplifier());
		} else {
			potencyStr = I18n.format("bl.elixir.potency.n", effect.getAmplifier());
		}
		tooltip.add(I18n.format("tooltip.bl.elixir.potency", potencyStr));

		int durationLevel = MathHelper.floor(effect.getDuration() / 3600.0F);
		String durationLevelStr;
		if(I18n.hasKey("bl.elixir.duration." + durationLevel)) {
			durationLevelStr = I18n.format("bl.elixir.duration." + durationLevel);
		} else {
			durationLevelStr = I18n.format("bl.elixir.duration.n", (durationLevel + 1));
		}
		tooltip.add(I18n.format("tooltip.bl.elixir.duration", durationLevelStr, StringUtils.ticksToElapsedTime(effect.getDuration()), effect.getDuration()));

		Potion potion = effect.getPotion();
		List<Tuple<String, AttributeModifier>> modifiers = Lists.<Tuple<String, AttributeModifier>>newArrayList();
		Map<IAttribute, AttributeModifier> modifersMap = potion.getAttributeModifierMap();
		if (!modifersMap.isEmpty()) {
			for (Entry<IAttribute, AttributeModifier> entry : modifersMap.entrySet()) {
				AttributeModifier modifier = entry.getValue();
				modifier = new AttributeModifier(modifier.getName(), potion.getAttributeModifierAmount(effect.getAmplifier(), modifier), modifier.getOperation());
				modifiers.add(new Tuple<>(((IAttribute)entry.getKey()).getName(), modifier));
			}
		}
		
		ElixirEffect elixirEffect = this.getElixirFromItem(stack);
		
		boolean hasEffectDescription = I18n.hasKey("tooltip." + elixirEffect.getEffectName() + ".effect");
		
		if (!modifiers.isEmpty() || hasEffectDescription) {
			tooltip.add("");
			tooltip.add(TextFormatting.DARK_PURPLE + I18n.format("tooltip.bl.elixir.when_applied"));

			for (Tuple<String, AttributeModifier> tuple : modifiers) {
				AttributeModifier modifier = tuple.getSecond();
				double amount = modifier.getAmount();
				double adjustedAmount;

				if (modifier.getOperation() != 1 && modifier.getOperation() != 2) {
					adjustedAmount = modifier.getAmount();
				} else {
					adjustedAmount = modifier.getAmount() * 100.0D;
				}

				if (amount > 0.0D) {
					tooltip.add(TextFormatting.BLUE + I18n.format("attribute.modifier.plus." + modifier.getOperation(), ItemStack.DECIMALFORMAT.format(adjustedAmount), I18n.format("attribute.name." + (String)tuple.getFirst())));
				} else if (amount < 0.0D) {
					adjustedAmount = adjustedAmount * -1.0D;
					tooltip.add(TextFormatting.RED + I18n.format("attribute.modifier.take." + modifier.getOperation(), ItemStack.DECIMALFORMAT.format(adjustedAmount), I18n.format("attribute.name." + (String)tuple.getFirst())));
				}
			}
			
			if(hasEffectDescription) {
				tooltip.addAll(ItemTooltipHandler.splitTooltip(I18n.format("tooltip." + elixirEffect.getEffectName() + ".effect"), 0));
			}
		}
	}

	@Override
	public Map<Integer, String> getVariants() {
		Map<Integer, String> variants = new HashMap<>();
		for (ElixirEffect effect : this.effects) {
			variants.put(effect.getID() * 2, "green");
			variants.put(effect.getID() * 2 + 1, "orange");
		}
		return variants;
	}
}

