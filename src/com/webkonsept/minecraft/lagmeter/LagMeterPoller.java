package com.webkonsept.minecraft.lagmeter;

public class LagMeterPoller implements Runnable {
	long lastPoll = System.currentTimeMillis() - 3000;
	LagMeter plugin;
	
	LagMeterPoller (LagMeter instance){
		plugin = instance;
	}
	
	@Override
	public void run() {
		long now = System.currentTimeMillis();
		long timeSpent = (now - lastPoll) / 1000;
		if (timeSpent == 0){
			timeSpent = 1;
		}
		float tps = plugin.interval / timeSpent;
		plugin.ticksPerSecond = tps;
		plugin.history.add(tps);
		lastPoll = now;
	}

}
