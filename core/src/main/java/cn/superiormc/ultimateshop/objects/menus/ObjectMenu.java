package cn.superiormc.ultimateshop.objects.menus;

import cn.superiormc.ultimateshop.UltimateShop;
import cn.superiormc.ultimateshop.gui.inv.CommonGUI;
import cn.superiormc.ultimateshop.managers.ConfigManager;
import cn.superiormc.ultimateshop.managers.DynamicCommandManager;
import cn.superiormc.ultimateshop.managers.LanguageManager;
import cn.superiormc.ultimateshop.objects.ObjectShop;
import cn.superiormc.ultimateshop.objects.ObjectThingRun;
import cn.superiormc.ultimateshop.objects.buttons.AbstractButton;
import cn.superiormc.ultimateshop.objects.buttons.ObjectButton;
import cn.superiormc.ultimateshop.objects.buttons.ObjectItem;
import cn.superiormc.ultimateshop.objects.items.ObjectAction;
import cn.superiormc.ultimateshop.objects.items.ObjectCondition;
import cn.superiormc.ultimateshop.utils.CommonUtil;
import cn.superiormc.ultimateshop.utils.TextUtil;
import org.bukkit.command.CommandSender;
import org.bukkit.command.defaults.BukkitCommand;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.util.*;
import java.util.function.BiConsumer;

public class ObjectMenu {

    public MenuType type;

    public static Map<String, ObjectMenu> commonMenus = new HashMap<>();

    public static Collection<String> notCommonMenuNames = new HashSet<>();

    public String fileName;

    private final boolean hasMenuFile;

    private ObjectShop shop = null;

    private ObjectCondition condition;

    private ObjectAction openAction;

    private ObjectAction closeAction;

    public ConfigurationSection menuConfigs;

    protected final Map<Integer, AbstractButton> menuItems = new TreeMap<>();

    protected final Map<String, AbstractButton> buttonItems = new HashMap<>();

    private boolean useGeyser;

    private boolean useDialog;

    private boolean dynamicLayout;

    public ObjectMenu(String fileName, ObjectShop shop) {
        this.fileName = fileName;
        this.shop = shop;
        this.type = MenuType.Shop;
        this.hasMenuFile = true;
        initMenu();
        initButtons();
    }

    public ObjectMenu(ObjectShop shop) {
        this.fileName = shop.getShopName();
        this.shop = shop;
        this.type = MenuType.Shop;
        this.hasMenuFile = false;
        initMenu();
        initButtons();
    }

    public ObjectMenu(String fileName, ObjectItem item) {
        this.fileName = fileName;
        this.shop = item.getShopObject();
        this.type = MenuType.More;
        this.hasMenuFile = true;
        initMenu();
        initButtons();
    }

    public ObjectMenu(String fileName) {
        this.fileName = fileName;
        this.type = MenuType.Common;
        this.hasMenuFile = true;
        initMenu();
        initButtons();
        if (!UltimateShop.freeVersion) {
            initCustomCommand();
        }
    }

    public ObjectMenu(String fileName, ConfigurationSection menuConfigs) {
        this.fileName = fileName;
        this.type = MenuType.Common;
        this.hasMenuFile = false;
        this.menuConfigs = menuConfigs;
        initMenu();
        initButtons();
        if (!UltimateShop.freeVersion) {
            initCustomCommand();
        }
    }

    public MenuType getType() {
        return type;
    }

    private void applyShopMenuOverrides() {
        if (shop == null) {
            return;
        }

        ConfigurationSection overrideSection = shop.getConfig().getConfigurationSection("settings.menu-settings");
        if (overrideSection == null) {
            return;
        }

        if (menuConfigs == null) {
            menuConfigs = new YamlConfiguration();
        }

        mergeSection(menuConfigs, overrideSection);
    }

    private void mergeSection(ConfigurationSection target, ConfigurationSection source) {
        for (String key : source.getKeys(false)) {
            Object value = source.get(key);
            if (value instanceof ConfigurationSection sourceSection) {
                ConfigurationSection targetSection = target.getConfigurationSection(key);
                if (targetSection == null) {
                    targetSection = target.createSection(key);
                }
                mergeSection(targetSection, sourceSection);
            } else {
                target.set(key, value);
            }
        }
    }

