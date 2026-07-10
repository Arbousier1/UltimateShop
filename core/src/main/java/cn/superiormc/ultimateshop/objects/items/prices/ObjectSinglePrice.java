package cn.superiormc.ultimateshop.objects.items.prices;

import cn.superiormc.ultimateshop.UltimateShop;
import cn.superiormc.ultimateshop.managers.CacheManager;
import cn.superiormc.ultimateshop.managers.ConfigManager;
import cn.superiormc.ultimateshop.methods.StaticPlaceholder;
import cn.superiormc.ultimateshop.objects.buttons.ObjectItem;
import cn.superiormc.ultimateshop.objects.caches.ObjectCache;
import cn.superiormc.ultimateshop.objects.items.AbstractSingleThing;
import cn.superiormc.ultimateshop.objects.items.GiveItemStack;
import cn.superiormc.ultimateshop.objects.items.ItemStorage;
import cn.superiormc.ultimateshop.utils.AmountVariableUtil;
import cn.superiormc.ultimateshop.utils.CommonUtil;
import cn.superiormc.ultimateshop.utils.ItemUtil;
import cn.superiormc.ultimateshop.utils.MathUtil;
import cn.superiormc.ultimateshop.utils.TextUtil;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static cn.superiormc.ultimateshop.utils.MathUtil.scale;

public class ObjectSinglePrice extends AbstractSingleThing {

    private Map<Integer, BigDecimal> applyCostMap = new HashMap<>();

    private boolean customPrice;

    private ObjectItem item;

    private PriceMode priceMode;

    private boolean isStatic;

    private boolean isAlwaysApply = false;

    private BigDecimal baseAmount;

    private String amountOption;

    public ObjectSinglePrice() {
        super();
    }

    public ObjectSinglePrice(String id, ObjectPrices prices) {
        super(id, prices);
        this.item = prices.getItem();
        this.priceMode = prices.getPriceMode();
        this.things = prices;
        this.amountOption = singleSection.getString("amount", "1");
        String tempVal1 = ConfigManager.configManager.getString("prices." + singleSection.getString("custom-type") + ".amount", "1");
        if (singleSection.getString("custom-type") != null && tempVal1 != null) {
            this.amountOption = tempVal1;
        }
        this.isStatic = amountOption.matches("-?\\d+(\\.\\d+)?");
        if (!amountOption.contains("{conditional_") &&
                ConfigManager.configManager.getBooleanOrDefault("placeholder.auto-settings.add-discount-in-all-price-amount.enabled",
                        "placeholder.auto-settings.add-conditional-in-all-price-amount.enabled") &&
                !ConfigManager.configManager.getStringListOrDefault("placeholder.auto-settings.add-discount-in-all-price-amount.black-shops",
                        "placeholder.auto-settings.add-conditional-in-all-price-amount.black-shops").contains(prices.getItem().getShop())) {
            if (!ConfigManager.configManager.getBooleanOrDefault("placeholder.auto-settings.add-discount-in-all-price-amount.black-dynamic-price",
                    "placeholder.auto-settings.add-conditional-in-all-price-amount.black-dynamic-price") ||
                    isStatic) {
                if (prices.getPriceMode() == PriceMode.BUY) {
                    this.amountOption = "{conditional_" + ConfigManager.configManager.getStringOrDefault("placeholder.auto-settings.add-discount-in-all-price-amount.buy-placeholder",
                            "placeholder.auto-settings.add-conditional-in-all-price-amount.black-dynamic-price", "") + "} * (" + amountOption + ")";
                } else {
                    this.amountOption = "{conditional_" + ConfigManager.configManager.getStringOrDefault("placeholder.auto-settings.add-discount-in-all-price-amount.sell-placeholder",
                            "placeholder.auto-settings.add-conditional-in-all-price-amount.sell-placeholder", "") +
                            "} * (" + amountOption + ")";
                }
                isStatic = false;
            }
        }
        Pattern pattern = Pattern.compile("-?\\d+(\\.\\d+)?");
        Matcher matcher = pattern.matcher(amountOption);
        if (matcher.find()) {
            this.baseAmount = new BigDecimal(matcher.group());
        }
        initCustomMode();
        initApplyCostMap();
        initApplyCondition();
        initRequireCondition();
        initAction();
    }

    private void initCustomMode() {
        customPrice = !singleSection.getString("custom-type", "none").equals("none");
    }

    private void initApplyCostMap() {
        if (Objects.isNull(singleSection) || (singleSection.getInt("start-apply", -1) != -1
                && singleSection.getInt("end-apply", -1) != -1)) {
            isAlwaysApply = true;
            return;
        }
        List<Integer> apply = singleSection.getIntegerList("apply");
        List<Double> cost = singleSection.getDoubleList("cost");
        while (apply.size() > cost.size()) {
            if (!cost.isEmpty()) {
                cost.add(cost.get(cost.size() - 1));
            } else {
                cost.add(-1.0);
            }
        }
        Map<Integer, BigDecimal> applyCostMap = new HashMap<>();
        for (int i = 0 ; i < apply.size() ; i++) {
            applyCostMap.put(apply.get(i), BigDecimal.valueOf(cost.get(i)));
        }
        this.applyCostMap = applyCostMap;
    }

    @Override
    public boolean playerHasEnough(ItemStorage storage,
                                   Player player,
                                   boolean take,
                                   double cost) {
        if (singleSection == null) {
            return false;
        }
        if (customPrice) {
            return super.playerHasEnough(storage,
                    ConfigManager.configManager.config.
                            getConfigurationSection("prices." + singleSection.getString("custom-type")),
                    player,
                    take,
                    cost);
        }
        return super.playerHasEnough(storage, singleSection, player, take, cost);
    }

