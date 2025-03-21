package ru.azenizzka.xplugin.authentication;

import com.google.gson.Gson;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import ru.azenizzka.xplugin.XPlugin;
import ru.azenizzka.xplugin.utils.ChatUtils;

public class AuthManager {
  private static final String USER_DATA_DIR = "users";
  private static final String SEPARATOR = File.separator;
  private static final Set<Player> unLoggedPlayers = new HashSet<>();

  public AuthManager() {
    File userDataDir = new File(XPlugin.instance.getDataFolder(), USER_DATA_DIR);
    if (!userDataDir.exists())
      //noinspection ResultOfMethodCallIgnored
      userDataDir.mkdirs();

    new BukkitRunnable() {
      @Override
      public void run() {
        for (Player player : unLoggedPlayers) {
          if (!isRegistered(player)) {
            ChatUtils.sendTitle(
                player, "Вам необходимо зарегистрироваться!", "/register <пароль>", 2);
          } else {
            ChatUtils.sendTitle(player, "Вам необходимо авторизоваться!", "/login <пароль>", 2);
          }
        }
      }
    }.runTaskTimerAsynchronously(XPlugin.instance, 0L, 20L);
  }

  public void addUnLoggedPlayer(Player player) {
    unLoggedPlayers.add(player);
  }

  public boolean isLogged(Player player) {
    if (unLoggedPlayers.contains(player)) {
      try {
        UserData userData = getUserDataFromFile(player);

        if (userData.getLastLoggedIp().equals(getPlayerIp(player))) {
          unLoggedPlayers.remove(player);
          ChatUtils.sendMessage(player, "Вы были авторизованы по IP адресу");
        }

      } catch (Exception ignored) {
      }
    }

    return !unLoggedPlayers.contains(player);
  }

  public boolean authUser(Player player, String password) {
    if (isRegistered(player)) {
      return !loginUser(player, password);
    } else {
      return !registerUser(player, password);
    }
  }

  private boolean isRegistered(Player player) {
    File userFile = getPlayerFile(player);
    return userFile.exists();
  }

  private boolean registerUser(Player player, String password) {
    File userFile = getPlayerFile(player);

    try (FileWriter writer = new FileWriter(userFile)) {
      UserData userData = generateUserData(player, password);

      Gson gson = new Gson();
      gson.toJson(userData, writer);
      ChatUtils.sendMessage(player, "Вы успешно зарегистрировались!");
    } catch (Exception ignored) {
      return false;
    }

    return true;
  }

  private boolean loginUser(Player player, String password) {
    if (isLogged(player)) return true;

    if (!checkPassword(player, password)) return false;

    unLoggedPlayers.remove(player);
    File userFile = getPlayerFile(player);

    try (FileWriter writer = new FileWriter(userFile)) {
      UserData userData = generateUserData(player, password);

      ChatUtils.sendMessage(player, "Вы успешно авторизовались!");

      Gson gson = new Gson();
      gson.toJson(userData, writer);
    } catch (Exception ignored) {
      return false;
    }

    return true;
  }

  private boolean checkPassword(Player player, String password) {
    try {
      return getUserDataFromFile(player).checkPassword(password);
    } catch (Exception ignored) {
    }

    return false;
  }

  private UserData getUserDataFromFile(Player player) throws Exception {
    File userFile = getPlayerFile(player);

    if (!userFile.exists()) throw new Exception();

    try (FileReader reader = new FileReader(userFile)) {
      Gson gson = new Gson();
      return gson.fromJson(reader, UserData.class);
    }
  }

  private UserData generateUserData(Player player, String password) throws Exception {
    UserData userData = new UserData();
    userData.setPassword(password);
    userData.setLastLoggedIp(getPlayerIp(player));

    return userData;
  }

  private File getPlayerFile(Player player) {
    File userDir =
        new File(XPlugin.instance.getDataFolder(), USER_DATA_DIR + SEPARATOR + player.getName());

    if (!userDir.exists())
      //noinspection ResultOfMethodCallIgnored
      userDir.mkdirs();

    File userIp = new File(userDir.getPath() + SEPARATOR + getPlayerIp(player));
    try {
      //noinspection ResultOfMethodCallIgnored
      userIp.createNewFile();
    } catch (IOException e) {
      throw new RuntimeException(e);
    }

    return new File(userDir.getPath() + SEPARATOR + "data.json");
  }

  public String getPlayerIp(Player player) {
    try {
      return Objects.requireNonNull(player.getAddress()).getAddress().getHostAddress();
    } catch (Exception e) {
      player.kick(
          Component.text(
              "Произошла ошибка при получении IP-адреса. Пожалуйста, попробуйте позже и обратитесь к администрации"));
      return null;
    }
  }
}