    public void initMenu() {
        if (type == MenuType.Common) {
            commonMenus.put(fileName, this);
        } else if (fileName != null && !fileName.isEmpty()) {
            notCommonMenuNames.add(fileName);
        }

        if (hasMenuFile) {
            File file = new File(UltimateShop.instance.getDataFolder() + "/menus/" + fileName + ".yml");
            if (!file.exists()){
                TextUtil.sendMessage(null, TextUtil.pluginPrefix() + " §cError: We can not found your menu file: " +
                        fileName + ".yml!");
            } else {
                if (type == MenuType.Common) {
                    TextUtil.sendMessage(null, TextUtil.pluginPrefix() + " §fLoaded menu: " + fileName + ".yml!");
                }
                this.menuConfigs = YamlConfiguration.loadConfiguration(file);
            }
        }
        applyShopMenuOverrides();
        if (menuConfigs == null) {
            this.condition = new ObjectCondition();
            this.openAction = new ObjectAction();
            this.closeAction = new ObjectAction();
            this.useGeyser = true;
            this.useDialog = false;
            return;
        } else if (shop != null) {
            this.condition = new ObjectCondition(menuConfigs.getConfigurationSection("conditions"));
            this.openAction = new ObjectAction(menuConfigs.getConfigurationSection("open-actions"), shop);
            this.closeAction = new ObjectAction(menuConfigs.getConfigurationSection("close-actions"));
            this.useGeyser = true;
        } else {
            this.condition = new ObjectCondition(menuConfigs.getConfigurationSection("conditions"), shop);
            this.openAction = new ObjectAction(menuConfigs.getConfigurationSection("open-actions"));
            this.closeAction = new ObjectAction(menuConfigs.getConfigurationSection("close-actions"));
            this.useGeyser = menuConfigs.getBoolean("bedrock.enabled", true);
        }
        if (UltimateShop.freeVersion || !UltimateShop.methodUtil.methodID().equals("paper") || !CommonUtil.getMinorVersion(21, 9) ||
                !ConfigManager.configManager.getBoolean("menu.dialog.enabled")) {
            this.useDialog = false;
        } else {
            this.useDialog = menuConfigs.getBoolean("dialog.enabled", true);
        }
        this.dynamicLayout = menuConfigs.getBoolean("dynamic-layout", false) && !UltimateShop.freeVersion;
    }

    private void buildShopItems(MenuSender menuSender, Map<Integer, AbstractButton> target) {
        if (menuConfigs == null) {
            return;
        }

        parseLayout(menuConfigs.getStringList("layout"), (slot, rawId) -> {
            String id = rawId;
            if (!menuSender.isStatic()) {
                id = TextUtil.withPAPI(id, menuSender.getPlayer());
            }

            AbstractButton button = getButtonByLayoutId(id, menuSender, true);
            if (button != null) {
                target.put(slot, button);
            }
        });
    }

    public void initButtons() {
        if (menuConfigs == null) {
            return;
        }

        ConfigurationSection tempVal1 = menuConfigs.getConfigurationSection("buttons");
        if (tempVal1 != null) {
            for (String button : tempVal1.getKeys(false)) {
                if (shop == null) {
                    buttonItems.put(button, new ObjectButton(tempVal1.getConfigurationSection(button)));
                } else {
                    buttonItems.put(button, new ObjectButton(tempVal1.getConfigurationSection(button), shop));
                }
            }
        }

        if (!dynamicLayout) {
            if (type == MenuType.Shop) {
                buildShopItems(MenuSender.empty, menuItems);
            } else {
                buildButtonItems(MenuSender.empty, menuItems);
            }
        }
    }

    private void buildButtonItems(MenuSender menuSender, Map<Integer, AbstractButton> target) {
        parseLayout(menuConfigs.getStringList("layout"), (slot, rawId) -> {
            String id = rawId;
            if (!menuSender.isStatic()) {
                id = TextUtil.withPAPI(id, menuSender.getPlayer());
            }

            AbstractButton buttonObj = getButtonByLayoutId(id, menuSender, false);
            if (buttonObj != null) {
                target.putIfAbsent(slot, buttonObj);
            }
        });
    }



    private AbstractButton getButtonByLayoutId(String id, MenuSender menuSender, boolean includeShopItems) {
        if (id == null || id.isEmpty()) {
            return null;
        }

        if (!id.contains("||")) {
            return getSingleButtonById(id, menuSender, includeShopItems);
        }

        String[] candidates = id.split("\\|\\|");
        for (String candidate : candidates) {
            AbstractButton button = getSingleButtonById(candidate.trim(), menuSender, includeShopItems);
            if (button != null) {
                return button;
            }
        }
        return null;
    }

