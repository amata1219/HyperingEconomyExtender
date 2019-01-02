package amata1219.hypering.economy.extender;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.CreatureSpawnEvent.SpawnReason;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.plugin.java.JavaPlugin;

import com.gmail.nossr50.mcMMO;

import amata1219.hypering.economy.HyperingEconomyAPI;
import amata1219.hypering.economy.SQL;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;

public class HyperingEconomyExtender extends JavaPlugin implements Listener {

	private static  HyperingEconomyExtender plugin;

	private HyperingEconomyAPI api;

	private final Set<String> worlds = new HashSet<>();
	private final Map<Material, Integer> blocks = new HashMap<>();

	private final Map<UUID, HyperingEconomyScoreboard> seichi = new HashMap<>();

	@Override
	public void onEnable(){
		plugin = this;

		saveDefaultConfig();

		worlds.addAll(getConfig().getStringList("Worlds"));

		getConfig().getConfigurationSection("Blocks").getKeys(false).forEach(key -> blocks.put(Material.valueOf(key), getConfig().getInt("Blocks." + key)));

		getServer().getOnlinePlayers().forEach(player -> register(player));

		getServer().getPluginManager().registerEvents(this, this);

		api = SQL.getSQL().getHyperingEconomyAPI();
	}

	@Override
	public void onDisable(){
		HandlerList.unregisterAll((JavaPlugin) this);

		getServer().getOnlinePlayers().forEach(player -> unregister(player));
	}

	public static HyperingEconomyExtender getPlugin(){
		return plugin;
	}

	public void register(final Player player){
		seichi.put(player.getUniqueId(), new HyperingEconomyScoreboard(player, ChatColor.AQUA + "掘削ボード"));
	}

	public void unregister(final Player player){
		UUID uuid = player.getUniqueId();
		seichi.get(uuid).unload();
		seichi.remove(uuid);
	}

	@EventHandler
	public void onJoin(PlayerJoinEvent e){
		register(e.getPlayer());
	}

	@EventHandler
	public void onQuit(PlayerQuitEvent e){
		unregister(e.getPlayer());
	}

	@EventHandler(priority = EventPriority.HIGH)
	public void onBreak(BlockBreakEvent e){
		if(e.isCancelled())
			return;

		Player player = e.getPlayer();
		if(player.getGameMode() != GameMode.SURVIVAL)
			return;

		if(!worlds.contains(e.getPlayer().getWorld().getName()))
			return;

		Block block = e.getBlock();
		Material material = block.getType();
		if(!blocks.containsKey(material))
			return;

		if(mcMMO.getPlaceStore().isTrue(block))
			return;

		UUID uuid = player.getUniqueId();
		int money = blocks.get(material);

		api.addMoney(uuid, money);

		HyperingEconomyScoreboard board = seichi.get(uuid);
		board.add(money);
		board.display();
	}

	@EventHandler
	public void onKill(EntityDamageByEntityEvent e){
		if(e.isCancelled())
			return;

		Entity entity = e.getEntity();
		if(!(entity instanceof LivingEntity))
			return;

		Entity damager = e.getDamager();
		if(!(damager instanceof Player))
			return;

		if(((LivingEntity) entity).getHealth() > e.getDamage())
			return;

		if(entity.hasMetadata("HyperingEconomy:MobKill"))
			return;

		Player player = ((Player) damager);
		api.addMoney(player.getUniqueId(), 300L);
		player.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(ChatColor.GRAY + "+ ¥300"));
	}

	private final List<EntityType> enemies = Collections.unmodifiableList(new ArrayList<>(Arrays.asList(EntityType.ZOMBIE
			, EntityType.SKELETON, EntityType.CREEPER, EntityType.WITCH, EntityType.ENDERMAN, EntityType.HUSK, EntityType.STRAY
			, EntityType.PIG_ZOMBIE, EntityType.SPIDER, EntityType.BLAZE, EntityType.WITHER_SKELETON, EntityType.SQUID
			, EntityType.GUARDIAN, EntityType.MAGMA_CUBE, EntityType.SLIME)));

	@EventHandler
	public void onSpawn(CreatureSpawnEvent e){
		if(e.isCancelled())
			return;

		LivingEntity entity = e.getEntity();
		SpawnReason reason = e.getSpawnReason();
		if(reason == SpawnReason.SPAWNER){
			applyMeta(entity);
			return;
		}

		FileConfiguration config = getConfig();
		int count = 0;

		switch(entity.getType()){
		case ZOMBIE:
		case SKELETON:
		case HUSK:
		case STRAY:
		case CREEPER:
		case SPIDER:
		case WITCH:
		case WITHER_SKELETON:
		case SQUID:
		case BLAZE:
		case MAGMA_CUBE:
		case ZOMBIE_VILLAGER:
		case BAT:
		case ENDERMITE:
			for(Entity ent : entity.getNearbyEntities(16, 16, 16)){
				if(enemies.contains(ent.getType()))
					count++;
			}

			if(count > config.getInt("UpperLimit.Normal"))
				applyMeta(entity);
			return;
		case SLIME:
			if(entity.getLocation().getBlockY() > 40)
				return;

			for(Entity ent : entity.getNearbyEntities(12, 48, 12)){
				if(ent.getType() == EntityType.SLIME)
					count++;
			}

			if(count > config.getInt("UpperLimit.Slime"))
				applyMeta(entity);
			return;
		case GUARDIAN:
			for(Entity ent : entity.getNearbyEntities(38, 38, 38)){
				if(ent.getType() == EntityType.GUARDIAN)
					count++;
			}

			if(count > config.getInt("UpperLimit.Guardian"))
				applyMeta(entity);
			return;
		case PIG_ZOMBIE:
			if(reason == SpawnReason.NETHER_PORTAL){
				for(Entity ent : entity.getNearbyEntities(32, 1, 32)){
					if(ent.getType() == EntityType.PIG_ZOMBIE)
						count++;
				}

				if(count > config.getInt("UpperLimit.Pigman"))
					applyMeta(entity);
				return;
			}

			for(Entity ent : entity.getNearbyEntities(16, 16, 16)){
				if(enemies.contains(ent.getType()))
					count++;
			}

			if(count > config.getInt("UpperLimit.Normal"))
				applyMeta(entity);
			return;
		default:
			return;
		}
	}

	private void applyMeta(LivingEntity entity){
		entity.setMetadata("HyperingEconomy:MobKill", new FixedMetadataValue(this, this));
	}

}
