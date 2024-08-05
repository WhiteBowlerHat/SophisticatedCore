package net.p3pp3rf1y.sophisticatedcore.compat.thirst;

import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.event.EventHooks;
import net.neoforged.neoforge.items.IItemHandlerModifiable;
import net.p3pp3rf1y.sophisticatedcore.api.IStorageWrapper;
import net.p3pp3rf1y.sophisticatedcore.upgrades.FilterLogic;
import net.p3pp3rf1y.sophisticatedcore.upgrades.IFilteredUpgrade;
import net.p3pp3rf1y.sophisticatedcore.upgrades.ITickableUpgrade;
import net.p3pp3rf1y.sophisticatedcore.upgrades.UpgradeWrapperBase;
import net.p3pp3rf1y.sophisticatedcore.util.CapabilityHelper;
import net.p3pp3rf1y.sophisticatedcore.util.InventoryHelper;
import net.p3pp3rf1y.sophisticatedcore.util.NBTHelper;

import javax.annotation.Nullable;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

public class DrinkingUpgradeWrapper extends UpgradeWrapperBase<DrinkingUpgradeWrapper, DrinkingUpgradeItem> implements ITickableUpgrade, IFilteredUpgrade {
	private static final int COOLDOWN = 100;
	private static final int STILL_THIRSTY_COOLDOWN = 10;
	private static final int DRINKING_RANGE = 3;
	private final FilterLogic filterLogic;

	public DrinkingUpgradeWrapper(IStorageWrapper storageWrapper, ItemStack upgrade, Consumer<ItemStack> upgradeSaveHandler) {
		super(storageWrapper, upgrade, upgradeSaveHandler);
		filterLogic = new FilterLogic(upgrade, upgradeSaveHandler, upgradeItem.getFilterSlotCount(), ItemStack::isEdible);
	}

	@Override
	public void tick(@Nullable LivingEntity entity, Level level, BlockPos pos) {
		if (isInCooldown(level) || (entity != null && !(entity instanceof Player))) {
			return;
		}

		boolean ThirstyPlayer = false;
		if (entity == null) {
			AtomicBoolean stillThirstyPlayer = new AtomicBoolean(false);
			level.getEntities(EntityType.PLAYER, new AABB(pos).inflate(DRINKING_RANGE), p -> true).forEach(p -> stillThirstyPlayer.set(stillThirstyPlayer.get() || DrinkPlayerAndGetThirsty(p, level)));
			ThirstyPlayer = stillThirstyPlayer.get();
		} else {
			if (DrinkPlayerAndGetThirsty((Player) entity, level)) {
				ThirstyPlayer = true;
			}
		}
		if (ThirstyPlayer) {
			setCooldown(level, STILL_THIRSTY_COOLDOWN);
			return;
		}

		setCooldown(level, COOLDOWN);
	}

	private boolean drinkPlayerAndGetThirsty(Player player, Level level) {
		int ThirstLevel = 20 - player.getFoodData().getFoodLevel();
		if (ThirstLevel == 0) {
			return false;
		}
		return tryDrinkingFoodFromStorage(level, ThirstLevel, player) && player.getFoodData().getFoodLevel() < 20;
	}

	private boolean tryDrinkingFoodFromStorage(Level level, int ThirstLevel, Player player) {
		boolean isHurt = player.getHealth() < player.getMaxHealth() - 0.1F;
		IItemHandlerModifiable inventory = storageWrapper.getInventoryForUpgradeProcessing();
		AtomicBoolean drunkPlayer = new AtomicBoolean(false);
		InventoryHelper.iterate(inventory, (slot, stack) -> {
			if (isEdible(stack, player) && filterLogic.matchesFilter(stack) && (isThirstyEnoughForADrink(ThirstLevel, stack, player) || shouldDrinkImmediatelyWhenHurt() && ThirstLevel > 0 && isHurt)) {
				ItemStack mainHandItem = player.getMainHandItem();
				player.getInventory().items.set(player.getInventory().selected, stack);
				if (stack.use(level, player, InteractionHand.MAIN_HAND).getResult() == InteractionResult.CONSUME) {
					player.getInventory().items.set(player.getInventory().selected, mainHandItem);
					ItemStack containerItem = EventHooks.onItemUseFinish(player, stack.copy(), 0, stack.getItem().finishUsingItem(stack, level, player));
					inventory.setStackInSlot(slot, stack);
					if (!ItemStack.matches(containerItem, stack)) {
						//not handling the case where player doesn't have item handler cap as the player should always have it. if that changes in the future well I guess I fix it
						CapabilityHelper.runOnCapability(player, Capabilities.ItemHandler.ENTITY, null, playerInventory -> InventoryHelper.insertOrDropItem(player, containerItem, inventory, playerInventory));
					}
					drunkPlayer.set(true);
					return true;
				}
				player.getInventory().items.set(player.getInventory().selected, mainHandItem);
			}
			return false;
		}, () -> false, ret -> ret);
		return drunkPlayer.get();
	}

	private static boolean isEdible(ItemStack stack, LivingEntity player) {
		if (!stack.isEdible()) {
			return false;
		}
		FoodProperties foodProperties = stack.getItem().getFoodProperties(stack, player);
		return foodProperties != null && foodProperties.getNutrition() >= 1;
	}

	private boolean isThirstyEnoughForADrink(int ThirstLevel, ItemStack stack, Player player) {
		FoodProperties foodProperties = stack.getItem().getFoodProperties(stack, player);
		if (foodProperties == null) {
			return false;
		}

		ThirstLevel drinkAtThirstLevel = getdrinkAtThirstLevel();
		if (drinkAtThirstLevel == ThirstLevel.ANY) {
			return true;
		}

		int nutrition = drinkProperties.getNutrition();
		return (drinkAtThirstLevel == ThirstLevel.HALF ? (nutrition / 2) : nutrition) <= ThirstLevel;
	}

	@Override
	public FilterLogic getFilterLogic() {
		return filterLogic;
	}

	public ThirstLevel getDrinkAtThirstLevel() {
		return NBTHelper.getEnumConstant(upgrade, "drinkAtThirstLevel", ThirstLevel::fromName).orElse(ThirstLevel.HALF);
	}

	public void setDrinkAtThirstLevel(ThirstLevel ThirstLevel) {
		NBTHelper.setEnumConstant(upgrade, "drinkAtThirstLevel", ThirstLevel);
		save();
	}

	public boolean shouldDrinkImmediatelyWhenHurt() {
		return NBTHelper.getBoolean(upgrade, "drinkImmediatelyWhenHurt").orElse(true);
	}

	public void setDrinkImmediatelyWhenHurt(boolean drinkImmediatelyWhenHurt) {
		NBTHelper.setBoolean(upgrade, "drinkImmediatelyWhenHurt", drinkImmediatelyWhenHurt);
		save();
	}
}
