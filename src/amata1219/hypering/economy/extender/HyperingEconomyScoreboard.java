package amata1219.hypering.economy.extender;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.Map;

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

				if(System.currentTimeMillis() - map.keySet().stream().max(Comparator.reverseOrder()).get() > 5000)
					sboard.clearSlot(DisplaySlot.SIDEBAR);
			}
		}.runTaskTimer(HyperingEconomyExtender.getPlugin(), 0, 6000);
	}

	public void display(){
		Scoreboard sboard = player.getScoreboard();
		if(sboard != null && sboard != board){
			if(sboard.getObjectives().stream().filter(obj -> obj.getDisplaySlot() == DisplaySlot.SIDEBAR).count()> 0)
				return;
		}

		update();
		player.setScoreboard(board);
	}

	public void update(){
		board.getEntries().forEach(entry -> board.resetScores(entry));

		set(ChatColor.RED + "所持金: " + SQL.getSQL().getMoney(player.getUniqueId()), 14);
		set(ChatColor.RED + "増加量: + ¥" + increasePerSecond() + "/s", 13);
		set("", 12);

		Int i = new Int(12 - map.size());

		map.forEach((k, v) -> set(ChatColor.GRAY + "+" + v, i.increaseAndGet()));

		objective.setDisplaySlot(DisplaySlot.SIDEBAR);
	}

	private void set(String key, int value){
		objective.getScore(key).setScore(value);
	}

	class Int {

		private int i;

		public Int(int i){
			this.i = i;
		}

		public int increaseAndGet(){
			i++;
			return i;
		}

	}

	public void add(int money){
		if(map.size() >= 12)
			map.remove(map.keySet().stream().min(Comparator.naturalOrder()).get().longValue());

		map.put(System.currentTimeMillis(), money);
	}

	public long increasePerSecond(){
		if(map.isEmpty())
			return 0L;

		return map.keySet().stream().reduce(0L, Long::sum) / map.values().stream().reduce(0, Integer::sum);
	}

	public void unload(){
		task.cancel();
	}

}
