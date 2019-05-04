package com.gamerforea.railcraft;

import com.gamerforea.eventhelper.config.Config;
import com.gamerforea.eventhelper.config.ConfigBoolean;
import com.gamerforea.eventhelper.config.ConfigInt;

@Config(name = "RailCraft")
public final class EventConfig
{
	private static final String CATEGORY_OTHER = "other";

	@ConfigBoolean(category = CATEGORY_OTHER, comment = "Включить взрывы вагонеток при столковении с мобами")
	public static boolean cartExplosions = true;

	@ConfigInt(category = CATEGORY_OTHER, comment = "Максимальный радиус прогрузки Мирового якоря", min = 0, max = 16)
	public static int worldAnchorRadius = 1;

	@ConfigInt(category = CATEGORY_OTHER, comment = "Максимальное количество чанков, прогружаемых Мировым якорем", min = 0)
	public static int worldAnchorMaxSentinelChunks = 25;
}
