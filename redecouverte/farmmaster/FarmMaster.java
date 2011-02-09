package redecouverte.farmmaster;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.bukkit.DyeColor;
import org.bukkit.Material;
import org.bukkit.Server;
import org.bukkit.block.Block;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.event.Event.Priority;
import org.bukkit.event.Event.Type;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.PluginDescriptionFile;
import org.bukkit.plugin.PluginLoader;
import org.bukkit.plugin.PluginManager;
import org.bukkit.util.config.Configuration;

public class FarmMaster extends JavaPlugin {

    private static final Logger logger = Logger.getLogger("Minecraft");
    private YmlDB plantDB;
    private EBlockListener mBlockListener;
    private Configuration config;

    public FarmMaster(PluginLoader pluginLoader, Server instance, PluginDescriptionFile desc, File folder, File plugin, ClassLoader cLoader) {

        super(pluginLoader, instance, desc, folder, plugin, cLoader);

        folder.mkdirs();
        
        File oldFile = new File(getDataFolder(), "plants.db");
        if (oldFile.exists()) {
            oldFile.delete();
        }
    }

    public void onEnable() {
        try {
            File configFile = new File(getDataFolder(), "config.yml");
            if (!configFile.exists()) {
                configFile.createNewFile();

                FileOutputStream fo = new FileOutputStream(configFile);
                InputStream fi = this.getClass().getResourceAsStream("/config.yml");

                try {
                    int data = fi.read();
                    while (data != -1) {
                        fo.write(data);
                        data = fi.read();
                    }
                } catch (Exception e) {
                } finally {
                    fi.close();
                    fo.close();
                }

            }

            this.config = new Configuration(configFile);
            this.config.load();

            this.plantDB = new YmlDB(this, this.getDataFolder().getCanonicalPath());

            this.getServer().getScheduler().scheduleSyncRepeatingTask(this, new PlantTimer(this), 50, 50);

            PluginManager pm = getServer().getPluginManager();

            mBlockListener = new EBlockListener(this);
            pm.registerEvent(Type.BLOCK_DAMAGED, mBlockListener, Priority.Normal, this);

            PluginDescriptionFile pdfFile = this.getDescription();
            logger.log(Level.INFO, pdfFile.getName() + " version " + pdfFile.getVersion() + " enabled.");
        } catch (Exception e) {
            logger.log(Level.WARNING, "FarmMaster error: " + e.getMessage() + e.getStackTrace().toString());
            e.printStackTrace();
            return;
        }
    }

    public void onDisable() {
        try {
            this.getServer().getScheduler().cancelTasks(this);
            this.plantDB.saveDB();

            PluginDescriptionFile pdfFile = this.getDescription();
            logger.log(Level.INFO, pdfFile.getName() + " version " + pdfFile.getVersion() + " disabled.");
        } catch (Exception e) {
            logger.log(Level.WARNING, "FarmMaster : error: " + e.getMessage() + e.getStackTrace().toString());
            e.printStackTrace();
            return;
        }
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String commandLabel, String[] args) {

        try {
            String cmd = command.getName().toLowerCase();

            if (cmd.equals("fmreload")) {
                this.config.load();
                sender.sendMessage("FarmMaster configuration reloaded.");
                return true;
            }

            return false;


        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    public YmlDB getPlantDB() {
        return this.plantDB;
    }

    public boolean addPlant(Block b, ItemStack is) {

        DyeColor col = DyeColor.getByData((byte) is.getDurability());
        String colName = col.toString().toLowerCase();
        String configNode = "plants." + colName;

        String evolveType = "";

        try {
            evolveType = this.config.getString(configNode + ".evolveto.type", "");
        } catch (Exception e) {
            evolveType = "";
        }

        if (evolveType == "") {
            return false;
        }

        int growTime;
        int evolveData;

        try {
            growTime = this.config.getInt(configNode + ".growtime", 7);
            evolveData = this.config.getInt(configNode + ".evolveto.data", 0);
        } catch (Exception e) {
            return false;
        }

        Material evolveMat = Material.matchMaterial(evolveType);
        if (evolveMat == null) {
            logger.info("FarmMaster: Unknown material: " + evolveType);
            return false;
        }

        PlantInfo newInfo = new PlantInfo(b.getWorld().getName(), b.getX(), b.getY(), b.getZ(), 0, growTime, evolveMat.getId(), evolveData);
        this.plantDB.RegisterPlant(newInfo);

        return true;
    }
}
