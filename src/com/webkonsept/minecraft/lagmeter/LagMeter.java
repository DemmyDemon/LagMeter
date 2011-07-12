package com.webkonsept.minecraft.lagmeter;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.logging.Logger;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginDescriptionFile;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.config.Configuration;

import com.nijiko.permissions.PermissionHandler;
import com.nijikokun.bukkit.Permissions.Permissions;

public class LagMeter extends JavaPlugin {
	private Logger log = Logger.getLogger("Minecraft");
	private PermissionHandler Permissions;
	private boolean usePermissions;
	private HashMap<String,Boolean> fallbackPermissions = new HashMap<String,Boolean>();
	protected float ticksPerSecond = 20;
	
	private LagMeterPoller poller = new LagMeterPoller(this);
	protected int averageLength = 10;
	protected LagMeterStack history = new LagMeterStack();
	
	//Configurable
	protected int interval = 40;
	protected boolean useAverage = true;
	
	@Override
	public void onDisable() {
		this.out("Disabled!");
		getServer().getScheduler().cancelTasks(this);
	}

	@Override
	public void onEnable() {
		loadConfig();
		history.setMaxSize(averageLength);
		if(!setupPermissions()){
			fallbackPermissions.put("lagmeter.command",true);
		}
		getServer().getScheduler().scheduleSyncRepeatingTask(this,poller,0,interval);
		this.out("Enabled!  Polling every "+interval+" server ticks.");
	}
	
	public boolean onCommand(CommandSender sender, Command command, String commandLabel, String[] args) {
		
		if ( ! this.isEnabled() ) return false;
		boolean success = false;
		if (command.getName().equalsIgnoreCase("lag")){
			success = true;
			if (sender instanceof Player){
				if (permit((Player)sender,"lagmeter.command")){
					sendLagMeter(sender);
				}
				else {
					sender.sendMessage(ChatColor.GOLD+"Sorry, permission was denied.");
				}
			}
			else {
				sendLagMeter(sender);
			}
		}
		return success;
	}
	protected void sendLagMeter(CommandSender sender){
		String lagMeter = "";
		float tps = 0f;
		if (useAverage){
			tps = history.getAverage();
		}
		else {
			tps = ticksPerSecond;
		}
		if (tps < 21){
			int looped = 0;
			while (looped++ < tps){
				lagMeter += "#";
			}
			lagMeter = String.format("%-20s",lagMeter); 
		}
		else {
			sender.sendMessage(ChatColor.GOLD+"LagMeter just loaded, please wait for polling.");
			return;
		}
		ChatColor color = ChatColor.GOLD;
		if (tps >= 20){
			color = ChatColor.GREEN;
		}
		else if (tps >= 18){
			color = ChatColor.GREEN;
		}
		else if (tps >= 15){
			color = ChatColor.YELLOW;
		}
		else {
			color = ChatColor.RED;
		}
		sender.sendMessage(ChatColor.GOLD+"["+color+lagMeter+ChatColor.GOLD+"] "+tps+" TPS");
	}
	public boolean permit(Player player,String permission){ 
		
		boolean allow = false; // Default to GTFO
		if ( usePermissions ){
			allow = Permissions.has(player,permission);
		}
		else if (player.isOp()){
			allow = true;
		}
		else {
			if (fallbackPermissions.get(permission) || false){
				allow = true;
			}
		}
		return allow;
	}
	private boolean setupPermissions() {
		Plugin test = this.getServer().getPluginManager().getPlugin("Permissions");
		if (this.Permissions == null){
			if (test != null){
				this.Permissions = ((Permissions)test).getHandler();
				this.usePermissions = true;
				return true;
			}
			else {
				this.out("Permissions plugin not found, defaulting to OPS CHECK mode");
				return false;
			}
		}
		else {
			this.out("Urr, this is odd...  Permissions are already set up!");
			return true;
		}
	}
	public void out(String message) {
		PluginDescriptionFile pdfFile = this.getDescription();
		log.info("[" + pdfFile.getName()+ " " + pdfFile.getVersion() + "] " + message);
	}
	public void crap(String message){
		PluginDescriptionFile pdfFile = this.getDescription();
		log.severe("[" + pdfFile.getName()+ " " + pdfFile.getVersion() + "] " + message);
	}
	public void loadConfig() {
		File configFile = new File(this.getDataFolder(),"settings.yml");
		File configDir = this.getDataFolder();
		Configuration config = new Configuration(configFile);
		
		config.load();
		
		// Loading
		interval = config.getInt("interval",interval);
		useAverage = config.getBoolean("useAverage",useAverage);
		averageLength = config.getInt("averageLength",averageLength);
		
		// Sanity check
		if (interval < 20){
			this.crap("An interval under 20 was configured.  This is very stupid.");
		}
		if (averageLength > 100){
			this.crap("You've specified an average of over 100 samples.  This will tell you roughly nothing!");
		}
		if (!configFile.exists()){
			if (!configDir.exists()){
				configDir.mkdir();
			}
			try {
				configFile.createNewFile();
			} catch (IOException e) {
				e.printStackTrace();
				this.crap("IOError while creating config file: "+e.getMessage());
			}
			config.save();
		}
	}
}
