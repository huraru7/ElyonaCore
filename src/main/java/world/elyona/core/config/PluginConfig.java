package world.elyona.core.config;

import org.bukkit.configuration.file.FileConfiguration;
import world.elyona.core.ElyonaCorePlugin;

public class PluginConfig {

    private final ElyonaCorePlugin plugin;

    // Database
    private String dbType;
    private String sqliteFile;
    private String mysqlHost;
    private int mysqlPort;
    private String mysqlDatabase;
    private String mysqlUsername;
    private String mysqlPassword;

    // Economy
    private String currencyName;
    private String currencySymbol;
    private long initialGrant;
    private double transactionTax;

    // MIMIC
    private String mimicPrefixColor;
    private String mimicTextColor;

    public PluginConfig(ElyonaCorePlugin plugin) {
        this.plugin = plugin;
        plugin.saveDefaultConfig();
        reload();
    }

    public void reload() {
        plugin.reloadConfig();
        FileConfiguration cfg = plugin.getConfig();

        dbType = cfg.getString("database.type", "sqlite");
        sqliteFile = cfg.getString("database.sqlite.file", "elyona_data.db");
        mysqlHost = cfg.getString("database.mysql.host", "localhost");
        mysqlPort = cfg.getInt("database.mysql.port", 3306);
        mysqlDatabase = cfg.getString("database.mysql.database", "elyona");
        mysqlUsername = cfg.getString("database.mysql.username", "root");
        mysqlPassword = cfg.getString("database.mysql.password", "");

        currencyName = cfg.getString("economy.currency_name", "Cred");
        currencySymbol = cfg.getString("economy.currency_symbol", "Cr");
        initialGrant = cfg.getLong("economy.initial_grant", 500L);
        transactionTax = cfg.getDouble("economy.transaction_tax", 0.05);

        mimicPrefixColor = cfg.getString("mimic.prefix_color", "#7F77DD");
        mimicTextColor = cfg.getString("mimic.text_color", "#D3D1C7");
    }

    public boolean isMySql() {
        return "mysql".equalsIgnoreCase(dbType);
    }

    public String getSqliteFile() { return sqliteFile; }
    public String getMysqlHost() { return mysqlHost; }
    public int getMysqlPort() { return mysqlPort; }
    public String getMysqlDatabase() { return mysqlDatabase; }
    public String getMysqlUsername() { return mysqlUsername; }
    public String getMysqlPassword() { return mysqlPassword; }
    public String getCurrencyName() { return currencyName; }
    public String getCurrencySymbol() { return currencySymbol; }
    public long getInitialGrant() { return initialGrant; }
    public double getTransactionTax() { return transactionTax; }
    public String getMimicPrefixColor() { return mimicPrefixColor; }
    public String getMimicTextColor() { return mimicTextColor; }
}
