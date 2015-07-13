package mexsource.skinchanger;

import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import io.netty.channel.Channel;
import mexsource.skinchanger.packets.ProtocolHandler;
import mexsource.skinchanger.packets.Reflection;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.Plugin;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

public class PacketHandler implements Listener {

	private static Class<?> classPacketPlayerInfo = Reflection.getClass("{nms}.PacketPlayOutPlayerInfo");
	private static Class<?> classEnumPlayerInfoAction = Reflection.getClass("{nms}.EnumPlayerInfoAction");
	private static Class<?> classPlayerData = Reflection.getClass("{nms}.PlayerInfoData");
	private static Class<?> classEnumGamemode = Reflection.getClass("{nms}.EnumGamemode");
	private static Class<?> classIChatBaseComponent = Reflection.getClass("{nms}.IChatBaseComponent");
	private static Class<?> classPlayerInteractManager = Reflection.getClass("{nms}.PlayerInteractManager");
	private static Class<?> classEntityPlayer = Reflection.getClass("{nms}.EntityPlayer");

	private ProtocolHandler protocolHandler;
	private HashMap<UUID, Object> privatePackets = new HashMap<UUID, Object>();

	public void sendPrivatePacket(Player p, Object packet){
		privatePackets.put(p.getUniqueId(), packet);
		protocolHandler.sendPacket(p, packet);
	}

	public void broadcastPrivatePacket(Object packet){
		for(Player player : Bukkit.getOnlinePlayers()){
			sendPrivatePacket(player, packet);
		}
	}

	private HashMap<UUID, Property> currentSkins = new HashMap<UUID, Property>();

	public PacketHandler(Plugin plugin){
		protocolHandler = new ProtocolHandler(plugin){
			@SuppressWarnings("rawtypes")
			@Override
			public Object onPacketOutAsync(final Player reciever, Channel channel, Object packet){
				if(privatePackets.containsValue(packet) && privatePackets.containsKey(reciever.getUniqueId())){
					if(privatePackets.get(reciever.getUniqueId()).equals(packet)){
						privatePackets.remove(reciever.getUniqueId());
						return super.onPacketOutAsync(reciever, channel, packet);
					}
				}
				if(!classPacketPlayerInfo.isInstance(packet) || reciever == null || !currentSkins.containsKey(reciever.getUniqueId())){
					return super.onPacketOutAsync(reciever, channel, packet);
				}
				try{
					Field listField = classPacketPlayerInfo.getDeclaredField("b");
					listField.setAccessible(true);

					Object list = listField.get(packet);
					List newList = fillGameProfiles((List) list);
					listField.set(packet, newList);

					/*//For level in tab list
					Bukkit.getScheduler().runTaskLater(plugin, new Runnable(){
						@Override
						public void run(){
							try{
								Property levelTexture = SkinChanger.getLevelTexture(reciever.getLevel());
								if(levelTexture != null){
									broadcastPrivatePacket(createRemovePacket(reciever));
									broadcastPrivatePacket(createAddPacket(reciever, levelTexture));
								}
							}catch(Exception ex){
								ex.printStackTrace();
							}
						}
					}, 5L);*/
				}catch(Exception ex){
					ex.printStackTrace();
				}
				return super.onPacketOutAsync(reciever, channel, packet);
			}
		};
	}

	private GameProfile fillGameProfile(GameProfile profile){
		if(!currentSkins.containsKey(profile.getId())){
			return profile;
		}
		Property skin = currentSkins.get(profile.getId());
		profile.getProperties().clear();
		profile.getProperties().put("textures", skin);
		return profile;
	}

	private List fillGameProfiles(List playerDatas) throws Exception {
		Field gameProfileField = classPlayerData.getDeclaredField("d");
		gameProfileField.setAccessible(true);
		for(Object playerData : playerDatas){
			GameProfile profile = (GameProfile) gameProfileField.get(playerData);
			fillGameProfile(profile);
		}
		return playerDatas;
	}

	public ProtocolHandler getProtocolHandler(){
		return protocolHandler;
	}

	public void onPlayerLeave(PlayerQuitEvent e){
		currentSkins.remove(e.getPlayer().getUniqueId());
	}

	public void updateSkin(Player p, Property skin){
		try{
			broadcastPrivatePacket(createRemovePacket(p));
			broadcastPrivatePacket(createAddPacket(p, skin));

			currentSkins.put(p.getUniqueId(), skin);
		}catch(Exception ex){
			ex.printStackTrace();
		}
	}

