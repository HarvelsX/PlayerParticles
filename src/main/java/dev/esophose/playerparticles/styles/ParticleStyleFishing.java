package dev.esophose.playerparticles.styles;

import com.google.common.collect.ImmutableSet;
import dev.esophose.playerparticles.PlayerParticles;
import dev.rosewood.rosegarden.config.CommentedFileConfiguration;
import dev.esophose.playerparticles.particles.PParticle;
import dev.esophose.playerparticles.particles.ParticlePair;
import java.lang.reflect.Method;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CountDownLatch;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerFishEvent;
import org.bukkit.projectiles.ProjectileSource;
import space.arim.morepaperlib.scheduling.GracefulScheduling;
import space.arim.morepaperlib.scheduling.ScheduledTask;

public class ParticleStyleFishing extends ConfiguredParticleStyle implements Listener {

    // I hate legacy versions. The Spigot API changed the PlayerFishEvent#getHook method from returning a Fish to a FishHook in 1.13
    private static Method PlayerFishEvent_getHook;
    static {
        try {
            PlayerFishEvent_getHook = PlayerFishEvent.class.getDeclaredMethod("getHook");
        } catch (ReflectiveOperationException ex) {
            ex.printStackTrace();
        }
    }

    private final Set<Projectile> projectiles;

    protected ParticleStyleFishing() {
        super("fishing", false, false, 0);

        this.projectiles = ConcurrentHashMap.newKeySet();

        // Removes all fish hooks that are considered dead
        GracefulScheduling scheduling = PlayerParticles.getInstance().scheduling();
        scheduling.asyncScheduler().runAtFixedRate(() -> {
            for (Projectile projectile : this.projectiles) {
                ScheduledTask task = scheduling.entitySpecificScheduler(projectile).run(() -> {
                    if (projectile.isValid()) return;
                    this.projectiles.remove(projectile);
                }, () -> this.projectiles.remove(projectile));

                if (task == null) {
                    this.projectiles.remove(projectile);
                }
            }
        }, Duration.ZERO, Duration.ofMillis(250));
    }

    @Override
    public List<PParticle> getParticles(ParticlePair particle, Location location) {
        Queue<PParticle> particles = new ConcurrentLinkedQueue<>();

        CountDownLatch countDownLatch = new CountDownLatch(this.projectiles.size());
        for (Projectile projectile : this.projectiles) {
            ScheduledTask task = PlayerParticles.getInstance().scheduling().entitySpecificScheduler(projectile).run(() -> {
                ProjectileSource shooter = projectile.getShooter();
                if (shooter instanceof Player && ((Player) shooter).getUniqueId().equals(particle.getOwnerUniqueId())) {
                    particles.add(PParticle.builder(projectile.getLocation()).offsets(0.05F, 0.05F, 0.05F).build());
                }
                countDownLatch.countDown();
            }, countDownLatch::countDown);

            if (task == null) {
                countDownLatch.countDown();
            }
        }

        try {
            countDownLatch.await();
        } catch (InterruptedException ignored) {
        }

        return new ArrayList<>(particles);
    }

    @Override
    public void updateTimers() {

    }

    @Override
    protected List<String> getGuiIconMaterialNames() {
        return Collections.singletonList("FISHING_ROD");
    }

    @Override
    public boolean hasLongRangeVisibility() {
        return true;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerFish(PlayerFishEvent event) {
        // Done through a string switch for 1.9.4 compatibility
        switch (event.getState().toString()) {
            case "FISHING":
                try {
                    this.projectiles.add((Projectile) PlayerFishEvent_getHook.invoke(event));
                } catch (ReflectiveOperationException ignored) { }
                break;
            case "CAUGHT_FISH":
            case "CAUGHT_ENTITY":
            case "REEL_IN":
                try {
                    this.projectiles.remove((Projectile) PlayerFishEvent_getHook.invoke(event));
                } catch (ReflectiveOperationException ignored) { }
                break;
        }
    }

    @Override
    protected void setDefaultSettings(CommentedFileConfiguration config) {

    }

    @Override
    protected void loadSettings(CommentedFileConfiguration config) {

    }

}
