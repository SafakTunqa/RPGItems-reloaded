package think.rpgitems.power.impl;

import org.bukkit.Bukkit;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import think.rpgitems.power.*;

import java.util.List;

import static think.rpgitems.power.Utils.*;

/**
 * Power aoecommand.
 * <p>
 * The item will run {@link #command} for every entity
 * in range({@link #rm min} blocks ~ {@link #r max} blocks in {@link #facing view angle})
 * on isRightgiving the {@link #permission}
 * just for the use of the command.
 * </p>
 */
@SuppressWarnings("WeakerAccess")
@PowerMeta(defaultTrigger = "RIGHT_CLICK", withSelectors = true)
public class PowerAOECommand extends PowerCommand implements PowerHit {
    /**
     * Whether the command will be apply to the user
     */
    @Property
    public boolean selfapplication = false;

    /**
     * Type of targets. Can be `entity` `player` `mobs` now
     * entity: apply the command to every {@link LivingEntity} in range
     * player: apply the command to every {@link Player} in range
     * mobs: apply the command to every {@link LivingEntity}  except {@link Player}in range
     */
    @Property
    @AcceptedValue({"entity", "player", "mobs"})
    public String type = "entity";

    /**
     * Maximum radius
     */
    @Property(order = 6)
    public int r = 10;

    /**
     * Minimum radius
     */
    @Property(order = 5)
    public int rm = 0;

    /**
     * Maximum view angle
     */
    @Property(order = 7, required = true)
    public double facing = 30;

    /**
     * Maximum count
     */
    @Property
    public int c = 100;

    /**
     * Whether only apply to the entities that player have line of sight
     */
    @Property
    public boolean mustsee = false;

    @Override
    public String getName() {
        return "aoecommand";
    }

    @Override
    public PowerResult<Void> fire(Player player, ItemStack stack) {
        if (!checkCooldownByString(this, player, command, cooldown, true, false)) return PowerResult.cd();
        if (!getItem().consumeDurability(stack, cost)) return PowerResult.cost();
        if (!player.isOnline()) return PowerResult.noop();

        attachPermission(player, permission);

        String usercmd = handlePlayerPlaceHolder(player, command);

        boolean wasOp = player.isOp();
        try {
            if (permission.equals("*"))
                player.setOp(true);
            boolean forPlayers = type.equalsIgnoreCase("player");
            boolean forMobs = type.equalsIgnoreCase("mobs");
            int count = c;
            if (type.equalsIgnoreCase("entity") || forPlayers || forMobs) {
                List<LivingEntity> nearbyEntities = getNearestLivingEntities(this, player.getLocation(), player, r, rm);
                List<LivingEntity> ent = getLivingEntitiesInCone(nearbyEntities, player.getEyeLocation().toVector(), facing, player.getEyeLocation().getDirection());
                LivingEntity[] entities = ent.toArray(new LivingEntity[0]);
                for (int i = 0; i < count && i < entities.length; ++i) {
                    String cmd = usercmd;
                    LivingEntity e = entities[i];
                    if ((mustsee && !player.hasLineOfSight(e))
                                || (!selfapplication && e == player)
                                || (forPlayers && !(e instanceof Player))
                                || (forMobs && e instanceof Player)
                    ) {
                        ++count;
                        continue;
                    }
                    cmd = PowerCommandHit.handleEntityPlaceHolder(e, cmd);
                    Bukkit.getServer().dispatchCommand(player, cmd);
                }
            }
        } finally {
            if (permission.equals("*"))
                player.setOp(wasOp);
        }

        return PowerResult.ok();
    }

    @Override
    public PowerResult<Void> rightClick(Player player, ItemStack stack, PlayerInteractEvent event) {
        return fire(player, stack);
    }

    @Override
    public PowerResult<Void> leftClick(Player player, ItemStack stack, PlayerInteractEvent event) {
        return fire(player, stack);
    }

    @Override
    public PowerResult<Double> hit(Player player, ItemStack stack, LivingEntity entity, double damage, EntityDamageByEntityEvent event) {
        return fire(player, stack).with(damage);
    }
}
