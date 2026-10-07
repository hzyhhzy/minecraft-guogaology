package dev.guogaology.survival;

public enum SurvivalTheme {
    MATRIX("ordinal_crags","matrix",0x91C3ED), POWER("power_desert","power",0xF0AD59),
    HYDRA("epsilon_meadow","hydra",0xAEEAC2), ABSENCE("lho_absence","absence",0xCCB6EA),
    WEAVER("laver_tablelands","weaver",0x78D7D7), ASTRA("astra_awakening","astra",0xB9F3DC),
    GUOGAO("guogao_forest","guogao",0xE7AB72), FRONTIER("limit_highlands","frontier",0xD4BE87);
    public final String biome,id; public final int color;
    SurvivalTheme(String biome,String id,int color) {this.biome=biome;this.id=id;this.color=color;}
    public boolean underworld() {return this==GUOGAO;}
    public static SurvivalTheme byIndex(int i) {return values()[Math.floorMod(i,values().length)];}
}
