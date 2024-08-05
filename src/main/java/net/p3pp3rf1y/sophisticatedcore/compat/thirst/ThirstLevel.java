package net.p3pp3rf1y.sophisticatedcore.compat.thirst;

import com.google.common.collect.ImmutableMap;
import net.minecraft.util.StringRepresentable;

import java.util.Map;

public enum ThirstLevel implements StringRepresentable {
	ANY("any"),
	HALF("half"),
	FULL("full");

	private final String name;

	ThirstLevel(String name) {this.name = name;}

	@Override
	public String getSerializedName() {
		return name;
	}

	public ThirstLevel next() {
		return VALUES[(ordinal() + 1) % VALUES.length];
	}

	private static final Map<String, ThirstLevel> NAME_VALUES;
	private static final ThirstLevel[] VALUES;

	static {
		ImmutableMap.Builder<String, ThirstLevel> builder = new ImmutableMap.Builder<>();
		for (ThirstLevel value : ThirstLevel.values()) {
			builder.put(value.getSerializedName(), value);
		}
		NAME_VALUES = builder.build();
		VALUES = values();
	}

	public static ThirstLevel fromName(String name) {
		return NAME_VALUES.getOrDefault(name, HALF);
	}
}
