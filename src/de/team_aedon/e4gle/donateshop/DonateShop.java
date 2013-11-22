package de.team_aedon.e4gle.donateshop;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.enchantments.EnchantmentWrapper;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;

import java.text.SimpleDateFormat;
import java.util.*;

import de.team_aedon.e4gle.donateshop.DBConnection;

public final class DonateShop extends JavaPlugin implements Listener{
	
	private DBConnection db;

	public void onEnable(){
		getConfig();
		reloadConfig();
		saveDefaultConfig();
		Bukkit.getPluginManager().registerEvents(this, this);
		String url = "jdbc:mysql://"+getConfig().getString("mysql.url")+"/"+getConfig().getString("mysql.db");
		String user = getConfig().getString("mysql.user");
		String password = getConfig().getString("mysql.password");
		String servername = getConfig().getString("Servername");
		try {
			this.db = new DBConnection(url,user,password,servername);
			getLogger().info("Hooked into Database!");
			this.db.createDefaultTables();
			for(String paket : getConfig().getConfigurationSection("container").getKeys(false)){
				this.db.addPaketIfNotExists(paket);
				getLogger().info("Paket '"+paket+"' geladen!");
			}
			db.addPaketeToServerTableIfNotExists();
			getLogger().info("Plugin erfolgreich geladen!");
		}catch(Exception e){
			getLogger().info("Error: "+e.getMessage());
			getLogger().info("LADEVORGANG ABGEBROCHEN!");
		}
	}
	
	public void onDisable(){
		getLogger().info("Plugin beendet!");
	}

