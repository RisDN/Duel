package net.multylands.duels.gui.listeners;

import net.multylands.duels.Duels;
import net.multylands.duels.gui.ArenaInventoryHolder;
import net.multylands.duels.gui.GUIManager;
import net.multylands.duels.object.Arena;
import net.multylands.duels.object.DuelRequest;
import net.multylands.duels.utils.Chat;
import net.multylands.duels.utils.PersistentDataManager;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

public class ArenaGUIListener implements Listener {
    Duels plugin;

    public ArenaGUIListener(Duels plugin) {
        this.plugin = plugin;
    }

    public static List<String> lore = new ArrayList<>();

    @EventHandler
    public void onGuiClose(InventoryCloseEvent event) {
        Inventory inv = event.getInventory();
        if (inv.getLocation() != null) {
            return;
        }
        Player player = (Player) event.getPlayer();
        UUID playerUUID = player.getUniqueId();
        if (!(inv == Duels.manager.arenaInventories.get(playerUUID))) {
            return;
        }
        DuelRequest request = Duels.manager.inventoryRequests.get(playerUUID);
        Player target = Bukkit.getPlayer(request.getOpponent(player.getUniqueId()));
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            Duels.manager.openDuelInventory(player, target, request.getGame().getBet(), request.getGame().getRestrictions());
        }, 1L);
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        ItemStack item = event.getCurrentItem();
        Inventory inv = event.getInventory();
        int slot = event.getSlot();
        if (inv.getLocation() != null || !(inv.getHolder() instanceof ArenaInventoryHolder) || item == null) {
            return;
        }
        Player player = (Player) event.getWhoClicked();
        UUID playerUUID = player.getUniqueId();
        event.setCancelled(true);
        //always!!! get this request from the GUI clicker. because we are storing only sender: request in the requests map.
        DuelRequest request = Duels.manager.inventoryRequests.get(playerUUID);
        Player target = Bukkit.getPlayer(request.getOpponent(player.getUniqueId()));
        if (target == null) {
            Chat.sendMessage(player, plugin.languageConfig.getString("duel.target-is-offline"));
            request.removeStoreRequest(false);
            player.closeInventory();
            return;
        }

        if (slot == 53) {
            inv.close();
        } else {

            String not_selected = Chat.color(plugin.languageConfig.getString("arena-GUI.not-selected"));
            String selected = Chat.color(plugin.languageConfig.getString("arena-GUI.selected"));

            String arenaName = PersistentDataManager.getArenaName(plugin, item);
            Arena arena = Duels.Arenas.get(arenaName);
            ItemMeta meta = item.getItemMeta();

            if (!arena.isAvailable()) {
                Chat.sendMessage(player, plugin.languageConfig.getString("duel.arena-not-available"));
                return;
            }

            if (Duels.manager.selectedArenas.get(player.getUniqueId()) == null || Duels.manager.selectedArenas.get(player.getUniqueId()).getID() != arenaName) {
                for (String loreLine : plugin.languageConfig.getStringList("arena-GUI.format.lore")) {
                    lore.add(Chat.color(loreLine.replace("%status%", selected)));
                }
                Duels.manager.selectedArenas.put(player.getUniqueId(), arena);
                updateSelected(plugin, inv, arenaName);
            } else {
                for (String loreLine : plugin.languageConfig.getStringList("arena-GUI.format.lore")) {
                    lore.add(Chat.color(loreLine.replace("%status%", not_selected)));
                }
                Duels.manager.selectedArenas.put(player.getUniqueId(), null);
            }
            meta.setLore(lore);
            item.setItemMeta(meta);
            lore.clear();
        }
    }
    public static void updateSelected(Duels plugin, Inventory inventory, String arenaName) {
        for (ItemStack item : inventory.getContents()) {
            if (item == null || item.getType() == Material.AIR) {
                continue;
            }
            String itemsArenaName = PersistentDataManager.getArenaName(plugin, item);
            if (itemsArenaName == null || itemsArenaName.equals(arenaName)) {
                continue;
            }
            String not_selected = Chat.color(plugin.languageConfig.getString("arena-GUI.not-selected"));
            ItemMeta itemMeta = item.getItemMeta();
            for (String line : plugin.languageConfig.getStringList("arena-GUI.format.lore")) {
                ArenaInventoryHolder.lore.add(Chat.color(line.replace("%status%", not_selected)));
            }
            itemMeta.setLore(ArenaInventoryHolder.lore);
            ArenaInventoryHolder.lore.clear();
            item.setItemMeta(itemMeta);
        }
    }
}