	private Object createRemovePacket(Player p) throws Exception {
		Object entityPlayer = Reflection.getHandle(p);
		Object enumRemovePlayer = classEnumPlayerInfoAction.getDeclaredField("REMOVE_PLAYER").get(null);
		Object packetInfoRemove = classPacketPlayerInfo.newInstance();
		Field enumField = classPacketPlayerInfo.getDeclaredField("a");
		Field listField = classPacketPlayerInfo.getDeclaredField("b");

		enumField.setAccessible(true);
		listField.setAccessible(true);

		enumField.set(packetInfoRemove, enumRemovePlayer);

		List playerDataList = new ArrayList();

		Field fieldPing = classEntityPlayer.getDeclaredField("ping");
		Field fieldPlayerInteractManager = classEntityPlayer.getDeclaredField("playerInteractManager");
		Method methodGetGamemode = classPlayerInteractManager.getDeclaredMethod("getGameMode");
		Method methodGetPlayerListName = classEntityPlayer.getDeclaredMethod("getPlayerListName");
		Method methodGetProfile = classEntityPlayer.getSuperclass().getDeclaredMethod("getProfile");

		//Needed two-way
		int ping = fieldPing.getInt(entityPlayer);
		Object playerInteractManager = fieldPlayerInteractManager.get(entityPlayer);
		Object gameMode = methodGetGamemode.invoke(playerInteractManager);
		Object playerListName = methodGetPlayerListName.invoke(entityPlayer);
		GameProfile profile = (GameProfile)methodGetProfile.invoke(entityPlayer);

		Constructor entityPlayerDataConstructor = classPlayerData.getConstructor(classPacketPlayerInfo, GameProfile.class, int.class, classEnumGamemode, classIChatBaseComponent);

		//instance: PlayerInfoData
		Object playerData = entityPlayerDataConstructor.newInstance(packetInfoRemove, profile, ping, gameMode, playerListName);

		playerDataList.add(playerData);

		listField.set(packetInfoRemove, playerDataList);

		return packetInfoRemove;
	}

	private Object createAddPacket(Player p) throws Exception {
		return createAddPacket(p, (GameProfile)null);
	}

	private Object createAddPacket(Player p, GameProfile profile) throws Exception {
		Object entityPlayer = Reflection.getHandle(p);
		Object enumAddPlayer = classEnumPlayerInfoAction.getDeclaredField("ADD_PLAYER").get(null);
		Object packetInfoAdd = classPacketPlayerInfo.newInstance();
		Field enumField = classPacketPlayerInfo.getDeclaredField("a");
		Field listField = classPacketPlayerInfo.getDeclaredField("b");

		enumField.setAccessible(true);
		listField.setAccessible(true);

		enumField.set(packetInfoAdd, enumAddPlayer);

		List playerDataList = new ArrayList();

		Field fieldPing = classEntityPlayer.getDeclaredField("ping");
		Field fieldPlayerInteractManager = classEntityPlayer.getDeclaredField("playerInteractManager");
		Method methodGetGamemode = classPlayerInteractManager.getDeclaredMethod("getGameMode");
		Method methodGetPlayerListName = classEntityPlayer.getDeclaredMethod("getPlayerListName");
		Method methodGetProfile = classEntityPlayer.getSuperclass().getDeclaredMethod("getProfile");

		//Needed two-way
		int ping = fieldPing.getInt(entityPlayer);
		Object playerInteractManager = fieldPlayerInteractManager.get(entityPlayer);
		Object gameMode = methodGetGamemode.invoke(playerInteractManager);
		Object playerListName = methodGetPlayerListName.invoke(entityPlayer);
		GameProfile playerProfile = (GameProfile)methodGetProfile.invoke(entityPlayer);

		playerDataList.clear();

		Constructor entityPlayerDataConstructor = classPlayerData.getConstructor(classPacketPlayerInfo, GameProfile.class, int.class, classEnumGamemode, classIChatBaseComponent);

		Object playerData = entityPlayerDataConstructor.newInstance(packetInfoAdd, (profile == null ? playerProfile : profile), ping, gameMode, playerListName);

		playerDataList.add(playerData);

		listField.set(packetInfoAdd, playerDataList);

		return packetInfoAdd;
	}

	private Object createAddPacket(Player p, Property skin) throws Exception {
		Object entityPlayer = Reflection.getHandle(p);
		Object enumAddPlayer = classEnumPlayerInfoAction.getDeclaredField("ADD_PLAYER").get(null);
		Object packetInfoAdd = classPacketPlayerInfo.newInstance();
		Field enumField = classPacketPlayerInfo.getDeclaredField("a");
		Field listField = classPacketPlayerInfo.getDeclaredField("b");

		enumField.setAccessible(true);
		listField.setAccessible(true);

		enumField.set(packetInfoAdd, enumAddPlayer);

		List playerDataList = new ArrayList();

		Field fieldPing = classEntityPlayer.getDeclaredField("ping");
		Field fieldPlayerInteractManager = classEntityPlayer.getDeclaredField("playerInteractManager");
		Method methodGetGamemode = classPlayerInteractManager.getDeclaredMethod("getGameMode");
		Method methodGetPlayerListName = classEntityPlayer.getDeclaredMethod("getPlayerListName");
		Method methodGetProfile = classEntityPlayer.getSuperclass().getDeclaredMethod("getProfile");

		//Needed two-way
		int ping = fieldPing.getInt(entityPlayer);
		Object playerInteractManager = fieldPlayerInteractManager.get(entityPlayer);
		Object gameMode = methodGetGamemode.invoke(playerInteractManager);
		Object playerListName = methodGetPlayerListName.invoke(entityPlayer);
		GameProfile playerProfile = (GameProfile)methodGetProfile.invoke(entityPlayer);

		playerDataList.clear();

		Constructor entityPlayerDataConstructor = classPlayerData.getConstructor(classPacketPlayerInfo, GameProfile.class, int.class, classEnumGamemode, classIChatBaseComponent);

		playerProfile.getProperties().clear();
		playerProfile.getProperties().put("textures", skin);

		Object playerData = entityPlayerDataConstructor.newInstance(packetInfoAdd, playerProfile, ping, gameMode, playerListName);

		playerDataList.add(playerData);

		listField.set(packetInfoAdd, playerDataList);

		return packetInfoAdd;
	}
}
