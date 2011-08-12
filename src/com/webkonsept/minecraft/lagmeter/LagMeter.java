package com.webkonsept.minecraft.lagmeter;

import java.io.File;
import java.io.IOException;
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
	protected float ticksPerSecond = 20;
	
	protected LagMeterLogger logger = new LagMeterLogger(this);
	protected LagMeterPoller poller = new LagMeterPoller(this);
	protected int averageLength = 10;
	protected LagMeterStack history = new LagMeterStack();
	
	protected boolean crapPermissions = false;
	protected PermissionHandler pHandler;
	
	double memUsed = ( Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory() ) / 1048576;
	double memMax = Runtime.getRuntime().maxMemory() / 1048576;
	double memFree = memMax - memUsed;
	double percentageFree = ( 100 / memMax) * memFree;
	
	//Configurable
	protected int interval = 40;
	protected boolean useAverage = true;
	protected boolean enableLogging = true;
	protected int logInterval = 150;
	
	@Override
	public void onDisable() {
		this.out("Disabled!");
		getServer().getScheduler().cancelTasks(this);
		logger.disable();
	}

	@Override
	public void onEnable() {
		loadConfig();
		if (enableLogging){
			if (!logger.enable()){
				this.crap("Logging is disabled because: "+logger.getError());
				poller.setLogInterval(logInterval);
			}
		}
		history.setMaxSize(averageLength);
		getServer().getScheduler().scheduleSyncRepeatingTask(this,poller,0,interval);
		if(checkCrapPermissions()){
			this.crap("Old permissions system detected.  Using it.");
			crapPermissions = true;
		}
		String loggingMessage = "";
		if (enableLogging){
			loggingMessage = "  Logging to "+logger.getFilename();
		}
		this.out("Enabled!  Polling every "+interval+" server ticks."+loggingMessage);
	}
	protected boolean permit(Player player,String permission){
		boolean permit = false;
		if (crapPermissions){
			permit = pHandler.permission(player, permission);
		}
		else {
			permit = player.hasPermission(permission);
		}
		return permit;
	}
	private boolean checkCrapPermissions() {
		boolean crap = false;
		
		Plugin test = this.getServer().getPluginManager().getPlugin("Permissions");
		if (test != null){
			crap = true;
			this.pHandler = ((Permissions)test).getHandler();
		}
		
		return crap;
	}
	public boolean onCommand(CommandSender sender, Command command, String commandLabel, String[] args) {
		
		if ( ! this.isEnabled() ) return false;
		boolean success = false;
		if (
				(sender instanceof Player && this.permit((Player)sender, "lagmeter.command."+command.getName().toLowerCase()))
				|| !(sender instanceof Player)
				// That is, if it's a player with permission, or not a player at all (console?), then...
		){
			if (command.getName().equalsIgnoreCase("lag")){
				success = true;
				sendLagMeter(sender);
			}
			else if (command.getName().equalsIgnoreCase("mem")){
				success = true; 			
				sendMemMeter(sender);
			}
			else if (command.getName().equalsIgnoreCase("lagmem")){
				success = true;
				sendLagMeter(sender);
				sendMemMeter(sender);
			}
		}
		else {
			success = true;  // Not really a success, but a valid command at least.
			sender.sendMessage(ChatColor.GOLD+"Sorry, permission lagmeter.command."+command.getName().toLowerCase()+" was denied.");
		}
		
		return success;
	}
	protected void updateMemoryStats (){
		memUsed = ( Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory() ) / 1048576;
		memMax = Runtime.getRuntime().maxMemory() / 1048576;
		memFree = memMax - memUsed;
		percentageFree = ( 100 / memMax) * memFree;
	}
	protected void sendMemMeter(CommandSender sender){
		updateMemoryStats();
		ChatColor wrapColor = ChatColor.WHITE;
		if (sender instanceof Player){
			wrapColor = ChatColor.GOLD;
		}
		
		ChatColor color = ChatColor.GOLD;
		if (percentageFree >= 60){
			color = ChatColor.GREEN;
		}
		else if (percentageFree >= 35){
			color = ChatColor.YELLOW;
		}
		else {
			color = ChatColor.RED;
		}
		
		String bar = "";
		int looped = 0;
		
		while (looped++ < (percentageFree/5) ){
			bar += '#';
		}
		//bar = String.format("%-20s",bar);
		bar += ChatColor.WHITE;
		while (looped++ <= 20){
			bar += '_';
		}
		sender.sendMessage(wrapColor+"["+color+bar+wrapColor+"] "+memFree+"MB/"+memMax+"MB ("+(int)percentageFree+"%) free");
	}
	protected void sendLagMeter(CommandSender sender){
		
		ChatColor wrapColor = ChatColor.WHITE;
		if (sender instanceof Player){
			wrapColor = ChatColor.GOLD;
		}
		
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
			//lagMeter = String.format("%-20s",lagMeter);
			lagMeter += ChatColor.WHITE;
			while (looped++ <= 20){
				lagMeter += "_";
			}
		}
		else {
			sender.sendMessage(wrapColor+"LagMeter just loaded, please wait for polling.");
			return;
		}
		ChatColor color = wrapColor;
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
		sender.sendMessage(wrapColor+"["+color+lagMeter+wrapColor+"] "+tps+" TPS");
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
		interval 		= config.getInt		("interval",		interval);
		useAverage 		= config.getBoolean	("useAverage",		useAverage);
		averageLength 	= config.getInt		("averageLength",	averageLength);
		enableLogging 	= config.getBoolean	("log.enable",		enableLogging);
		logInterval		= config.getInt		("log.interval",	logInterval);
		
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
		}
		config.save();  // Saving regardless, because I want new settings to be written to it after updating etc.
	}
}
