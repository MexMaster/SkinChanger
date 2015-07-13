package mexsource.skinchanger;

import com.gargoylesoftware.htmlunit.BrowserVersion;
import com.gargoylesoftware.htmlunit.Page;
import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.html.*;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.minecraft.MinecraftSessionService;
import com.mojang.authlib.properties.Property;
import mexsource.skinchanger.packets.Reflection;
import mexsource.skinchanger.packets.Reflection.MethodInvoker;

import java.lang.reflect.Method;
import java.util.UUID;

public class SkinManager {

	private static final String LOGINURL = "https://minecraft.net/login";
	private static final String PROFILEURL = "https://minecraft.net/profile";
	
	private String username;
	private UUID mojangUUID;
	private String password;
	private String secQuestion;
	private WebClient webClient;
	
	private boolean inUse = false;

	public SkinManager(UUID mojangUUID, String username, String password, String secQuestion){
		this.username = username;
		this.mojangUUID = mojangUUID;
		this.password = password;
		this.secQuestion = secQuestion;
		this.webClient = new WebClient(BrowserVersion.FIREFOX_24);
		webClient.getOptions().setJavaScriptEnabled(false);
		webClient.getOptions().setCssEnabled(false);
		webClient.getOptions().setThrowExceptionOnScriptError(false);
		webClient.getOptions().setThrowExceptionOnFailingStatusCode(false);
		webClient.getOptions().setPrintContentOnFailingStatusCode(false);
		webClient.getOptions().setUseInsecureSSL(true);
		webClient.getCookieManager().setCookiesEnabled(true);
	}
	
	public boolean isUsed(){
		return inUse;
	}
	
	public GameProfile setTextures(GameProfile profile, String imagePath){
		try{
			if(!upload(imagePath)){
				return null;
			}
			GameProfile tmpProfile = new GameProfile(mojangUUID, null);
			Class<?> classMinecraftServer = Reflection.getClass("{nms}.MinecraftServer");
			Object minecaftServer = classMinecraftServer.getMethod("getServer").invoke(null);
			Method methodGetSessionService = null;
			for(Method method : classMinecraftServer.getMethods()){
				if(method.getReturnType() == MinecraftSessionService.class){
					methodGetSessionService = method;
				}
			}
			if(methodGetSessionService == null){
				methodGetSessionService = classMinecraftServer.getMethod("aB");
			}
			Object sessionService = methodGetSessionService.invoke(minecaftServer);
			MethodInvoker methodFillProfile = Reflection.getMethod(sessionService.getClass(), "fillProfileProperties", GameProfile.class, boolean.class);
			methodFillProfile.invoke(sessionService, tmpProfile, true);
			profile.getProperties().clear();
			profile.getProperties().putAll(tmpProfile.getProperties());
			return profile;
		}catch(Exception ex){
			ex.printStackTrace();
			return null;
		}
	}

	public Property receiveTexturesByImage(String imagePath){
		try{
			if(!upload(imagePath)){
				return null;
			}
			Thread.sleep(3000);
			GameProfile tmpProfile = new GameProfile(mojangUUID, null);
			Class<?> classMinecraftServer = Reflection.getClass("{nms}.MinecraftServer");
			Object minecaftServer = classMinecraftServer.getMethod("getServer").invoke(null);
			Method methodGetSessionService = null;
			for(Method method : classMinecraftServer.getMethods()){
				if(method.getReturnType() == MinecraftSessionService.class){
					methodGetSessionService = method;
				}
			}
			if(methodGetSessionService == null){
				methodGetSessionService = classMinecraftServer.getMethod("aB");
			}
			Object sessionService = methodGetSessionService.invoke(minecaftServer);
			MethodInvoker methodFillProfile = Reflection.getMethod(sessionService.getClass(), "fillProfileProperties", GameProfile.class, boolean.class);
			methodFillProfile.invoke(sessionService, tmpProfile, true);
			return tmpProfile.getProperties().get("textures").iterator().next();
		}catch(Exception ex){
			ex.printStackTrace();
			return null;
		}
	}

