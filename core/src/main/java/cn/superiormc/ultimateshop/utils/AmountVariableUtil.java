package cn.superiormc.ultimateshop.utils;

import cn.superiormc.ultimateshop.managers.CacheManager;
import cn.superiormc.ultimateshop.managers.ChinaHolidayManager;
import cn.superiormc.ultimateshop.managers.ConfigManager;
import cn.superiormc.ultimateshop.objects.buttons.ObjectItem;
import cn.superiormc.ultimateshop.objects.caches.ObjectCache;
import cn.superiormc.ultimateshop.objects.caches.ObjectUseTimesCache;
import cn.superiormc.ultimateshop.objects.items.prices.PriceMode;
import org.bukkit.Statistic;
import org.bukkit.entity.Player;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class AmountVariableUtil {

    private AmountVariableUtil() {
    }

    public static String replacePriceVariables(Player player, String text, ObjectItem item,
                                               int offsetAmount, PriceMode priceMode, ObjectCache objectCache) {
        return replaceVariables(player, text, item, offsetAmount, priceMode == PriceMode.BUY, true, objectCache);
    }

    public static String replaceProductVariables(Player player, String text, ObjectItem item,
                                                 int offsetAmount, boolean buyOrSell, ObjectCache objectCache) {
        return replaceVariables(player, text, item, offsetAmount, buyOrSell, true, objectCache);
    }

    public static String replaceLimitVariables(Player player, String text, ObjectItem item, ObjectCache objectCache) {
        return replaceVariables(player, text, item, 0, true, false, objectCache);
    }

    private static String replaceVariables(Player player, String text, ObjectItem item,
                                           int offsetAmount, boolean buyOrSell, boolean includeLimitVariables,
                                           ObjectCache objectCache) {
        ObjectUseTimesCache playerCache = objectCache.getUseTimesCache().get(item);
        ObjectUseTimesCache serverCache = CacheManager.cacheManager.serverCache.getUseTimesCache().get(item);
        long activeTicks = getActiveTicks(player);
        int dayOfYear = CommonUtil.getNowTime().getDayOfYear();

        int playerBuyTimes = playerCache != null ? playerCache.getBuyUseTimes() : 0;
        int playerSellTimes = playerCache != null ? playerCache.getSellUseTimes() : 0;
        int playerTotalBuyTimes = playerCache != null ? playerCache.getTotalBuyUseTimes() : 0;
        int playerTotalSellTimes = playerCache != null ? playerCache.getTotalSellUseTimes() : 0;
        int serverBuyTimes = serverCache != null ? serverCache.getBuyUseTimes() : 0;
        int serverSellTimes = serverCache != null ? serverCache.getSellUseTimes() : 0;
        int serverTotalBuyTimes = serverCache != null ? serverCache.getTotalBuyUseTimes() : 0;
        int serverTotalSellTimes = serverCache != null ? serverCache.getTotalSellUseTimes() : 0;

        int adjustedPlayerBuyTimes = applyOffset(playerBuyTimes, offsetAmount, buyOrSell, true);
        int adjustedPlayerSellTimes = applyOffset(playerSellTimes, offsetAmount, buyOrSell, false);
        int adjustedServerBuyTimes = applyOffset(serverBuyTimes, offsetAmount, buyOrSell, true);
        int adjustedServerSellTimes = applyOffset(serverSellTimes, offsetAmount, buyOrSell, false);
        int adjustedPlayerTotalBuyTimes = applyOffset(playerTotalBuyTimes, offsetAmount, buyOrSell, true);
        int adjustedPlayerTotalSellTimes = applyOffset(playerTotalSellTimes, offsetAmount, buyOrSell, false);
        int adjustedServerTotalBuyTimes = applyOffset(serverTotalBuyTimes, offsetAmount, buyOrSell, true);
        int adjustedServerTotalSellTimes = applyOffset(serverTotalSellTimes, offsetAmount, buyOrSell, false);
        int periodSellLimit = periodSellLimit(player, item);

        double lastSellPlayerSeconds = seconds(playerCache != null ? playerCache.getSellLastTimeName() : "0");
        double lastResetSellPlayerSeconds = seconds(playerCache != null ? playerCache.getSellLastResetTimeName() : "0");
        double lastSellServerSeconds = seconds(serverCache != null ? serverCache.getSellLastTimeName() : "0");
        double lastResetSellServerSeconds = seconds(serverCache != null ? serverCache.getSellLastResetTimeName() : "0");
        String calculatedEnvironmentIndex = modelEnvironmentIndex(player, dayOfYear);
        LocalDate today = CommonUtil.getNowTime().toLocalDate();
        ChinaHolidayManager hm = ChinaHolidayManager.chinaHolidayManager;
        boolean holidayEnabled = hm != null && hm.isLoaded();
        int isChinaHoliday = 0;
        int isChinaWorkday = 1;
        String chinaHolidayName = "";
        String chinaHolidayBeta = modelValue(player, "beta", "1");
        String muWinterAuto = modelValue(player, "mu-winter", "0");
        String muSummerAuto = modelValue(player, "mu-summer", "0");
        String muNationalAuto = modelValue(player, "mu-national", "0");
        if (holidayEnabled) {
            isChinaHoliday = hm.isHoliday(today) ? 1 : 0;
            isChinaWorkday = hm.isActualWorkday(today) ? 1 : 0;
            chinaHolidayName = hm.getHolidayName(today);
            chinaHolidayBeta = decimal(hm.getBeta(today));
            if (hm.getMuWinter() > 0) {
                muWinterAuto = decimal(hm.getMuWinter());
            }
            if (hm.getMuNational() > 0) {
                muNationalAuto = decimal(hm.getMuNational());
            }
        }

        double delta = modelDouble(player, "decay-delta", 1);
        double tauDays = modelDouble(player, "decay-tau-days", 7);
        String sellDecayedPlayer = computeDecayedFromHistory(
                playerCache, true, true, adjustedPlayerSellTimes, lastResetSellPlayerSeconds, delta, tauDays);
        String sellDecayedServer = computeDecayedFromHistory(
                serverCache, false, true, adjustedServerSellTimes, lastResetSellServerSeconds, delta, tauDays);
        String buyDecayedPlayer = computeDecayedFromHistory(
                playerCache, true, false, adjustedPlayerBuyTimes, 0, delta, tauDays);
        String buyDecayedServer = computeDecayedFromHistory(
                serverCache, false, false, adjustedServerBuyTimes, 0, delta, tauDays);

        String[] commonVariables = new String[] {
                "buy-times-player", String.valueOf(adjustedPlayerBuyTimes),
                "sell-times-player", String.valueOf(adjustedPlayerSellTimes),
                "buy-times-server", String.valueOf(adjustedServerBuyTimes),
                "sell-times-server", String.valueOf(adjustedServerSellTimes),
                "buy-total-player", String.valueOf(adjustedPlayerTotalBuyTimes),
                "sell-total-player", String.valueOf(adjustedPlayerTotalSellTimes),
                "buy-total-server", String.valueOf(adjustedServerTotalBuyTimes),
                "sell-total-server", String.valueOf(adjustedServerTotalSellTimes),
                "last-buy-player", playerCache != null ? playerCache.getBuyLastTimeName() : "0",
                "last-sell-player", playerCache != null ? playerCache.getSellLastTimeName() : "0",
                "last-buy-server", serverCache != null ? serverCache.getBuyLastTimeName() : "0",
                "last-sell-server", serverCache != null ? serverCache.getSellLastTimeName() : "0",
                "last-reset-buy-player", playerCache != null ? playerCache.getBuyLastResetTimeName() : "0",
                "last-reset-sell-player", playerCache != null ? playerCache.getSellLastResetTimeName() : "0",
                "last-reset-buy-server", serverCache != null ? serverCache.getBuyLastResetTimeName() : "0",
                "last-reset-sell-server", serverCache != null ? serverCache.getSellLastResetTimeName() : "0",
                "active-ticks", String.valueOf(activeTicks),
                "active-seconds", decimal(activeTicks / 20.0),
                "active-minutes", decimal(activeTicks / 1200.0),
                "active-hours", decimal(activeTicks / 72000.0),
                "P", decimal(activeTicks / 72000.0),
                "current-day-of-year", String.valueOf(dayOfYear),
                "day-of-year", String.valueOf(dayOfYear),
                "t", String.valueOf(dayOfYear),
                "base-price", modelValue(player, "base-price", "0"),
                "p0", modelValue(player, "base-price", "0"),
                "p_0", modelValue(player, "base-price", "0"),
                "quota-base", modelValue(player, "quota-base", "0"),
                "Q_B", modelValue(player, "quota-base", "0"),
                "active-weight", modelValue(player, "active-weight", "0"),
                "gamma", modelValue(player, "active-weight", "0"),
                "total-quota", modelValue(player, "total-quota", "0"),
                "T", modelValue(player, "total-quota", "0"),
                "quota", modelQuota(player, activeTicks),
                "Q", modelQuota(player, activeTicks),
                "environment-index", modelValue(player, "environment-index", "1"),
                "epsilon", modelValue(player, "environment-index", "1"),
                "environment-index-calculated", calculatedEnvironmentIndex,
                "epsilon-calculated", calculatedEnvironmentIndex,
                "special-price-index", modelValue(player, "special-price-index", "1"),
                "iota", modelValue(player, "special-price-index", "1"),
                "beta", modelValue(player, "beta", "1"),
                "noise", modelValue(player, "noise", "0"),
                "noise-sigma", modelValue(player, "noise-sigma", "0.025"),
                "sigma_n", modelValue(player, "noise-sigma", "0.025"),
                "noise-x", modelValue(player, "noise-x", "0"),
                "x", modelValue(player, "noise-x", "0"),
                "alpha-winter", modelValue(player, "alpha-winter", "0.15"),
                "alpha_winter", modelValue(player, "alpha-winter", "0.15"),
                "alpha-summer", modelValue(player, "alpha-summer", "0.15"),
                "alpha_summer", modelValue(player, "alpha-summer", "0.15"),
                "alpha-national", modelValue(player, "alpha-national", "0.075"),
                "alpha_national", modelValue(player, "alpha-national", "0.075"),
                "mu-winter", modelValue(player, "mu-winter", "0"),
                "mu_winter", modelValue(player, "mu-winter", "0"),
                "mu-summer", modelValue(player, "mu-summer", "0"),
                "mu_summer", modelValue(player, "mu-summer", "0"),
                "mu-national", modelValue(player, "mu-national", "0"),
                "mu_national", modelValue(player, "mu-national", "0"),
                "sigma-winter", modelValue(player, "sigma-winter", "65536"),
                "sigma_winter", modelValue(player, "sigma-winter", "65536"),
                "sigma-summer", modelValue(player, "sigma-summer", "65536"),
                "sigma_summer", modelValue(player, "sigma-summer", "65536"),
                "sigma-national", modelValue(player, "sigma-national", "33.1776"),
                "sigma_national", modelValue(player, "sigma-national", "33.1776"),
                "period-sell-limit", String.valueOf(periodSellLimit),
                "nu", String.valueOf(periodSellLimit),
                "price-decay", modelValue(player, "price-decay", "0.05"),
                "lambda", modelValue(player, "price-decay", "0.05"),
                "decay-delta", modelValue(player, "decay-delta", "1"),
                "delta", modelValue(player, "decay-delta", "1"),
                "decay-tau-days", modelValue(player, "decay-tau-days", "7"),
                "tau", modelValue(player, "decay-tau-days", "7"),
                "decay-history-days", decayHistoryDays(player, periodSellLimit),
                "T_history", decayHistoryDays(player, periodSellLimit),
                "sell-decayed-player", sellDecayedPlayer,
                "sell-decayed-server", sellDecayedServer,
                "buy-decayed-player", buyDecayedPlayer,
                "buy-decayed-server", buyDecayedServer,
                "sell-revenue-player", playerCache != null ? decimal(playerCache.getTotalSellRevenue()) : "0",
                "M_sell", playerCache != null ? decimal(playerCache.getTotalSellRevenue()) : "0",
                "buy-cost-player", playerCache != null ? decimal(playerCache.getTotalBuyCost()) : "0",
                "M_buy", playerCache != null ? decimal(playerCache.getTotalBuyCost()) : "0",
                "sell-revenue-server", serverCache != null ? decimal(serverCache.getTotalSellRevenue()) : "0",
                "buy-cost-server", serverCache != null ? decimal(serverCache.getTotalBuyCost()) : "0",
                "is-china-holiday", String.valueOf(isChinaHoliday),
                "is-china-workday", String.valueOf(isChinaWorkday),
                "china-holiday-name", chinaHolidayName,
                "china-holiday-beta", chinaHolidayBeta,
                "mu-winter-auto", muWinterAuto,
                "mu-summer-auto", muSummerAuto,
                "mu-national-auto", muNationalAuto
        };

        text = CommonUtil.modifyString(player, text, commonVariables);
        if (!includeLimitVariables) {
            return text;
        }

        int buyLimitPlayer = item.getPlayerBuyLimit(player);
        int sellLimitPlayer = item.getPlayerSellLimit(player);
        int buyLimitServer = item.getServerBuyLimit(player);
        int sellLimitServer = item.getServerSellLimit(player);
        int buyTimesMaxPlayer = item.getBuyTimesMaxValue(player);
        int sellTimesMaxPlayer = item.getSellTimesMaxValue(player);

        return CommonUtil.modifyString(player, text,
                "buy-limit-player", String.valueOf(buyLimitPlayer),
                "sell-limit-player", String.valueOf(sellLimitPlayer),
                "buy-limit-server", String.valueOf(buyLimitServer),
                "sell-limit-server", String.valueOf(sellLimitServer),
                "buy-limit-left-player", remaining(buyLimitPlayer, adjustedPlayerBuyTimes),
                "sell-limit-left-player", remaining(sellLimitPlayer, adjustedPlayerSellTimes),
                "buy-limit-left-server", remaining(buyLimitServer, adjustedServerBuyTimes),
                "sell-limit-left-server", remaining(sellLimitServer, adjustedServerSellTimes),
                "buy-times-max-player", String.valueOf(buyTimesMaxPlayer),
                "sell-times-max-player", String.valueOf(sellTimesMaxPlayer),
                "buy-times-left-player", remaining(buyTimesMaxPlayer, adjustedPlayerBuyTimes),
                "sell-times-left-player", remaining(sellTimesMaxPlayer, adjustedPlayerSellTimes));
    }

    private static String computeDecayedFromHistory(ObjectUseTimesCache cache, boolean isPlayer,
                                                     boolean isSell, int currentTimes,
                                                     double currentElapsedSeconds,
                                                     double delta, double tauDays) {
        if (cache == null) {
            return "0";
        }
        List<ObjectUseTimesCache.PeriodRecord> rawHistory = isSell
                ? cache.getSellHistory() : cache.getBuyHistory();
        List<DecayCalculator.PeriodRecord> history = new ArrayList<>();
        for (ObjectUseTimesCache.PeriodRecord r : rawHistory) {
            history.add(new DecayCalculator.PeriodRecord(r.getBuyTimes(), r.getSellTimes(), r.getResetTime()));
        }
        LocalDateTime currentResetTime = isSell
                ? cache.getLastResetSellTimeAsDateTime()
                : cache.getLastResetBuyTimeAsDateTime();
        double result = DecayCalculator.computeDecayedFromHistory(
                history, currentTimes, currentResetTime, isSell, delta, tauDays, CommonUtil.getNowTime());
        return decimal(result);
    }

    private static int applyOffset(int value, int offsetAmount, boolean buyOrSell, boolean placeholderBuyOrSell) {
        if ((buyOrSell && placeholderBuyOrSell) || (!buyOrSell && !placeholderBuyOrSell)) {
            return value + offsetAmount;
        }
        return value;
    }

    private static long getActiveTicks(Player player) {
        if (player == null) {
            return 0;
        }
        return player.getStatistic(Statistic.PLAY_ONE_MINUTE);
    }

    private static String modelValue(Player player, String key, String defaultValue) {
        String value = ConfigManager.configManager.getString("placeholder.data.economy-model." + key, defaultValue);
        return TextUtil.withPAPI(value, player);
    }

    private static double modelDouble(Player player, String key, double defaultValue) {
        try {
            return Double.parseDouble(modelValue(player, key, decimal(defaultValue)));
        } catch (NumberFormatException ignored) {
            return defaultValue;
        }
    }

    private static String modelQuota(Player player, long activeTicks) {
        return decimal(DecayCalculator.computeQuota(
                modelDouble(player, "quota-base", 0),
                modelDouble(player, "active-weight", 0),
                activeTicks / 72000.0,
                modelDouble(player, "total-quota", 0)));
    }

    private static String modelEnvironmentIndex(Player player, int dayOfYear) {
        double holidayImpact = DecayCalculator.holidayImpact(modelDouble(player, "alpha-winter", 0.15), modelDouble(player, "mu-winter", 0), modelDouble(player, "sigma-winter", 65536), dayOfYear)
                + DecayCalculator.holidayImpact(modelDouble(player, "alpha-summer", 0.15), modelDouble(player, "mu-summer", 0), modelDouble(player, "sigma-summer", 65536), dayOfYear)
                + DecayCalculator.holidayImpact(modelDouble(player, "alpha-national", 0.075), modelDouble(player, "mu-national", 0), modelDouble(player, "sigma-national", 33.1776), dayOfYear);
        double beta = modelDouble(player, "beta", 1);
        double noise = modelDouble(player, "noise", 0);
        return decimal((1 - holidayImpact) * beta + noise);
    }

    private static int periodSellLimit(Player player, ObjectItem item) {
        if (item == null) {
            return Math.max(0, (int) modelDouble(player, "period-sell-limit", 0));
        }
        int maxValue = item.getSellTimesMaxValue(player);
        if (maxValue >= 0) {
            return maxValue;
        }
        return Math.max(0, (int) modelDouble(player, "period-sell-limit", 0));
    }

    private static String decayHistoryDays(Player player, int periodSellLimit) {
        return decimal(DecayCalculator.computeDecayHistoryDays(
                modelDouble(player, "decay-tau-days", 7),
                modelDouble(player, "decay-delta", 1),
                periodSellLimit));
    }

    private static double seconds(String value) {
        if (value == null || value.isEmpty()) {
            return 0;
        }
        try {
            return Double.parseDouble(value);
        } catch (NumberFormatException ignored) {
            return 0;
        }
    }

    private static String remaining(int limit, int used) {
        if (limit < 0) {
            return "-1";
        }
        return String.valueOf(Math.max(limit - used, 0));
    }

    private static String decimal(double value) {
        if (Double.isNaN(value) || Double.isInfinite(value)) {
            return "0";
        }
        return BigDecimal.valueOf(value)
                .setScale(10, RoundingMode.HALF_UP)
                .stripTrailingZeros()
                .toPlainString();
    }
}
