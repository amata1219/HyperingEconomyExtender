package amata1219.hypering.economy.extender;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.Map;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
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

	public HyperingEconomyScoreboard(final Player player){
		this.player = player;
		this.board = Bukkit.getScoreboardManager().getNewScoreboard();
		this.objective = this.board.registerNewObjective("HEScoreboard", "dummy", ChatColor.AQUA + "稼ぎ人ボード");
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

}
