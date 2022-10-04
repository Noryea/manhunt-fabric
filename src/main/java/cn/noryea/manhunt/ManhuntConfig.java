package cn.noryea.manhunt;

import com.google.gson.*;
import net.minecraft.util.Formatting;

import java.io.*;

import static cn.noryea.manhunt.Manhunt.LOGGER;

public class ManhuntConfig {

  private ManhuntConfig() {
  }

  //Config instance.
  public static final ManhuntConfig INSTANCE = new ManhuntConfig();
  private final File confFile = new File("./config/manhunt.json");
  Gson gson = new GsonBuilder().setPrettyPrinting().create();

  private Formatting huntersColor = Formatting.RED;
  private Formatting runnersColor = Formatting.GREEN;
  private int delay = 0;

  //Getters
  public Formatting getHuntersColor() {
    return huntersColor;
  }
  public Formatting getRunnersColor() {
    return runnersColor;
  }
  public int getDelay() {
    return delay;
  }

  //Setters
  public void setHuntersColor(Formatting color) {
    if(color == null) color = Formatting.WHITE;
    huntersColor = color;
    save();
  }
  public void setRunnersColor(Formatting color) {
    if(color == null) color = Formatting.WHITE;
    runnersColor = color;
    save();
  }
  public void setDelay(int time) {
    delay = time;
    save();
  }

  public void load() {
    if (!confFile.exists() || confFile.length() == 0) save();
    try {
      JsonObject jo = gson.fromJson(new FileReader(confFile), JsonObject.class);
      JsonElement je;

      if((je = jo.get("huntersColor")) != null) huntersColor = Formatting.byName(je.getAsString());
      if((je = jo.get("runnersColor")) != null) runnersColor = Formatting.byName(je.getAsString());
      if((je = jo.get("compassDelay")) != null) delay = je.getAsInt();
    } catch (FileNotFoundException ex) {
      LOGGER.trace("Couldn't load configuration file", ex);
    }
  }

  public void save() {
    try {
      if (!confFile.exists()) { confFile.getParentFile().mkdirs(); confFile.createNewFile(); }
      else {
        JsonObject jo = new JsonObject();
        jo.add("_ColorsList", new JsonPrimitive(String.join(", ", Formatting.getNames(true, false))));
        jo.add("huntersColor", new JsonPrimitive(huntersColor.getName()));
        jo.add("runnersColor", new JsonPrimitive(runnersColor.getName()));
        jo.add("compassDelay", new JsonPrimitive(delay));

        PrintWriter printwriter = new PrintWriter(new FileWriter(confFile));
        printwriter.print(gson.toJson(jo));
        printwriter.close();
      }
    } catch (IOException ex) {
      LOGGER.trace("Couldn't save configuration file", ex);
    }
  }
}
