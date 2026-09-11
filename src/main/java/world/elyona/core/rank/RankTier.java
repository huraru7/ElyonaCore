package world.elyona.core.rank;

import org.bukkit.Material;

public enum RankTier {
    BRONZE   (0,      0,      null,            Material.ORANGE_STAINED_GLASS_PANE, "§6Bronze"),
    SILVER   (1_000,  500,    "rank_silver",   Material.LIGHT_GRAY_STAINED_GLASS_PANE, "§7Silver"),
    GOLD     (3_000,  1_500,  "rank_gold",     Material.YELLOW_STAINED_GLASS_PANE, "§eGold"),
    PLATINUM (7_000,  3_000,  "rank_platinum", Material.CYAN_STAINED_GLASS_PANE, "§bPlatinum"),
    DIAMOND  (15_000, 6_000,  "rank_diamond",  Material.BLUE_STAINED_GLASS_PANE, "§9Diamond"),
    LEGEND   (30_000, 15_000, "rank_legend",   Material.PURPLE_STAINED_GLASS_PANE, "§5Legend");

    /** このランクに達するために必要な累計EXP */
    public final long requiredExp;
    /** ランクアップ報酬 Cr */
    public final long crReward;
    /** ランクアップ時に付与される称号ID (null = なし) */
    public final String titleId;
    /** GUIアイコン素材 */
    public final Material iconMaterial;
    /** 表示名（カラーコード付き） */
    public final String displayName;

    RankTier(long requiredExp, long crReward, String titleId, Material iconMaterial, String displayName) {
        this.requiredExp = requiredExp;
        this.crReward = crReward;
        this.titleId = titleId;
        this.iconMaterial = iconMaterial;
        this.displayName = displayName;
    }

    /** EXPから対応するランクを返す */
    public static RankTier fromExp(long exp) {
        RankTier result = BRONZE;
        for (RankTier tier : values()) {
            if (exp >= tier.requiredExp) result = tier;
        }
        return result;
    }

    /** 次のランクを返す（LEGENDの場合はnull） */
    public RankTier next() {
        RankTier[] values = values();
        int idx = ordinal() + 1;
        return idx < values.length ? values[idx] : null;
    }

    /** 前のランクを返す（BRONZEの場合はnull） */
    public RankTier previous() {
        int idx = ordinal() - 1;
        return idx >= 0 ? values()[idx] : null;
    }
}
