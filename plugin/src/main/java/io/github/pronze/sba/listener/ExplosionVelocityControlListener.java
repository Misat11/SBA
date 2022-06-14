package io.github.pronze.sba.listener;
import io.github.pronze.sba.SBA;
import io.github.pronze.sba.config.SBAConfig;
import io.github.pronze.sba.service.AntiCheatIntegration;
import io.github.pronze.sba.utils.Logger;
import org.bukkit.GameMode;
import org.bukkit.entity.Explosive;
import org.bukkit.entity.Fireball;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.util.Vector;
import org.screamingsandals.bedwars.Main;
import org.screamingsandals.lib.utils.annotations.Service;
import org.screamingsandals.lib.utils.annotations.methods.OnPostEnable;
import java.util.HashSet;
import java.util.Set;

@Service
public class ExplosionVelocityControlListener implements Listener {
    private final Set<Player> explosionAffectedPlayers = new HashSet<>();

    @OnPostEnable
    public void postEnable() {
        SBA.getInstance().registerListener(this);
    }

    @EventHandler
    public void onPlayerDamage(EntityDamageEvent event) {
        if (event.getCause() == EntityDamageEvent.DamageCause.FALL) {
            final var entity = event.getEntity();
            if (entity instanceof Player) {
                final var player = (Player) entity;
                if (explosionAffectedPlayers.contains(player)) {
                    event.setDamage(SBAConfig.getInstance().node("tnt-fireball-jumping", "fall-damage").getDouble(3.0D));
                    explosionAffectedPlayers.remove(player);
                    AntiCheatIntegration.getInstance().endTntJump(player);
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onExplode(EntityDamageByEntityEvent event) {
        final var explodedEntity = event.getDamager();

        if (explodedEntity instanceof Explosive) {
            final var detectionDistance = SBAConfig.getInstance().node("tnt-fireball-jumping", "detection-distance").getDouble(5.0D);

            explodedEntity.getWorld().getNearbyEntities(explodedEntity.getLocation(), detectionDistance, detectionDistance, detectionDistance)
                    .stream()
                    .filter(entity ->  !entity.equals(explodedEntity))
                    .forEach(entity -> {
                        Vector vector = explodedEntity
                                .getLocation()
                                .clone()
                                .add(0, SBAConfig.getInstance().node("tnt-fireball-jumping", "acceleration-y").getDouble(1.0) ,0)
                                .toVector()
                                .subtract(explodedEntity.getLocation().toVector()).normalize();
                        vector.setY(vector.getY() /  SBAConfig.getInstance().node("tnt-fireball-jumping", "reduce-y").getDouble(2.0));
                        vector.multiply(SBAConfig.getInstance().node("tnt-fireball-jumping", "launch-multiplier").getDouble(4.0));

                        if (entity instanceof Player) {
                            final var player = (Player) entity;
                            if (player.getGameMode() == GameMode.SPECTATOR || !Main.isPlayerInGame(player)) {
                                return;
                            }
                            vector.add(new Vector(player.getEyeLocation().getDirection().getX(), 0, player.getEyeLocation().getDirection().getZ()));
                            AntiCheatIntegration.getInstance().beginTntJump(player);
                            player.setVelocity(vector);
                            explosionAffectedPlayers.add(player);
                            return;
                        }

                        if (Main.getInstance().isEntityInGame(entity)) {
                            entity.setVelocity(vector);
                        }
                    });
        }
    }
}
