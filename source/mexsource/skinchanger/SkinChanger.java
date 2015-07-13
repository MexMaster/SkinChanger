package mexsource.skinchanger;

import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.UUID;

public class SkinChanger extends JavaPlugin {
	
	private static PacketHandler packetHandler;
	private static SkinManager skinManager;
	private static File imageFolder;
	//private static File levelFolder;

	//private static ArrayList<Property> levels = new ArrayList<Property>();
	
	public void onEnable(){
		imageFolder = new File(this.getDataFolder(), "skins");
		//levelFolder = new File(this.getDataFolder(), "level");
		skinManager = new SkinManager(UUID.fromString("uuid"), "e-mail", "password", "security answer");

		/*//Pre-process levels
		for(int i = 0; new File(levelFolder, i + ".png").exists(); i++){
			try{Thread.sleep(61000);}catch(Exception exc){};
			String imagePath = new File(levelFolder, i + ".png").getAbsolutePath();
			Property property = null;
			try{
				property = skinManager.receiveTexturesByImage(imagePath);
			}catch(Exception ex){
				ex.printStackTrace();
				continue;
			}
			levels.add(property);
		}*/

		packetHandler = new PacketHandler(this);
		this.getServer().getPluginManager().registerEvents(packetHandler, this);
		this.getCommand("changeskin").setExecutor(new CommandChangeSkin());
	}
	
	public void onDisable(){
		packetHandler.getProtocolHandler().close();
	}
	
	public static SkinManager getSkinManager(){
		return skinManager;
	}
	
	public static SkinManager s(){
		return getSkinManager();
	}
	
	public static PacketHandler getPacketHandler(){
		return packetHandler;
	}
	
	public static PacketHandler p(){
		return getPacketHandler();
	}
	
	public static File getImageFolder(){
		return imageFolder;
	}
	
	public static File i(){
		return getImageFolder();
	}

	/*public static File getLevelFolder(){
		return levelFolder;
	}

	public static File l(){
		return getLevelFolder();
	}

	public static Property getLevelTexture(int level){
		try{
			return levels.get(level);
		}catch(Exception ex){
			return null;
		}
	}*/
}