    private AbstractButton getSingleButtonById(String id, MenuSender menuSender, boolean includeShopItems) {
        if (id == null || id.isEmpty()) {
            return null;
        }

        if (includeShopItems && shop != null) {
            if (!UltimateShop.freeVersion) {
                AbstractButton copyItem = shop.getCopyItem(id);
                if (copyItem != null && copyItem.canDisplay(menuSender)) {
                    return copyItem;
                }
            }

            AbstractButton button = shop.getButton(id);
            if (button != null && button.canDisplay(menuSender)) {
                return button;
            }

            AbstractButton product = shop.getProduct(id);
            if (product != null && product.canDisplay(menuSender)) {
                return product;
            }
        }

        AbstractButton buttonObj = buttonItems.get(id);
        if (buttonObj != null && buttonObj.canDisplay(menuSender)) {
            return buttonObj;
        }
        return null;
    }

    private void initCustomCommand() {
        String commandName = menuConfigs.getString("custom-command.name");
        if (commandName != null && !commandName.isEmpty()) {
            BukkitCommand command = createCustomCommand(commandName, fileName);
            command.setDescription(getString("custom-command.description", "UltimateShop Custom Command for " + commandName));
            DynamicCommandManager.register(command);
            TextUtil.sendMessage(null, TextUtil.pluginPrefix() + " §cRegistered custom command for menu: " + fileName + ".");
        }
    }

    private static BukkitCommand createCustomCommand(String commandName, String menuId) {
        return new BukkitCommand(commandName) {
            @Override
            public boolean execute(CommandSender sender, String label, String[] args) {
                if (!(sender instanceof Player player)) {
                    LanguageManager.languageManager.sendStringText("error.in-game");
                    return true;
                }
                CommonGUI.openGUI(player, menuId, false, false);
                return true;
            }
        };
    }

    public String getString(String path, String defaultValue) {
        if (defaultValue == null) {
            return menuConfigs.getString(path);
        }
        return menuConfigs.getString(path, defaultValue);
    }

    public int getInt(String path, int defaultValue) {
        return menuConfigs.getInt(path, defaultValue);
    }

    public Map<Integer, AbstractButton> getMenu(MenuSender menuSender) {
        if (!dynamicLayout) {
            return new TreeMap<>(menuItems);
        }
        MenuSender effectiveSender = menuSender == null ? MenuSender.empty : menuSender;
        Map<Integer, AbstractButton> result = new TreeMap<>(menuItems);
        if (type == MenuType.Shop) {
            buildShopItems(effectiveSender, result);
        } else {
            buildButtonItems(effectiveSender, result);
        }
        return result;
    }

    public ObjectCondition getCondition() {
        return condition;
    }

    public void doOpenAction(Player player, boolean reopen) {
        if (openAction != null) {
            openAction.runAllActions(new ObjectThingRun(player, reopen));
        }
    }

    public void doCloseAction(Player player) {
        if (closeAction != null) {
            closeAction.runAllActions(new ObjectThingRun(player));
        }
    }

    public String getName() {
        return fileName;
    }

    public ConfigurationSection getConfig() {
        return menuConfigs;
    }

    public boolean isUseGeyser() {
        return useGeyser;
    }

    public boolean isUseDialog() {
        return useDialog;
    }

    public boolean isDynamicLayout() {
        return dynamicLayout;
    }

    protected Map<Integer, AbstractButton> getButtons() {
        return menuItems;
    }

    protected void parseLayout(List<String> layout, BiConsumer<Integer, String> itemHandler) {
        int slot = 0;
        for (String singleLine : layout) {
            int c = 0;
            while (c < singleLine.length()) {
                String id;
                if (singleLine.charAt(c) == '`') {
                    int end = singleLine.indexOf('`', c + 1);
                    if (end == -1) {
                        id = String.valueOf(singleLine.charAt(c));
                        c++;
                    } else {
                        id = singleLine.substring(c + 1, end);
                        c = end + 1;
                    }
                } else {
                    id = String.valueOf(singleLine.charAt(c));
                    c++;
                }

                itemHandler.accept(slot, id);
                slot++;
            }
        }
    }

}
