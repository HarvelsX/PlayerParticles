package com.esophose.playerparticles.styles;

import org.bukkit.Bukkit;
import org.bukkit.event.Listener;

import com.esophose.playerparticles.PlayerParticles;
import com.esophose.playerparticles.styles.api.ParticleStyle;
import com.esophose.playerparticles.styles.api.ParticleStyleManager;

public class DefaultStyles {

	public static ParticleStyle NONE = new ParticleStyleNone();
	public static ParticleStyle SPIRAL = new ParticleStyleSpiral();
	public static ParticleStyle HALO = new ParticleStyleHalo();
	public static ParticleStyle POINT = new ParticleStylePoint();
	public static ParticleStyle MOVE = new ParticleStyleMove();
	public static ParticleStyle SPIN = new ParticleStyleSpin();
	public static ParticleStyle QUADHELIX = new ParticleStyleQuadhelix();
	public static ParticleStyle ORBIT = new ParticleStyleOrbit();
	public static ParticleStyle FEET = new ParticleStyleFeet();

	public static void registerStyles() {
		ParticleStyleManager.registerStyle(NONE);
		ParticleStyleManager.registerStyle(SPIRAL);
		ParticleStyleManager.registerStyle(HALO);
		ParticleStyleManager.registerStyle(POINT);
		ParticleStyleManager.registerCustomHandledStyle(MOVE);
		ParticleStyleManager.registerStyle(SPIN);
		ParticleStyleManager.registerStyle(QUADHELIX);
		ParticleStyleManager.registerStyle(ORBIT);
		ParticleStyleManager.registerStyle(FEET);
		
		Bukkit.getServer().getPluginManager().registerEvents((Listener) MOVE, PlayerParticles.getPlugin());
	}

}