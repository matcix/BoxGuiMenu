package net.matcix.boxGuiMenu;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;
import org.yaml.snakeyaml.Yaml;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Main extends JavaPlugin implements Listener {

    private File commandFile;
    private Map<String, String> commands;
    private int itemsPerPage = 26; // 每页显示的命令数

    @Override
    public void onEnable() {
        getCommand("menu").setExecutor((sender, command, label, args) -> {
            if (sender instanceof Player) {
                openMenu((Player) sender, 1); // 打开第1页
            }
            return true;
        });

        getServer().getPluginManager().registerEvents(this, this);
        initCommandFile();
    }

    private void initCommandFile() {
        commandFile = new File(getDataFolder(), "command.yml");
        if (!commandFile.exists()) {
            try {
                commandFile.getParentFile().mkdirs();
                commandFile.createNewFile();
                try (FileWriter writer = new FileWriter(commandFile)) {
                    writer.write("commands:\n  help: \"打开帮助菜单\"\n  example: \"示例命令\"");
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        loadCommands();
    }

    private void loadCommands() {
        Yaml yaml = new Yaml();
        try (InputStream inputStream = Files.newInputStream(commandFile.toPath())) {
            Map<String, Object> yamlData = yaml.load(new InputStreamReader(inputStream));
            commands = (Map<String, String>) yamlData.get("commands");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        player.sendMessage(ChatColor.GREEN + "输入/menu打开菜单");
    }

    private void openMenu(Player player, int page) {
        Inventory inventory = Bukkit.createInventory(null, 27, ChatColor.GREEN + "命令菜单 - 第 " + page + " 页");

        // 计算要显示的命令
        int startIndex = (page - 1) * itemsPerPage;
        List<Map.Entry<String, String>> commandList = new ArrayList<>(commands.entrySet());

        // 添加命令到GUI
        for (int i = startIndex; i < startIndex + itemsPerPage && i < commandList.size(); i++) {
            Map.Entry<String, String> entry = commandList.get(i);
            String command = entry.getKey();
            String description = entry.getValue();

            // 检查description是否为String类型
            if (description instanceof String) {
                inventory.setItem(i - startIndex, createCommandItem(command, description));
            } else {
                getLogger().warning("命令 " + command + " 的描述不是字符串，将跳过该命令。");
            }
        }

        // 添加翻页命令
        if (page > 1) {
            inventory.setItem(25, createCommandItem("上一页", "返回到上一页", page - 1)); // 显示“上一页”
        }
        if (startIndex + itemsPerPage < commandList.size()) {
            inventory.setItem(26, createCommandItem("下一页", "进入下一页", page + 1)); // 显示“下一页”
        }

        player.openInventory(inventory);
    }


    private ItemStack createCommandItem(String command, String description) {
        ItemStack item = new ItemStack(Material.PAPER);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ChatColor.YELLOW + command);
            List<String> lore = new ArrayList<>();
            lore.add(ChatColor.GRAY + description);
            lore.add(ChatColor.BLUE + "点击以执行");
            meta.setLore(lore);
            item.setItemMeta(meta);
        }
        return item;
    }

    private ItemStack createCommandItem(String command, String description, int page) {
        ItemStack item = createCommandItem(command, description);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            List<String> lore = new ArrayList<>();
            lore.add(ChatColor.GRAY + description);
            lore.add(ChatColor.BLUE + "点击以" + (command.equals("上一页") ? "返回" : "前往"));
            meta.setLore(lore);
            item.setItemMeta(meta);
        }
        return item;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (event.getView().getTitle().startsWith(ChatColor.GREEN + "命令菜单")) {
            event.setCancelled(true); // 防止玩家取出物品

            if (event.getCurrentItem() == null || !event.getCurrentItem().hasItemMeta()) return;

            String name = event.getCurrentItem().getItemMeta().getDisplayName();
            Player player = (Player) event.getWhoClicked();

            // 前一页
            if (name.equals(ChatColor.YELLOW + "上一页")) {
                openMenu(player, getCurrentPage(event.getView().getTitle()) - 1);
            }
            // 下一页
            else if (name.equals(ChatColor.YELLOW + "下一页")) {
                openMenu(player, getCurrentPage(event.getView().getTitle()) + 1);
            }
            // 执行命令
            else {
                String command = name.replace(ChatColor.YELLOW + "", ""); // 去除颜色代码
                player.performCommand(command);
                player.closeInventory(); // 执行命令后关闭GUI
            }
        }
    }

    private int getCurrentPage(String title) {
        // 通过匹配正则表达式提取页码
        String regex = "\\d+";
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(title);

        if (matcher.find()) {
            return Integer.parseInt(matcher.group());
        }

        // 默认返回1，如果未找到页码
        return 1;
    }

}
