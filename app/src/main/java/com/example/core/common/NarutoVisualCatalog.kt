package com.example.core.common

import androidx.annotation.DrawableRes
import com.example.R

data class NarutoArtwork(
    val resName: String,
    val title: String,
    val subtitle: String,
    val clanTag: String,
    @DrawableRes val drawableRes: Int,
    val suggestedScreen: String
)

object NarutoVisualCatalog {
    val artworks: List<NarutoArtwork> = listOf(
        NarutoArtwork(
            resName = "img_naruto_uzumaki",
            title = "Naruto Uzumaki",
            subtitle = "Hidden Leaf Rasengan Twilight",
            clanTag = "KONOHA",
            drawableRes = R.drawable.img_naruto_uzumaki,
            suggestedScreen = "HOME"
        ),
        NarutoArtwork(
            resName = "img_naruto_sage_mode",
            title = "Naruto Sage Mode",
            subtitle = "Mount Myoboku Senjutsu",
            clanTag = "SAGE",
            drawableRes = R.drawable.img_naruto_sage_mode,
            suggestedScreen = "PROGRESS"
        ),
        NarutoArtwork(
            resName = "img_naruto_kurama_mode",
            title = "Naruto Kurama Mode",
            subtitle = "Nine-Tails Chakra Link",
            clanTag = "KURAMA",
            drawableRes = R.drawable.img_naruto_kurama_mode,
            suggestedScreen = "MISSIONS"
        ),
        NarutoArtwork(
            resName = "img_sasuke_uchiha",
            title = "Sasuke Uchiha",
            subtitle = "Moonlit Shrine Chidori",
            clanTag = "UCHIHA",
            drawableRes = R.drawable.img_sasuke_uchiha,
            suggestedScreen = "MISSIONS"
        ),
        NarutoArtwork(
            resName = "img_sasuke_susanoo",
            title = "Sasuke Susanoo",
            subtitle = "Winged Violet Armor",
            clanTag = "SUSANOO",
            drawableRes = R.drawable.img_sasuke_susanoo,
            suggestedScreen = "SCHEDULE"
        ),
        NarutoArtwork(
            resName = "img_kakashi_hatake",
            title = "Kakashi Hatake",
            subtitle = "Rainy Konoha Raikiri",
            clanTag = "ANBU",
            drawableRes = R.drawable.img_kakashi_hatake,
            suggestedScreen = "SCHEDULE"
        ),
        NarutoArtwork(
            resName = "img_itachi_uchiha",
            title = "Itachi Uchiha",
            subtitle = "Crimson Moon Crows",
            clanTag = "UCHIHA",
            drawableRes = R.drawable.img_itachi_uchiha,
            suggestedScreen = "PROGRESS"
        ),
        NarutoArtwork(
            resName = "img_minato_namikaze",
            title = "Minato Namikaze",
            subtitle = "Yellow Flash Flying Raijin",
            clanTag = "HOKAGE",
            drawableRes = R.drawable.img_minato_namikaze,
            suggestedScreen = "HOME"
        ),
        NarutoArtwork(
            resName = "img_madara_uchiha",
            title = "Madara Uchiha",
            subtitle = "Crimson Eclipse Gunbai",
            clanTag = "RINNEGAN",
            drawableRes = R.drawable.img_madara_uchiha,
            suggestedScreen = "SETTINGS"
        ),
        NarutoArtwork(
            resName = "img_akatsuki_clouds",
            title = "The Akatsuki",
            subtitle = "Rain Village Crimson Clouds",
            clanTag = "AKATSUKI",
            drawableRes = R.drawable.img_akatsuki_clouds,
            suggestedScreen = "SETTINGS"
        ),
        NarutoArtwork(
            resName = "bg_ninja_village",
            title = "Hidden Leaf Panorama",
            subtitle = "Classic Konoha Horizon",
            clanTag = "VILLAGE",
            drawableRes = R.drawable.bg_ninja_village,
            suggestedScreen = "GLOBAL"
        ),
        NarutoArtwork(
            resName = "img_scroll_banner",
            title = "Shinobi Secret Scroll",
            subtitle = "Washi Training Parchment",
            clanTag = "SCROLL",
            drawableRes = R.drawable.img_scroll_banner,
            suggestedScreen = "PROGRESS"
        )
    )

    fun resolveDrawable(resName: String?): Int {
        if (resName.isNullOrBlank()) return R.drawable.img_naruto_uzumaki
        val clean = resName.removePrefix("builtin://").trim()
        return artworks.firstOrNull { it.resName.equals(clean, ignoreCase = true) }?.drawableRes
            ?: when (clean.lowercase()) {
                "img_shinobi_avatar" -> R.drawable.img_shinobi_avatar
                else -> R.drawable.img_naruto_uzumaki
            }
    }

    fun defaultResNameForScreen(screenTarget: String, themePresetName: String): String {
        val normalized = themePresetName.uppercase()
        if (screenTarget.equals("HOME", ignoreCase = true)) {
            return when (normalized) {
                "SAGE_MODE" -> "img_naruto_sage_mode"
                "KURAMA" -> "img_naruto_kurama_mode"
                "SASUKE" -> "img_sasuke_uchiha"
                "AKATSUKI" -> "img_akatsuki_clouds"
                "DARK_JAPANESE" -> "img_itachi_uchiha"
                "PREMIUM_GLASS" -> "img_minato_namikaze"
                "CLAY" -> "img_kakashi_hatake"
                else -> "img_naruto_uzumaki"
            }
        }
        return when (screenTarget.uppercase()) {
            "SCHEDULE" -> "img_kakashi_hatake"
            "MISSIONS" -> "img_naruto_kurama_mode"
            "PROGRESS", "PROFILE" -> "img_naruto_sage_mode"
            "SETTINGS" -> "img_akatsuki_clouds"
            else -> "img_naruto_uzumaki"
        }
    }
}