	public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args){
		if(cmd.getName().equalsIgnoreCase("dshop")){
			if(args.length==0){
				sender.sendMessage(ChatColor.GOLD+"Commands:");
				if(sender.hasPermission("dshop.admin")){
					sender.sendMessage("- /dshop reload "+ChatColor.GRAY+"- "+ChatColor.GREEN+"Reloaded die Config.yml!");
					sender.sendMessage("- /dshop paket <kit> <spieler> <dauer>"+ChatColor.GRAY+"- "+ChatColor.GREEN+"HauptCommand - Nur Console!");
					sender.sendMessage("- /dshop bought [Spielername] "+ChatColor.GRAY+"- "+ChatColor.GREEN+"Zeigt alle Erworbenen Pakete an!");
				}else{
					sender.sendMessage("- /dshop bought "+ChatColor.GRAY+"- "+ChatColor.GREEN+"Zeigt alle Erworbenen Pakete an!");
				}
				sender.sendMessage("- /dshop "+ChatColor.GRAY+"- "+ChatColor.GREEN+"Ruft diese Hilfe auf!");
				sender.sendMessage("- /dshop expiration"+ChatColor.GRAY+"- "+ChatColor.GREEN+"Zeigt das Verfallsdatum der Erworbenen Pakete an!");
				return true;
			} else if(args[0].equalsIgnoreCase("reload")){
				getConfig();
				reloadConfig();
				saveDefaultConfig();
				sender.sendMessage(ChatColor.DARK_BLUE+"[DonateShop] "+ChatColor.WHITE+"config.yml reloaded");
				getLogger().info("'Config.yml' erneuert!");
				String url = "jdbc:mysql://"+getConfig().getString("mysql.url")+"/"+getConfig().getString("mysql.db");
				String user = getConfig().getString("mysql.user");
				String password = getConfig().getString("mysql.password");
				String servername = getConfig().getString("Servername");
				try {
					this.db = new DBConnection(url,user,password,servername);
					getLogger().info("Hooked into Database!");
					this.db.createDefaultTables();
					for(String paket : getConfig().getConfigurationSection("container").getKeys(false)){
						this.db.addPaketIfNotExists(paket);
						getLogger().info("Paket '"+paket+"' geladen!");
					}
					db.addPaketeToServerTableIfNotExists();
					getLogger().info("Plugin erfolgreich reloaded!");
				}catch(Exception e){
					getLogger().info("Error: "+e.getMessage());
					getLogger().info("LADEVORGANG ABGEBROCHEN!");
				}
				return true;
			} else if(args[0].equalsIgnoreCase("paket") && args.length==4){
				if(sender instanceof Player){
					sender.sendMessage("Du hast keine Berechtigung diesen Befehl zu nuzen!");
					return true;
				}
				try{
					if(args[3].equalsIgnoreCase("-1")){
						Long duration = -1L;
						db.addPaketToPlayer(args[2].toString(), args[1].toString(), duration);
					} else{
						int dauer = Integer.parseInt(args[3].toString());
						Long addtime = dauer*86400000L;
						Long duration = System.currentTimeMillis()+addtime;
						db.addPaketToPlayer(args[2].toString(), args[1].toString(), duration);
					}
					sender.sendMessage("Added Paket '"+args[1].toString()+"' to Player '"+args[2].toString()+"'");
					Player target = (Bukkit.getServer().getPlayer(args[2]));
			        if(target != null) {
			        	target.sendMessage(ChatColor.GREEN+"Vielen Dank, dass du unseren Server unterstützt! "+ChatColor.WHITE+"Das von dir erworbene Paket steht dir bei deinem nächsten Login zu Verfügung!");
			        }
					return true;
				}catch(Exception e){
					getLogger().info("Error in 'AddPaketToPlayerEvent: "+e.getMessage());
				}
			} else if(args[0].equalsIgnoreCase("expiration")){
				if(sender instanceof Player){
					try{
						sender.sendMessage(ChatColor.GOLD+"Paketname    "+ChatColor.WHITE+"-    "+ChatColor.RED+"Expiration");
						sender.sendMessage("");
						for(int paketID : db.getPlayerPakete(sender.getName())){
							if(db.paketExpireable(paketID)){
								if(!db.isPaketExpired(paketID)){
									SimpleDateFormat ft = 
											new SimpleDateFormat ("dd.MM.yyyy 'um' HH:mm");
									sender.sendMessage(ChatColor.GREEN+db.getPaketNameBySPid(paketID)+ChatColor.WHITE+"    -    "+ChatColor.GRAY+ft.format(db.getExpirationDateInMS(paketID)));
								}
							}
						}
						return true;
					}catch(Exception e){
						getLogger().info("Error in onPlayerComman(expiration) :"+e.getMessage());
					}
				}else{
					sender.sendMessage("Dieser Command muss Ingame ausgeführt werden!");
					return true;
				}
			} else if(args[0].equalsIgnoreCase("bought")){
				String Spielername = new String();
				try{
					if(args.length==1 && sender instanceof Player){
						Spielername = sender.getName();
					}else if(args.length==2 && db.playerExists(args[1])){
						if(sender.hasPermission("dshop.admin")){
							Spielername = args[1];
						}else{
							sender.sendMessage(ChatColor.RED+"Keine Berechtigung!");
							return true;
						}
					}else if(args.length==2 && !db.playerExists(args[1])){
						sender.sendMessage(ChatColor.RED+"Dieser Spieler existiert nicht!");
						return true;
					}else{
						sender.sendMessage(ChatColor.RED+"Usage: /dshop bought [Spielername]");
						return true;
					}
					sender.sendMessage(ChatColor.GOLD+"Alle gekauften Pakete: ");
					sender.sendMessage("");
					if(!db.playerHasBoughtPakets(Spielername)){
						sender.sendMessage(ChatColor.GRAY+"Es wurden noch keine Pakete gekauft!");
					} else {
						for(int paketID : db.getPlayerPakete(Spielername)){
							sender.sendMessage(ChatColor.GREEN+db.getPaketNameBySPid(paketID));
						}
					}
					return true;
				}catch(Exception e){
					getLogger().info("Eror in onPlayerCommand: "+e.getMessage()); 
				}
			}
		}
		return false;
	}
	
	@EventHandler
	public void onPlayerJoin(PlayerJoinEvent evt) {
		Player player = evt.getPlayer();
		try {
			db.addPaketeToServerTableIfNotExists();
			db.addPlayerIfNotExists(player.getName());
			List<String> pakete =db.giveUngivenPlayerPakete(player.getName());
			for(String paket : pakete){
				if(getConfig().getConfigurationSection("container").contains(paket)){
					String path = "container."+paket;
					if(getConfig().getConfigurationSection(path).contains("give")){
						ConfigurationSection items = getConfig().getConfigurationSection((path+".give"));
						for (String item : items.getKeys(false)){
							ItemStack IS = new ItemStack(Material.getMaterial(getConfig().getString(path+".give."+item+".material")));
							IS.setAmount(getConfig().getInt(path+".give."+item+".qty"));
							if(getConfig().getConfigurationSection((path+".give."+item)).contains("enchantments")){
								ConfigurationSection enchantments = getConfig().getConfigurationSection((path+".give."+item+".enchantments"));
								for(String enchantment : enchantments.getKeys(false)){
									Enchantment myEnchantment = new EnchantmentWrapper(getConfig().getInt((path+".give."+item+".enchantments."+enchantment+".id")));
									IS.addUnsafeEnchantment(myEnchantment, getConfig().getInt((path+".give."+item+".enchantments."+enchantment+".power")));
								}
							}
							if(getConfig().getConfigurationSection((path+".give."+item)).contains("item-name")){
								ItemMeta im = IS.getItemMeta();
								String name = getConfig().getString(path+".give."+item+".item-name");
								name = replaceTags(name, player.getName());
								im.setDisplayName(name);
								IS.setItemMeta(im);
							}
							if(getConfig().getConfigurationSection((path+".give."+item)).contains("item-lore")){
								ItemMeta im = IS.getItemMeta();
								List<String> newlore = new ArrayList<String>();
								for(String lore : getConfig().getStringList(path+".give."+item+".item-lore")){
									lore = replaceTags(lore, player.getName());
									newlore.add(lore);
								}
								im.setLore(newlore);
								IS.setItemMeta(im);
							}
							player.getInventory().addItem(IS);
						}
					}
					
					if(getConfig().getConfigurationSection(path).contains("rang")){
						String command = "yapp "+player.getName()+" | g= "+getConfig().getString((path+".rang"))+" | @ "+player.getName();
						Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command);
					}
				
					if(getConfig().getConfigurationSection(path).contains("commands")){
						List<String> commands = getConfig().getStringList((path+".commands"));
						for(String command : commands){
							command = replaceTags(command, player.getName());
							Boolean boo = Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command);
							if(!boo){
								getLogger().info("Der in der 'config.yml' angegebene Command('"+command+"') scheint nicht zu funktionieren!");
							}
						}
						
					}
					player.sendMessage(ChatColor.DARK_GREEN+"Vielen Dank, dass du unseren Server unterstützt! "+ChatColor.WHITE+"Du hast das Paket: '"+ChatColor.BOLD+""+ChatColor.GOLD+paket+ChatColor.WHITE+"' erhalten!");
				}
			}
			List<String> expiredPakete = db.getExpiredPlayerPakets(player.getName());
			if(!expiredPakete.isEmpty()){
				for(String paket : expiredPakete){
					if(!db.hasAnUnexpiredPaketOf(player.getName(), paket)){
						getLogger().info(db.hasAnUnexpiredPaketOf(player.getName(), paket)+"");
						if(getConfig().getConfigurationSection(("container."+paket)).contains("onExpiration")){
							if(getConfig().getConfigurationSection(("container."+paket+".onExpiration")).contains("rang")){
								String onEx = "yapp "+player.getName()+" | g= "+getConfig().getString(("container."+paket+".onExpiration.rang"))+" | @ "+player.getName();
								Bukkit.dispatchCommand(Bukkit.getConsoleSender(), onEx);
							}
							if(getConfig().getConfigurationSection(("container."+paket+".onExpiration")).contains("commands")){
								List<String> commands = getConfig().getStringList(("container."+paket+".onExpiration.commands"));
								for(String command : commands){
									command = replaceTags(command, player.getName());
									Boolean boo = Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command);
									if(!boo){
										getLogger().info("Der in der 'config.yml' angegebene Command('"+command+"') scheint nicht zu funktionieren!");
									}
								}
							}
						}
					}
				}
			}
		}catch (Exception e) {
			getLogger().info("Error on 'PlayerLoginEvent': "+e.getMessage());
		}
	}
	
	public String replaceTags(String message, String spieler){
		message.replace("<player.name>", spieler);
		message.replace("<auml>", "ä");
		message.replace("<ouml>", "ö");
		message.replace("<uuml>", "ü");
		return message;
	}

}
