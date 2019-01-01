package amata1219.hypering.economy.extender;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.plugin.java.JavaPlugin;

import com.gmail.nossr50.mcMMO;

import amata1219.hypering.economy.HyperingEconomyAPI;
import amata1219.hypering.economy.SQL;

public class HyperingEconomyExtender extends JavaPlugin implements Listener {

	private HyperingEconomyAPI api;

	private final Set<String> worlds = new HashSet<>();
	private final Map<Material, Integer> blocks = new HashMap<>();

	@Override
	public void onEnable(){
		saveDefaultConfig();

		worlds.addAll(getConfig().getStringList("Worlds"));

		getConfig().getConfigurationSection("Blocks").getKeys(false).forEach(key -> blocks.put(Material.valueOf(key), getConfig().getInt("Blocks." + key)));

		getServer().getPluginManager().registerEvents(this, this);

		api = SQL.getSQL().getHyperingEconomyAPI();
	}

	@Override
	public void onDisable(){
		HandlerList.unregisterAll((JavaPlugin) this);
	}

	@EventHandler(priority = EventPriority.HIGH)
	public void onBreak(BlockBreakEvent e){
		if(e.isCancelled())
			return;

		Player player = e.getPlayer();
		if(player.getGameMode() != GameMode.SURVIVAL)
			return;

		Block block = e.getBlock();
		Material material = block.getType();
		if(!blocks.containsKey(material))
			return;

		if(mcMMO.getPlaceStore().isTrue(block))
			return;

		api.addMoney(player.getUniqueId(), blocks.get(material));
	}

}
