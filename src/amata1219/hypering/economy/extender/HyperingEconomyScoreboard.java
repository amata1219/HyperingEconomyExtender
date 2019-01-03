package amata1219.hypering.economy.extender;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Scoreboard;

import amata1219.hypering.economy.SQL;

public class HyperingEconomyScoreboard {

	private final Player player;

	private final Scoreboard board;
	private final Objective objective;

	private final Map<Long, Integer> map = new LinkedHashMap<>(12);
	//ms, money

	private final BukkitTask task;

	public HyperingEconomyScoreboard(final Player player, final String title){
		this.player = player;
		this.board = Bukkit.getScoreboardManager().getNewScoreboard();
		this.objective = this.board.registerNewObjective("HEScoreboard", "dummy", title);

		task = new BukkitRunnable(){
			@Override
			public void run(){
				Scoreboard sboard = player.getScoreboard();
				if(sboard != board)
					return;

				if(map.isEmpty())
					return;

				if(System.currentTimeMillis() - map.keySet().stream().max(Comparator.naturalOrder()).get() > 5000){
					map.clear();
					sboard.clearSlot(DisplaySlot.SIDEBAR);
				}
			}
		}.runTaskTimer(HyperingEconomyExtender.getPlugin(), 0, 150);
	}

	public void display(){
		Scoreboard sboard = player.getScoreboard();
		if(sboard != null && sboard != board){
			if(sboard.getObjectives().stream().filter(obj -> obj.getDisplaySlot() == DisplaySlot.SIDEBAR).count() > 0)
				return;
		}

		update();
		player.setScoreboard(board);
	}

	public void update(){
		board.getEntries().forEach(entry -> board.resetScores(entry));

		set(ChatColor.RED + "所持金: ¥" + SQL.getSQL().getMoney(player.getUniqueId()), 15);
		set(ChatColor.RED + "増加量: + ¥" + increasePerSecond() + "/s", 14);
		set("", 13);

		AtomicInteger i = new AtomicInteger(12);

		map.forEach((k, v) -> set(ChatColor.GRAY + "+" + v + getSpace(i.get()), i.getAndDecrement()));

		objective.setDisplaySlot(DisplaySlot.SIDEBAR);
	}

	private void set(String key, int value){
		objective.getScore(key).setScore(value);
	}

	private final StringBuilder builder = new StringBuilder();

	private String getSpace(int length){
		builder.setLength(0);

		for(int i = 0; i < length; i++)
			builder.append(" ");

		return builder.toString();
	}

	public void add(int money){
		if(map.size() >= 12)
			map.remove(map.keySet().stream().min(Comparator.naturalOrder()).get().longValue());

		map.put(System.currentTimeMillis(), money);
	}

	public int increasePerSecond(){
		if(map.isEmpty())
			return 0;

		if(map.size() == 1)
			return map.values().stream().max(Comparator.naturalOrder()).get().intValue();

		return Double.valueOf(map.values().stream().mapToInt(i -> i.intValue()).sum() / (Long.valueOf(map.keySet().stream().max(Comparator.naturalOrder()).get().longValue() - map.keySet().stream().min(Comparator.naturalOrder()).get().longValue()).intValue() / 1000D)).intValue();
	}

	public void unload(){
		task.cancel();
	}

}
