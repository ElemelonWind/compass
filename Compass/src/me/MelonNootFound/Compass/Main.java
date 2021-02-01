package me.MelonNootFound.Compass;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.HashMap;
import java.util.HashSet;

public class Main extends JavaPlugin implements Listener
{
    HashMap<Player, Player> trackingPlayers = new HashMap<Player, Player>();
    HashMap<Player, HashSet<Player>> allowedPlayers = new HashMap<Player, HashSet<Player>>();
    String compassHelp = ("\u00A7e--------- PlayerCompass -------------------------" +
            "\n\u00A76/compass allow \u00A7e\u00A7oplayer\u00A7r - Allows \u00A7e\u00A7oplayer\u00A7r to track you." +
            "\n\u00A76/compass disallow\u00A7r - Disallows all players from tracking you." +
            "\n\u00A76/compass track \u00A7e\u00A7oplayer\u00A7r - Sets your compass to track \u00A7e\u00A7oplayer." +
            "\n\u00A76/compass reset\u00A7r - Resets compass to track spawn" +
            "\n\u00A76/compass bed\u00A7r - Sets your compass to track your bed");

    @Override
    public void onEnable()
    {
        getServer().getPluginManager().registerEvents(this, this);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args)
    {
        if (!(sender instanceof Player))
            return false;

        final Player player = (Player)sender;

        if (cmd.getName().equalsIgnoreCase("compass"))
        {
            if (args.length < 1) //sends /compass with no args
            {
                player.sendMessage(compassHelp);
                return true;
            }

            else if (args.length > 1 && args[0].equals("allow"))
            {
                Player allowee = Bukkit.getPlayerExact(args[1]);
                //Check if allowee is online and visible
                if (allowee == null || !player.canSee(allowee))
                {
                    player.sendMessage(ChatColor.RED + "Doesn't look like " + ChatColor.AQUA + args[1] + ChatColor.RED + " is online or a valid name.");
                    return true;
                }
                //If player hasn't allowed anyone before, add to hashmap
                if (!allowedPlayers.containsKey(player))
                    allowedPlayers.put(player, new HashSet<Player>());
                    //otherwise first check if they already allowed the allowee
                else if (allowedPlayers.get(player).contains(allowee))
                {
                    player.sendMessage(ChatColor.GREEN + "You already allowed " + ChatColor.AQUA + allowee.getName() + ChatColor.GREEN + " to track you.");
                    return true;
                }

                allowedPlayers.get(player).add(allowee);
                player.sendMessage(ChatColor.GREEN + "You allowed " +  ChatColor.AQUA + allowee.getName() + ChatColor.GREEN + " to track you.");
                return true;
            }

            else if (args.length > 1 && args[0].toLowerCase().equals("track"))
            {
                //First check if player is holding a compass
                if (!(player.getInventory().getItemInMainHand().getType().equals(Material.COMPASS) || player.getInventory().getItemInOffHand().getType().equals(Material.COMPASS)))
                {
                    player.sendMessage(ChatColor.RED + "You need to be holding a compass in your hand to track a player.");
                    return true;
                }

                final Player target = Bukkit.getPlayerExact(args[1]);
                //Check if target is invalid or invisible player
                if (target == null || !player.canSee(target))
                {
                    player.sendMessage(ChatColor.RED + "Doesn't look like " + ChatColor.AQUA + args[1] + ChatColor.RED + " is online or a valid name.");
                    return true;
                }

                //Don't allow tracking self
                if (target == player)
                {
                    player.sendMessage(ChatColor.RED + "You know where you are, right?");
                    return true;
                }

                //Check if target isn't allowing player
                if (!allowedPlayers.containsKey(target) || !allowedPlayers.get(target).contains(player))
                {
                    player.sendMessage(ChatColor.AQUA + target.getName() + ChatColor.RED + " has not allowed you to track them.");
                    player.sendMessage(ChatColor.AQUA + target.getName() + ChatColor.RED + " needs to run" + ChatColor.GOLD + " /compass allow " + player.getName() + ChatColor.RED + " to allow you to track them.");
                    return true;
                }

                trackingPlayers.put(player, target);
                player.sendMessage(ChatColor.GREEN + "Your compass is now tracking " + target.getName() + ".");

                new BukkitRunnable()
                {
                    public void run()
                    {
                        //Cancel task if player is offline or is no longer tracking target
                        if (!player.isOnline() || !trackingPlayers.containsKey(player) || !trackingPlayers.get(player).equals(target))
                            this.cancel();

                            //Cancel task if target is offline
                        else if (!target.isOnline())
                        {
                            player.sendMessage(ChatColor.RED + target.getName() + " is offline. Resetting compass to spawn.");
                            player.setCompassTarget(player.getWorld().getSpawnLocation());
                            this.cancel();
                        }

                        //Cancel task if target removed player from their allowedPlayers
                        else if (!trackingPlayers.containsKey(target) || !allowedPlayers.get(target).contains(player))
                        {
                            player.sendMessage(ChatColor.RED + target.getName() + " is no longer allowing you to track them. Resetting compass to spawn.");
                            player.setCompassTarget(player.getWorld().getSpawnLocation());
                            this.cancel();
                        }
                        else
                            player.setCompassTarget(target.getLocation());
                    }
                }.runTaskTimer(this, 5L, 300L);
                return true;
            }

            else if (args[0].toLowerCase().equals("disallow"))
            {
                allowedPlayers.remove(player);
                player.sendMessage(ChatColor.GREEN + "Disallowed all players from tracking you.");
                return true;
            }

            else if (args[0].toLowerCase().equals("reset"))
            {
                trackingPlayers.remove(player);
                player.setCompassTarget(player.getWorld().getSpawnLocation());
                player.sendMessage(ChatColor.GREEN + "Resetting compass to spawn.");
                return true;
            }
            
            else if (args[0].toLowerCase().equals("bed")) 
            {
            	trackingPlayers.remove(player);
            	if(!(player.getBedSpawnLocation() == null)) 
            	{
            		player.setCompassTarget(player.getBedSpawnLocation());
            		player.sendMessage(ChatColor.GREEN + "Setting compass to bed. If you change your bed, use /compass bed again to set it to your new bed.");
            	}
            	else 
            	{
            		player.setCompassTarget(player.getWorld().getSpawnLocation());
            		player.sendMessage(ChatColor.GREEN + "Your home bed was missing or obstructed. Compass reset to spawn.");
            	}
            	return true;
            }
        }
        //Not enough arguments
        sender.sendMessage(compassHelp);
        return true;
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void removeAllowedPlayersOnQuit(PlayerQuitEvent event)
    {
        allowedPlayers.remove(event.getPlayer());
        trackingPlayers.remove(event.getPlayer());
    }
}

//I MESSED UP BUT I DIDN'T WANNA DELETE THE CODE LOL

/*import java.util.ArrayList;
import java.util.List;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.CompassMeta;
import org.bukkit.plugin.java.JavaPlugin;

public class Main extends JavaPlugin {

	@Override
	public void onEnable() {

	}

	@Override
	public void onDisable() {

	}

	public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
		if(label.equalsIgnoreCase("compass")) {
			if(!(sender instanceof Player)) {
				sender.sendMessage("Console cannot use this command");
				return true;
			}
			Player player = (Player) sender;
			if(player.getInventory().firstEmpty() == -1) {
				Location loc = player.getLocation();
				World world = player.getWorld();
				
				world.dropItemNaturally(loc, getItem(player));
				player.sendMessage(ChatColor.GOLD + "The Minecraft Legends dropped a gift near you.");
				return true;
			}
			player.getInventory().addItem(getItem(player));
			player.sendMessage(ChatColor.GOLD + "The Minecraft Legends gave you a gift.");
			return true;
		}
		return false;
	}

	public ItemStack getItem(Player player) {
		ItemStack compass = new ItemStack(Material.COMPASS);
		CompassMeta meta = (CompassMeta) compass.getItemMeta();
		
		meta.setDisplayName(ChatColor.GREEN + "" + ChatColor.BOLD + "Useful Compass");
		List<String> lore = new ArrayList<String>();
		lore.add("");
		lore.add(ChatColor.GOLD + "" + ChatColor.ITALIC + "This compass points to your spawn bed");
		//meta.setLore(lore);
		
		if(!(player.getBedSpawnLocation() == null)) {
			meta.setLodestoneTracked(true);
			Location lodestone = player.getBedSpawnLocation();
			meta.setLodestone(lodestone);
			lore.add(ChatColor.GOLD + "Your bed location is " + player.getBedSpawnLocation().getX() + ", " 
					+ player.getBedSpawnLocation().getY() + ", " + player.getBedSpawnLocation().getZ());
		}
		
		meta.setLore(lore);
		compass.setItemMeta(meta);
		
		return compass;
	}
}*/