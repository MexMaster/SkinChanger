package mexsource.skinchanger;

import java.io.File;
import java.util.UUID;

import com.mojang.authlib.properties.Property;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class CommandChangeSkin implements CommandExecutor {

	@Override
	public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args){
		if(args.length < 1){
			sender.sendMessage(ChatColor.GRAY + "Keine Datei angegeben");
			return true;
		}
		if(!(sender instanceof Player)){
			sender.sendMessage(ChatColor.GRAY + "Du bist kein Spieler");
			return true;
		}
		if(SkinChanger.s().isUsed()){
			sender.sendMessage(ChatColor.GRAY + "Bitte warte einen Moment");
			return true;
		}
		Player p = (Player)sender;
		String imageName = args[0];
		String filePath = new File(SkinChanger.i().getAbsolutePath(), imageName).getAbsolutePath();
		//SkinChanger.p().setNextSkin(p.getUniqueId(), SkinChanger.s().receiveTextures(filePath));
		sender.sendMessage(ChatColor.GRAY + "Bitte warten...");
		Property property;
		if(filePath.endsWith(".png")){
			sender.sendMessage(ChatColor.GRAY + "[File: " + filePath + "]");
			if(!new File(filePath).exists()){
				sender.sendMessage(ChatColor.GRAY + "Die Datei existiert nicht!");
				property = null;
			}else{
				property = SkinChanger.s().receiveTexturesByImage(filePath);
			}
		}else{
			UUID playerUUID = Bukkit.getOfflinePlayer(args[0]).getUniqueId();
			sender.sendMessage(ChatColor.GRAY + "[UUID: " + playerUUID.toString() + "]");
			property = SkinChanger.s().receiveTexturesByUUID(playerUUID);
		}
		if(property == null){
			sender.sendMessage(ChatColor.GRAY + "Der Skin konnte nicht aktualisiert werden!");
			return true;
		}
		SkinChanger.p().updateSkin(p, property);

		World w1 = Bukkit.getWorlds().get(0);
		World w2 = Bukkit.getWorlds().get(1);
		World currentWorld = p.getWorld();
		Location currentLocation = p.getLocation();
		Location tpTo;

		if(currentWorld.getUID().equals(w1.getUID())){
			tpTo = new Location(w2, 0, 0, 0);
		}else{
			tpTo = new Location(w1, 0, 0, 0);
		}

		p.teleport(tpTo);
		p.teleport(currentLocation);

		sender.sendMessage(ChatColor.GRAY + "Dein Skin wurde geändert!");
		return true;
	}
}