	public Property receiveTexturesByUUID(UUID playerUUID){
		try{
			/*if(!upload(imagePath)){
				return null;
			}
			Thread.sleep(5000);*/
			GameProfile tmpProfile = new GameProfile(playerUUID, null);
			Class<?> classMinecraftServer = Reflection.getClass("{nms}.MinecraftServer");
			Object minecaftServer = classMinecraftServer.getMethod("getServer").invoke(null);
			Method methodGetSessionService = null;
			for(Method method : classMinecraftServer.getMethods()){
				if(method.getReturnType() == MinecraftSessionService.class){
					methodGetSessionService = method;
				}
			}
			if(methodGetSessionService == null){
				methodGetSessionService = classMinecraftServer.getMethod("aB");
			}
			Object sessionService = methodGetSessionService.invoke(minecaftServer);
			MethodInvoker methodFillProfile = Reflection.getMethod(sessionService.getClass(), "fillProfileProperties", GameProfile.class, boolean.class);
			methodFillProfile.invoke(sessionService, tmpProfile, true);
			return tmpProfile.getProperties().get("textures").iterator().next();
		}catch(Exception ex){
			ex.printStackTrace();
			return null;
		}
	}

	public boolean upload(String imagePath){
		if(isUsed()){
			return false;
		}
		inUse = true;
		try{
			HtmlPage page = webClient.getPage(LOGINURL);
			if(isLoginPage(page)){
				handleLoginPage(page);
			}
			page = webClient.getPage(PROFILEURL);
			if(isSecurityPage(page)){
				handleSecurityQuestionPage(page);
			}
			page = webClient.getPage(PROFILEURL);
			if(isProfilePage(page)){
				handleUpload(page, imagePath);
			}
		}catch(Exception ex){
			ex.printStackTrace();
			cleanUp();
			return false;
		}
		cleanUp();
		return true;
	}
	
	private void cleanUp(){
		if(webClient != null){
			webClient.closeAllWindows();
			webClient.getCache().clear();
			webClient.getCookieManager().clearCookies();
		}
		inUse = false;
	}
	
	private HtmlPage handleLoginPage(HtmlPage page) throws Exception {
		HtmlForm loginForm = (HtmlForm)page.getElementById("loginForm");
		
		HtmlTextInput usernameField = loginForm.getInputByName("username");
		HtmlPasswordInput passwordField = loginForm.getInputByName("password");
		HtmlCheckBoxInput rememberButton = loginForm.getInputByName("remember");
		HtmlSubmitInput submitButton = loginForm.getInputByValue("Sign in");

		usernameField.setValueAttribute(this.username);
		passwordField.setValueAttribute(this.password);
		rememberButton.setValueAttribute("true");
		return submitButton.click();
	}
	
	private Page handleSecurityQuestionPage(HtmlPage page) throws Exception {
		HtmlForm challengeForm = (HtmlForm)page.getElementById("challengeForm");
		HtmlTextInput awnserField = challengeForm.getInputByName("answer");
		HtmlButton proceedButton = (HtmlButton)challengeForm.getFirstByXPath("//button");

		awnserField.setValueAttribute(this.secQuestion);
		return proceedButton.click();
	}
	
	private void handleUpload(HtmlPage htmlPage, String imagePath) throws Exception {

		HtmlForm form = (HtmlForm)htmlPage.getForms().get(0);
		HtmlFileInput fileUploader = (HtmlFileInput)form.getInputByName("skin");
		HtmlSubmitInput uploadButton = form.getInputByValue("Upload");

		fileUploader.setValueAttribute(imagePath);
		HtmlPage page = uploadButton.click();
	}
	
	private boolean isLoginPage(HtmlPage page){
		return page.asText().contains("When logging in with a Mojang account, use your e-mail address as username.");
	}
	
	private boolean isSecurityPage(HtmlPage page){
		return page.asText().contains("Confirm your identity");
	}
	
	private boolean isProfilePage(HtmlPage page){
		return page.asText().contains("Change how you look in Minecraft");
	}
}
