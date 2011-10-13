package com.webkonsept.minecraft.lagmeter;

public class LagMeterPoller implements Runnable {
	long lastPoll = System.currentTimeMillis() - 3000;
	long polls = 0; // Haha, a Long here is optimism for sure!
	int logInterval = 150;
	LagMeter plugin;
	private JsonApiStream stream;
	private int streamFrequency;
	private int streamIndex = 0;
	
	LagMeterPoller (LagMeter instance){
		plugin = instance;
	}
	LagMeterPoller (LagMeter instance, int logInterval){
		this.logInterval = logInterval;
		plugin = instance;
	}
	
	public void setLogInterval(int interval){
		logInterval = interval;
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
		polls++;
		
		if (plugin.logger.enabled() && polls % logInterval == 0){
			plugin.updateMemoryStats();
			float aTPS = plugin.history.getAverage();
			plugin.logger.log("TPS: "+aTPS+"  Memory free: "+plugin.memFree+"/"+plugin.memMax);
		}
		
		if (stream != null) {
			if (streamIndex <= 0) {
				stream.addMessage(tps, plugin.memMax, plugin.memMax - plugin.memFree);
				streamIndex = streamFrequency;
			} else {
				streamIndex--;
			}
		}
	}
	/**
	 * Frequency of broadcasting will be pollInterval * frequency.
	 */
	public void enableJsonApi(JsonApiStream jsonApiStream, int frequency) {
		streamFrequency = frequency;
		stream = jsonApiStream;
	}

}
