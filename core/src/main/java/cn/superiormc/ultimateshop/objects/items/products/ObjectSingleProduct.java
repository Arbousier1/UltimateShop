package cn.superiormc.ultimateshop.objects.items.products;

import cn.superiormc.ultimateshop.managers.CacheManager;
import cn.superiormc.ultimateshop.managers.ConfigManager;
import cn.superiormc.ultimateshop.methods.StaticPlaceholder;
import cn.superiormc.ultimateshop.objects.buttons.ObjectItem;
import cn.superiormc.ultimateshop.objects.caches.ObjectCache;
import cn.superiormc.ultimateshop.objects.items.AbstractSingleThing;
import cn.superiormc.ultimateshop.utils.AmountVariableUtil;
import cn.superiormc.ultimateshop.utils.CommonUtil;
import cn.superiormc.ultimateshop.utils.MathUtil;
import cn.superiormc.ultimateshop.utils.TextUtil;
import org.bukkit.entity.Player;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static cn.superiormc.ultimateshop.utils.MathUtil.scale;

public class ObjectSingleProduct extends AbstractSingleThing {

    private ObjectItem item;

    private boolean isStatic;

    private BigDecimal baseAmount;

    public ObjectSingleProduct() {
        super();
    }

    public ObjectSingleProduct(String id, ObjectProducts products) {
        super(id, products);
        this.item = products.getItem();
        this.things = products;
        this.isStatic = singleSection.getString("amount", "1").matches("-?\\d+(\\.\\d+)?");
        Pattern pattern = Pattern.compile("-?\\d+(\\.\\d+)?");
        Matcher matcher = pattern.matcher(singleSection.getString("amount", "1"));
        if (matcher.find()) {
            this.baseAmount = new BigDecimal(matcher.group());
        }
        initApplyCondition();
        initRequireCondition();
        initAction();
    }

    @Override
    public String getDisplayName(Player player, int multi, BigDecimal amount, boolean alwaysStatic) {
        if (singleSection == null) {
            return ConfigManager.configManager.getStringWithLang(player, "placeholder.price.unknown");
        }
        String tempVal1 = singleSection.getString("placeholder",
                ConfigManager.configManager.getStringWithLang(player, "placeholder.price.unknown"));
        return CommonUtil.modifyString(player, tempVal1,
                "amount",
                MathUtil.toDisplayString(amount),
                "status",
                alwaysStatic ? "" : StaticPlaceholder.getCompareValue(player, baseAmount.multiply(new BigDecimal(multi)), amount));
    }

    public boolean isStatic() {
        return isStatic;
    }

    public BigDecimal getAmount(Player player, int offsetAmount, boolean buyOrSell) {
        String tempVal1 = singleSection.getString("amount", "1");
        BigDecimal cost;
        if (isStatic()) {
            cost = baseAmount;
        } else {
            if (item != null && ConfigManager.configManager.getBoolean("placeholder.data.can-used-in-amount")) {
                ObjectCache cache = CacheManager.cacheManager.getObjectCache(player);
                if (cache == null) {
                    return new BigDecimal(-1);
                }
                tempVal1 = AmountVariableUtil.replaceProductVariables(player, tempVal1, item, offsetAmount, buyOrSell, cache);
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
        if (ConfigManager.configManager.getBoolean("math.static-scale")) {
            return cost.setScale(scale, RoundingMode.HALF_UP);
        }
        return cost;
    }

}
