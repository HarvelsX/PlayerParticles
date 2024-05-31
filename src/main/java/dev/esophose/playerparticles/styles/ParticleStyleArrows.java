package dev.esophose.playerparticles.styles;

import dev.esophose.playerparticles.PlayerParticles;
import dev.esophose.playerparticles.particles.PParticle;
import dev.esophose.playerparticles.particles.ParticlePair;
import dev.rosewood.rosegarden.config.CommentedFileConfiguration;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Deque;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentLinkedDeque;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import space.arim.morepaperlib.scheduling.GracefulScheduling;
import space.arim.morepaperlib.scheduling.ScheduledTask;

public class ParticleStyleArrows extends ConfiguredParticleStyle implements Listener {

    private final Deque<LaunchedProjectile> projectiles;

    private int maxArrowsPerPlayer;
    private boolean onlySpawnIfFlying;
    private List<String> projectileEntityNames;
    private int arrowTrackingTime;

    protected ParticleStyleArrows() {
        super("arrows", false, false, 0);

        this.projectiles = new ConcurrentLinkedDeque<>();

        // Removes all arrows that are considered dead or too old to be tracked
        final GracefulScheduling scheduling = PlayerParticles.getInstance().scheduling();
        scheduling.asyncScheduler().runAtFixedRate(() -> {
            for (LaunchedProjectile launchedProjectile : this.projectiles) {
                Projectile projectile = launchedProjectile.getProjectile();
                ScheduledTask task = scheduling.entitySpecificScheduler(projectile).run(() -> {
                    boolean remove = this.arrowTrackingTime != -1
                            && projectile.getTicksLived() >= this.arrowTrackingTime;

                    if (projectile.isValid() && !remove) {
                        return;
                    }

                    this.projectiles.remove(launchedProjectile);
                }, () -> this.projectiles.remove(launchedProjectile));

                if (task == null) {
                    this.projectiles.remove(launchedProjectile);
                }
            }
        }, Duration.ZERO, Duration.ofMillis(250));
    }

    @Override
    public List<PParticle> getParticles(ParticlePair particle, Location location) {
        List<PParticle> particles = new ArrayList<>();

        int count = 0;

        for (LaunchedProjectile launchedProjectile : this.projectiles) {
            Projectile projectile = launchedProjectile.getProjectile();
            if (this.onlySpawnIfFlying && projectile.isOnGround())
                continue;

            if (launchedProjectile.getShooter().equals(particle.getOwnerUniqueId())) {
                particles.add(PParticle.builder(projectile.getLocation()).offsets(0.05F, 0.05F, 0.05F).build());
                count++;
            }

            if (count >= this.maxArrowsPerPlayer)
                break;
        }

        return particles;
    }

    @Override
    public void updateTimers() {

    }

    @Override
    protected List<String> getGuiIconMaterialNames() {
        return Collections.singletonList("BOW");
    }

    @Override
    public boolean hasLongRangeVisibility() {
        return true;
    }

    /**
     * The event used to get all projectiles fired by players
     * Adds all projectiles fired from players to the array
     *
     * @param event The ProjectileLaunchEvent
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onProjectileLaunch(ProjectileLaunchEvent event) {
        if (!(event.getEntity().getShooter() instanceof Player))
            return;

        String entityName = event.getEntity().getType().name();
        if (this.projectileEntityNames.contains(entityName)) {
            Projectile projectile = event.getEntity();
            UUID shooter = ((Player) projectile.getShooter()).getUniqueId();
            this.projectiles.addFirst(new LaunchedProjectile(projectile, shooter));
        }
    }

    @Override
    protected void setDefaultSettings(CommentedFileConfiguration config) {
        this.setIfNotExists("max-arrows-per-player", 10, "The max number of arrows that will spawn particles per player");
        this.setIfNotExists("only-spawn-if-flying", false, "Only spawn particles while the arrow is still in the air");
        this.setIfNotExists("arrow-entities", Arrays.asList("ARROW", "SPECTRAL_ARROW", "TIPPED_ARROW"), "The name of the projectile entities that are counted as arrows");
        this.setIfNotExists("arrow-tracking-time", 1200, "The maximum number of ticks to track an arrow for", "Set to -1 to disable (not recommended)");
    }

    @Override
    protected void loadSettings(CommentedFileConfiguration config) {
        this.maxArrowsPerPlayer = config.getInt("max-arrows-per-player");
        this.onlySpawnIfFlying = config.getBoolean("only-spawn-if-flying");
        this.projectileEntityNames = config.getStringList("arrow-entities");
        this.arrowTrackingTime = config.getInt("arrow-tracking-time");
    }

    private static class LaunchedProjectile {

        private final Projectile projectile;
        private final UUID shooter;

        public LaunchedProjectile(Projectile projectile, UUID shooter) {
            this.projectile = projectile;
            this.shooter = shooter;
        }

        public Projectile getProjectile() {
            return this.projectile;
        }

        public UUID getShooter() {
            return this.shooter;
        }

    }

}