    public boolean isStatic() {
        return isStatic;
    }

    public BigDecimal getAmount(Player player, int times, int offsetAmount) {
        if (singleSection == null) {
            return new BigDecimal(-1);
        }
        BigDecimal cost;
        if (isStatic()) {
            cost = baseAmount;
        } else {
            String tempVal1 = amountOption;
            if (item != null && ConfigManager.configManager.getBoolean("placeholder.data.can-used-in-amount")) {
                ObjectCache cache = CacheManager.cacheManager.getObjectCache(player);
                if (cache == null) {
                    return new BigDecimal(-1);
                }
                tempVal1 = AmountVariableUtil.replacePriceVariables(player, tempVal1, item, offsetAmount, priceMode, cache);
            }
            cost = MathUtil.doCalculate(TextUtil.withPAPI(tempVal1, player));
        }
        if (singleSection.getString("max-amount") != null) {
            BigDecimal maxAmount = new BigDecimal(TextUtil.withPAPI(singleSection.getString("max-amount"), player));
            if (cost.compareTo(maxAmount) > 0) {
                cost = maxAmount;
            }
        }
        if (singleSection.getString("min-amount") != null) {
            BigDecimal minAmount = new BigDecimal(TextUtil.withPAPI(singleSection.getString("min-amount"), player));
            if (cost.compareTo(minAmount) < 0) {
                cost = minAmount;
            }
        }
        if (!applyCostMap.isEmpty() && applyCostMap.containsKey(times)) {
            if (applyCostMap.get(times).compareTo(new BigDecimal(-1)) == 0) {
                cost = applyCostMap.get(times);
            }
        }
        if (ConfigManager.configManager.getBoolean("math.static-scale")) {
            return cost.setScale(scale, RoundingMode.HALF_UP);
        }
        return cost;
    }

    @Override
    public String getDisplayName(Player player, int multi, BigDecimal amount, boolean alwaysStatic) {
        if (empty) {
            return ConfigManager.configManager.getStringWithLang(player, "placeholder.price.empty");
        }
        if (singleSection == null) {
            return ConfigManager.configManager.getStringWithLang(player, "placeholder.price.unknown");
        }
        String tempVal1 = singleSection.getString("placeholder",
                ConfigManager.configManager.getStringWithLang(player, "placeholder.price.unknown"));
        String tempVal2;
        if (customPrice) {
            tempVal2 = ConfigManager.configManager.getStringWithLang(player, "prices." +
                    singleSection.getString("custom-type") + ".placeholder", tempVal1);
        } else {
            tempVal2 = singleSection.getString("placeholder", tempVal1);
        }
        if (!alwaysStatic && !tempVal2.contains("{status}") && !isStatic && baseAmount != null && !UltimateShop.freeVersion &&
                ConfigManager.configManager.getBoolean("placeholder.auto-settings.add-status-in-dynamic-price-placeholder.enabled")) {
            tempVal2 = tempVal2 + " " + StaticPlaceholder.getCompareValue(player, baseAmount.multiply(new BigDecimal(multi)), amount);
        }
        if (tempVal2.contains("{amount}") && ConfigManager.configManager.getBoolean("placeholder.auto-settings.change-amount-in-all-price-placeholder.enabled")) {
            tempVal2 = tempVal2.replace("{amount}", ConfigManager.configManager.getStringWithLang(player, "placeholder.auto-settings.change-amount-in-all-price-placeholder.replace-value", "{amount}"));
        }
        String itemName = "";
        if (tempVal2.contains("{display}")) {
            itemName = resolveDisplayPlaceholder(player);
        }
        return CommonUtil.modifyString(player, tempVal2,
                        "amount",
                        MathUtil.toDisplayString(amount),
                        "display",
                        itemName,
                        "status",
                        alwaysStatic || baseAmount == null ? "" : StaticPlaceholder.getCompareValue(player, baseAmount.multiply(new BigDecimal(multi)), amount));
    }

    private String resolveDisplayPlaceholder(Player player) {
        ConfigurationSection section = singleSection;
        if (customPrice) {
            ConfigurationSection customSection = ConfigManager.configManager.config.getConfigurationSection("prices." +
                    singleSection.getString("custom-type"));
            if (customSection != null) {
                section = customSection;
            }
        }
        switch (type) {
            case VANILLA_ITEM, HOOK_ITEM:
                GiveItemStack itemThing = getItemThing(section, player, 1, true);
                ItemStack displayItem = itemThing.getDisplayItem();
                if (displayItem == null) {
                    displayItem = itemThing.getTargetItem();
                }
                return ItemUtil.getItemName(displayItem);
            case VANILLA_ECONOMY:
                return section.getString("economy-type");
            case HOOK_ECONOMY:
                return section.getString("economy-plugin");
            default:
                return "";
        }
    }

    public int getStartApply() {
        if (isAlwaysApply || singleSection == null) {
            return -1;
        } else {
            return singleSection.getInt("start-apply", -1);
        }
    }

    public int getEndApply() {
        if (isAlwaysApply || singleSection == null) {
            return Integer.MAX_VALUE;
        } else {
            return singleSection.getInt("end-apply", Integer.MAX_VALUE);
        }
    }

    public boolean isAlwaysApply() {
        return isAlwaysApply;
    }

    public Map<Integer, BigDecimal> getApplyCostMap() {
        return applyCostMap;
    }

    public boolean getCustomPrice() {
        return customPrice;
    }

    public ObjectItem getItem() {
        return item;
    }

    @Override
    public String toString() {
        if (singleSection == null) {
            return "Empty Price";
        }
        return "Named Price: " + singleSection.getName();
    }

}
