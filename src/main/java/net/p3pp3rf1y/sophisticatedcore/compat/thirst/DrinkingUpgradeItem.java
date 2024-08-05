package net.p3pp3rf1y.sophisticatedcore.compat.thirst;

import net.p3pp3rf1y.sophisticatedcore.upgrades.IUpgradeCountLimitConfig;
import net.p3pp3rf1y.sophisticatedcore.upgrades.UpgradeItemBase;
import net.p3pp3rf1y.sophisticatedcore.upgrades.UpgradeType;

import java.util.List;
import java.util.function.IntSupplier;

public class DrinkingUpgradeItem extends UpgradeItemBase<DrinkingUpgradeWrapper> {
	public static final UpgradeType<DrinkingUpgradeWrapper> TYPE = new UpgradeType<>(DrinkingUpgradeWrapper::new);

	private final IntSupplier filterSlotCount;

	public DrinkingUpgradeItem(IntSupplier filterSlotCount, IUpgradeCountLimitConfig upgradeTypeLimitConfig) {
		super(upgradeTypeLimitConfig);
		this.filterSlotCount = filterSlotCount;
	}

	public int getFilterSlotCount() {
		return filterSlotCount.getAsInt();
	}

	@Override
	public UpgradeType<DrinkingUpgradeWrapper> getType() {
		return TYPE;
	}

	@Override
	public List<UpgradeConflictDefinition> getUpgradeConflicts() {
		return List.of();
	}
}
