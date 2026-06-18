package com.arata.yukarilauncher.feature.download

import com.arata.yukarilauncher.R
import com.arata.yukarilauncher.feature.download.enums.Category
import com.arata.yukarilauncher.feature.download.enums.Classify
import com.arata.yukarilauncher.feature.download.enums.ModLoader
import com.arata.yukarilauncher.feature.download.enums.Platform

data class FilterOption(
    val nameResId: Int,
    val categories: List<Category>,
    val children: List<FilterOption>? = null,
    val modLoader: ModLoader? = null
)

data class FilterSection(
    val titleResId: Int,
    val options: List<FilterOption>
)

object FilterConfig {

    fun getSections(platform: Platform, classify: Classify): List<FilterSection> {
        return when (platform) {
            Platform.CURSEFORGE -> {
                val sections = mutableListOf<FilterSection>()
                if (classify == Classify.MOD || classify == Classify.MODPACK) {
                    sections.add(getModloaderSection())
                }
                sections.addAll(getCurseForgeSections(classify))
                sections
            }
            Platform.MODRINTH -> {
                val sections = mutableListOf<FilterSection>()
                if (classify == Classify.MOD || classify == Classify.MODPACK) {
                    sections.add(getModloaderSection())
                }
                sections.addAll(getModrinthSections(classify))
                sections
            }
        }
    }

    private fun getModloaderSection(): FilterSection {
        return FilterSection(R.string.download_ui_modloader, modloaderOptions)
    }

    private fun getCurseForgeSections(classify: Classify): List<FilterSection> {
        return when (classify) {
            Classify.MOD -> listOf(
                FilterSection(R.string.download_ui_category, curseforgeModCategories)
            )
            Classify.MODPACK -> listOf(
                FilterSection(R.string.download_ui_category, curseforgeModpackCategories)
            )
            Classify.RESOURCE_PACK -> listOf(
                FilterSection(R.string.download_ui_category, curseforgeResourcePackCategories)
            )
            Classify.WORLD -> listOf(
                FilterSection(R.string.download_ui_category, curseforgeWorldCategories)
            )
            Classify.SHADER_PACK -> listOf(
                FilterSection(R.string.download_ui_category, curseforgeShaderCategories)
            )
            else -> emptyList()
        }
    }

    private fun getModrinthSections(classify: Classify): List<FilterSection> {
        return when (classify) {
            Classify.MOD -> listOf(
                FilterSection(R.string.download_ui_category, modrinthModCategories),
                FilterSection(R.string.download_ui_environment, modrinthEnvironment)
            )
            Classify.MODPACK -> listOf(
                FilterSection(R.string.download_ui_category, modrinthModpackCategories),
                FilterSection(R.string.download_ui_environment, modrinthEnvironment)
            )
            Classify.RESOURCE_PACK -> listOf(
                FilterSection(R.string.download_ui_category, modrinthResourcePackCategories),
                FilterSection(R.string.category_rp_feature, modrinthResourcePackFeatures),
                FilterSection(R.string.download_ui_resolution, modrinthResolution)
            )
            Classify.WORLD -> emptyList()
            Classify.SHADER_PACK -> listOf(
                FilterSection(R.string.download_ui_category, modrinthShaderCategories),
                FilterSection(R.string.category_shader_feature, modrinthShaderFeatures),
                FilterSection(R.string.category_shader_performance, modrinthShaderPerformance),
                FilterSection(R.string.download_ui_shader_loader, modrinthShaderLoaders)
            )
            else -> emptyList()
        }
    }

    // ============== CurseForge Mods ==============
    private val curseforgeModCategories = listOf(
        FilterOption(R.string.category_addons, listOf(Category.MOD_ADDONS), listOf(
            FilterOption(R.string.category_ae2, listOf(Category.MOD_AE2)),
            FilterOption(R.string.category_blood_magic, listOf(Category.MOD_BLOOD_MAGIC)),
            FilterOption(R.string.category_buildcraft, listOf(Category.MOD_BUILDCRAFT)),
            FilterOption(R.string.category_crafttweaker, listOf(Category.MOD_CRAFTTWEAKER)),
            FilterOption(R.string.category_create, listOf(Category.MOD_CREATE)),
            FilterOption(R.string.category_farmers_delight, listOf(Category.MOD_FARMERS_DELIGHT)),
            FilterOption(R.string.category_forestry, listOf(Category.MOD_FORESTRY)),
            FilterOption(R.string.category_galacticraft, listOf(Category.MOD_GALACTICRAFT)),
            FilterOption(R.string.category_industrial_craft, listOf(Category.MOD_INDUSTRIAL_CRAFT)),
            FilterOption(R.string.category_integrated_dynamics, listOf(Category.MOD_INTEGRATED_DYNAMICS)),
            FilterOption(R.string.category_kubejs, listOf(Category.MOD_KUBEJS)),
            FilterOption(R.string.category_refined_storage, listOf(Category.MOD_REFINED_STORAGE)),
            FilterOption(R.string.category_skyblock, listOf(Category.MOD_SKYBLOCK)),
            FilterOption(R.string.category_thaumcraft, listOf(Category.MOD_THAUMCRAFT)),
            FilterOption(R.string.category_thermal_expansion, listOf(Category.MOD_THERMAL_EXPANSION)),
            FilterOption(R.string.category_tinkers_construct, listOf(Category.MOD_TINKERS_CONSTRUCT)),
            FilterOption(R.string.category_twilight_forest, listOf(Category.MOD_TWILIGHT_FOREST)),
        )),
        FilterOption(R.string.category_adventure, listOf(Category.MOD_ADVENTURE)),
        FilterOption(R.string.category_api_and_library, listOf(Category.MOD_LIBRARY)),
        FilterOption(R.string.category_armor_tools_weapons, listOf(Category.MOD_EQUIPMENT)),
        FilterOption(R.string.category_bug_fixes, listOf(Category.MOD_BUG_FIXES)),
        FilterOption(R.string.category_cosmetic, listOf(Category.MOD_DECORATION)),
        FilterOption(R.string.category_creative_mode, listOf(Category.MOD_CREATIVE_MODE)),
        FilterOption(R.string.category_education, listOf(Category.MOD_EDUCATION)),
        FilterOption(R.string.category_food, listOf(Category.MOD_FOOD)),
        FilterOption(R.string.category_horror, listOf(Category.MOD_HORROR)),
        FilterOption(R.string.category_magic, listOf(Category.MOD_MAGIC)),
        FilterOption(R.string.category_map_and_info, listOf(Category.MOD_INFORMATION)),
        FilterOption(R.string.category_mcreator, listOf(Category.MOD_MCREATOR)),
        FilterOption(R.string.category_miscellaneous, listOf(Category.MOD_MISCELLANEOUS)),
        FilterOption(R.string.category_modjam, listOf(Category.MOD_MODJAM)),
        FilterOption(R.string.category_performance, listOf(Category.MOD_PERFORMANCE)),
        FilterOption(R.string.category_redstone, listOf(Category.MOD_REDSTONE)),
        FilterOption(R.string.category_server_utility, listOf(Category.MOD_SOCIAL)),
        FilterOption(R.string.category_storage, listOf(Category.MOD_STORAGE)),
        FilterOption(R.string.category_technology, listOf(Category.MOD_TECHNOLOGY), listOf(
            FilterOption(R.string.category_automation, listOf(Category.MOD_AUTOMATION)),
            FilterOption(R.string.category_energy, listOf(Category.MOD_ENERGY)),
            FilterOption(R.string.category_item_fluid_energy_transport, listOf(Category.MOD_ITEM_FLUID_ENERGY_TRANSPORT)),
            FilterOption(R.string.category_farming, listOf(Category.MOD_FARMING)),
            FilterOption(R.string.category_genetics, listOf(Category.MOD_GENETICS)),
            FilterOption(R.string.category_player_transport, listOf(Category.MOD_TRANSPORT)),
            FilterOption(R.string.category_processing, listOf(Category.MOD_PROCESSING)),
        )),
        FilterOption(R.string.category_twitch_integration, listOf(Category.MOD_TWITCH_INTEGRATION)),
        FilterOption(R.string.category_utility_qol, listOf(Category.MOD_UTILITY)),
        FilterOption(R.string.category_worldgen, listOf(Category.MOD_WORLDGEN), listOf(
            FilterOption(R.string.category_biomes, listOf(Category.MOD_BIOMES)),
            FilterOption(R.string.category_dimensions, listOf(Category.MOD_DIMENSIONS)),
            FilterOption(R.string.category_mobs, listOf(Category.MOD_MOBS)),
            FilterOption(R.string.category_ores_resources, listOf(Category.MOD_ORES_RESOURCES)),
            FilterOption(R.string.category_structures, listOf(Category.MOD_STRUCTURES)),
        )),
    )

    // ============== CurseForge Modpacks ==============
    private val curseforgeModpackCategories = listOf(
        FilterOption(R.string.category_adventure, listOf(Category.MODPACK_ADVENTURE)),
        FilterOption(R.string.category_combat, listOf(Category.MODPACK_COMBAT)),
        FilterOption(R.string.category_expert, listOf(Category.MODPACK_EXPERT)),
        FilterOption(R.string.category_exploration, listOf(Category.MODPACK_EXPLORATION)),
        FilterOption(R.string.category_extra_large, listOf(Category.MODPACK_EXTRA_LARGE)),
        FilterOption(R.string.category_ftb, listOf(Category.MODPACK_FTB)),
        FilterOption(R.string.category_hardcore, listOf(Category.MODPACK_CHALLENGING)),
        FilterOption(R.string.category_horror, listOf(Category.MODPACK_HORROR)),
        FilterOption(R.string.category_magic, listOf(Category.MODPACK_MAGIC)),
        FilterOption(R.string.category_map_based, listOf(Category.MODPACK_MAP_BASED)),
        FilterOption(R.string.category_mini_game, listOf(Category.MODPACK_MINI_GAME)),
        FilterOption(R.string.category_multiplayer, listOf(Category.MODPACK_MULTIPLAYER)),
        FilterOption(R.string.category_quests, listOf(Category.MODPACK_QUESTS)),
        FilterOption(R.string.category_rlcraft, listOf(Category.MODPACK_RLCRAFT)),
        FilterOption(R.string.category_sci_fi, listOf(Category.MODPACK_SCI_FI)),
        FilterOption(R.string.category_skyblock, listOf(Category.MODPACK_SKYBLOCK)),
        FilterOption(R.string.category_small_light, listOf(Category.MODPACK_LIGHTWEIGHT)),
        FilterOption(R.string.category_technology, listOf(Category.MODPACK_TECHNOLOGY)),
        FilterOption(R.string.category_vanilla_plus, listOf(Category.MODPACK_VANILLA)),
    )

    // ============== CurseForge Resource Packs ==============
    private val curseforgeResourcePackCategories = listOf(
        FilterOption(R.string.category_16x, listOf(Category.RP_16X)),
        FilterOption(R.string.category_32x, listOf(Category.RP_32X)),
        FilterOption(R.string.category_64x, listOf(Category.RP_64X)),
        FilterOption(R.string.category_128x, listOf(Category.RP_128X)),
        FilterOption(R.string.category_256x, listOf(Category.RP_256X)),
        FilterOption(R.string.category_512x_or_higher, listOf(Category.RP_512X)),
        FilterOption(R.string.category_animated, listOf(Category.RP_ANIMATED)),
        FilterOption(R.string.category_datapacks, listOf(Category.RP_DATAPACKS)),
        FilterOption(R.string.category_font_packs, listOf(Category.RP_FONT_PACKS)),
        FilterOption(R.string.category_medieval, listOf(Category.RP_MEDIEVAL)),
        FilterOption(R.string.category_miscellaneous, listOf(Category.RP_MISCELLANEOUS)),
        FilterOption(R.string.category_mod_support, listOf(Category.RP_MOD_SUPPORT)),
        FilterOption(R.string.category_modern, listOf(Category.RP_MODERN)),
        FilterOption(R.string.category_modjam, listOf(Category.RP_MODJAM)),
        FilterOption(R.string.category_photo_realistic, listOf(Category.RP_PHOTO_REALISTIC)),
        FilterOption(R.string.category_steampunk, listOf(Category.RP_STEAMPUNK)),
        FilterOption(R.string.category_traditional, listOf(Category.RP_TRADITIONAL)),
    )

    // ============== CurseForge Worlds ==============
    private val curseforgeWorldCategories = listOf(
        FilterOption(R.string.category_world_adventure, listOf(Category.WORLD_ADVENTURE)),
        FilterOption(R.string.category_creation, listOf(Category.WORLD_CREATION)),
        FilterOption(R.string.category_game_map, listOf(Category.WORLD_GAME_MAP)),
        FilterOption(R.string.category_modded_world, listOf(Category.WORLD_MODDED_WORLD)),
        FilterOption(R.string.category_parkour, listOf(Category.WORLD_PARKOUR)),
        FilterOption(R.string.category_puzzle, listOf(Category.WORLD_PUZZLE)),
        FilterOption(R.string.category_survival, listOf(Category.WORLD_SURVIVAL)),
    )

    // ============== CurseForge Shaders ==============
    private val curseforgeShaderCategories = listOf(
        FilterOption(R.string.category_fantasy, listOf(Category.SHADER_FANTASY)),
        FilterOption(R.string.category_realistic, listOf(Category.SHADER_REALISTIC)),
        FilterOption(R.string.category_vanilla, listOf(Category.SHADER_VANILLA)),
    )

    // ============== Modrinth Mods ==============
    private val modrinthModCategories = listOf(
        FilterOption(R.string.category_adventure, listOf(Category.MOD_ADVENTURE)),
        FilterOption(R.string.category_cursed, listOf(Category.MOD_CURSED)),
        FilterOption(R.string.category_decoration, listOf(Category.MOD_DECORATION)),
        FilterOption(R.string.category_economy, listOf(Category.MOD_ECONOMY)),
        FilterOption(R.string.category_equipment, listOf(Category.MOD_EQUIPMENT)),
        FilterOption(R.string.category_food, listOf(Category.MOD_FOOD)),
        FilterOption(R.string.category_game_mechanics, listOf(Category.MOD_GAME_MECHANICS)),
        FilterOption(R.string.category_library, listOf(Category.MOD_LIBRARY)),
        FilterOption(R.string.category_magic, listOf(Category.MOD_MAGIC)),
        FilterOption(R.string.category_management, listOf(Category.MOD_MANAGEMENT)),
        FilterOption(R.string.category_minigame, listOf(Category.MOD_MINIGAME)),
        FilterOption(R.string.category_mobs, listOf(Category.MOD_MOBS)),
        FilterOption(R.string.category_optimization, listOf(Category.MOD_OPTIMIZATION)),
        FilterOption(R.string.category_social, listOf(Category.MOD_SOCIAL)),
        FilterOption(R.string.category_storage, listOf(Category.MOD_STORAGE)),
        FilterOption(R.string.category_technology, listOf(Category.MOD_TECHNOLOGY)),
        FilterOption(R.string.category_transportation, listOf(Category.MOD_TRANSPORT)),
        FilterOption(R.string.category_utility, listOf(Category.MOD_UTILITY)),
        FilterOption(R.string.category_worldgen, listOf(Category.MOD_WORLDGEN)),
    )

    // ============== Modrinth Modpacks ==============
    private val modrinthModpackCategories = listOf(
        FilterOption(R.string.category_adventure, listOf(Category.MODPACK_ADVENTURE)),
        FilterOption(R.string.category_challenging, listOf(Category.MODPACK_CHALLENGING)),
        FilterOption(R.string.category_combat, listOf(Category.MODPACK_COMBAT)),
        FilterOption(R.string.category_kitchen_sink, listOf(Category.MODPACK_KITCHEN_SINK)),
        FilterOption(R.string.category_lightweight, listOf(Category.MODPACK_LIGHTWEIGHT)),
        FilterOption(R.string.category_magic, listOf(Category.MODPACK_MAGIC)),
        FilterOption(R.string.category_multiplayer, listOf(Category.MODPACK_MULTIPLAYER)),
        FilterOption(R.string.category_optimization, listOf(Category.MODPACK_OPTIMIZATION)),
        FilterOption(R.string.category_quests, listOf(Category.MODPACK_QUESTS)),
        FilterOption(R.string.category_technology, listOf(Category.MODPACK_TECHNOLOGY)),
    )

    // ============== Modrinth Resource Packs ==============
    private val modrinthResourcePackCategories = listOf(
        FilterOption(R.string.category_combat, listOf(Category.RP_COMBAT)),
        FilterOption(R.string.category_cursed, listOf(Category.RP_CURSED)),
        FilterOption(R.string.category_decoration, listOf(Category.RP_DECORATION)),
        FilterOption(R.string.category_modded, listOf(Category.RP_MOD_SUPPORT)),
        FilterOption(R.string.category_realistic, listOf(Category.RP_PHOTO_REALISTIC)),
        FilterOption(R.string.category_simplistic, listOf(Category.RP_SIMPLISTIC)),
        FilterOption(R.string.category_themed, listOf(Category.RP_THEMED)),
        FilterOption(R.string.category_tweaks, listOf(Category.RP_TWEAKS)),
        FilterOption(R.string.category_utility, listOf(Category.RP_UTILITY)),
        FilterOption(R.string.category_vanilla_like, listOf(Category.RP_VANILLA)),
    )

    private val modrinthResourcePackFeatures = listOf(
        FilterOption(R.string.category_rp_feature_audio, listOf(Category.RP_FEATURE_AUDIO)),
        FilterOption(R.string.category_rp_feature_blocks, listOf(Category.RP_FEATURE_BLOCKS)),
        FilterOption(R.string.category_rp_feature_core_shaders, listOf(Category.RP_FEATURE_CORE_SHADERS)),
        FilterOption(R.string.category_rp_feature_entities, listOf(Category.RP_FEATURE_ENTITIES)),
        FilterOption(R.string.category_rp_feature_environment, listOf(Category.RP_FEATURE_ENVIRONMENT)),
        FilterOption(R.string.category_rp_feature_equipment, listOf(Category.RP_FEATURE_EQUIPMENT)),
        FilterOption(R.string.category_rp_feature_fonts, listOf(Category.RP_FEATURE_FONTS)),
        FilterOption(R.string.category_rp_feature_gui, listOf(Category.RP_FEATURE_GUI)),
        FilterOption(R.string.category_rp_feature_items, listOf(Category.RP_FEATURE_ITEMS)),
        FilterOption(R.string.category_rp_feature_locale, listOf(Category.RP_FEATURE_LOCALE)),
        FilterOption(R.string.category_rp_feature_models, listOf(Category.RP_FEATURE_MODELS)),
    )

    private val modrinthResolution = listOf(
        FilterOption(R.string.category_8x_or_lower, listOf(Category.RP_8X)),
        FilterOption(R.string.category_16x, listOf(Category.RP_16X)),
        FilterOption(R.string.category_32x, listOf(Category.RP_32X)),
        FilterOption(R.string.category_48x, listOf(Category.RP_48X)),
        FilterOption(R.string.category_64x, listOf(Category.RP_64X)),
        FilterOption(R.string.category_128x, listOf(Category.RP_128X)),
        FilterOption(R.string.category_256x, listOf(Category.RP_256X)),
        FilterOption(R.string.category_512x_or_higher, listOf(Category.RP_512X)),
    )

    // ============== Modrinth Shaders ==============
    private val modrinthShaderCategories = listOf(
        FilterOption(R.string.category_cartoon, listOf(Category.SHADER_CARTOON)),
        FilterOption(R.string.category_cursed, listOf(Category.SHADER_CURSED)),
        FilterOption(R.string.category_fantasy, listOf(Category.SHADER_FANTASY)),
        FilterOption(R.string.category_realistic, listOf(Category.SHADER_REALISTIC)),
        FilterOption(R.string.category_semi_realistic, listOf(Category.SHADER_SEMI_REALISTIC)),
        FilterOption(R.string.category_vanilla_like, listOf(Category.SHADER_VANILLA)),
    )

    private val modrinthShaderFeatures = listOf(
        FilterOption(R.string.category_atmosphere, listOf(Category.SHADER_ATMOSPHERE)),
        FilterOption(R.string.category_bloom, listOf(Category.SHADER_BLOOM)),
        FilterOption(R.string.category_colored_lighting, listOf(Category.SHADER_COLORED_LIGHTING)),
        FilterOption(R.string.category_foliage, listOf(Category.SHADER_FOLIAGE)),
        FilterOption(R.string.category_path_tracing, listOf(Category.SHADER_PATH_TRACING)),
        FilterOption(R.string.category_pbr, listOf(Category.SHADER_PBR)),
        FilterOption(R.string.category_reflections, listOf(Category.SHADER_REFLECTIONS)),
        FilterOption(R.string.category_shadows, listOf(Category.SHADER_SHADOWS)),
    )

    private val modrinthShaderPerformance = listOf(
        FilterOption(R.string.category_configuration_potato, listOf(Category.SHADER_POTATO)),
        FilterOption(R.string.category_configuration_low, listOf(Category.SHADER_LOW)),
        FilterOption(R.string.category_configuration_medium, listOf(Category.SHADER_MEDIUM)),
        FilterOption(R.string.category_configuration_high, listOf(Category.SHADER_HIGH)),
        FilterOption(R.string.category_screenshot, listOf(Category.SHADER_SCREENSHOT)),
    )

    private val modrinthShaderLoaders = listOf(
        FilterOption(R.string.category_shader_loader_iris, listOf(Category.SHADER_LOADER_IRIS)),
        FilterOption(R.string.category_shader_loader_optifine, listOf(Category.SHADER_LOADER_OPTIFINE)),
        FilterOption(R.string.category_shader_loader_vanilla, listOf(Category.SHADER_LOADER_VANILLA)),
        FilterOption(R.string.category_shader_loader_canvas, listOf(Category.SHADER_LOADER_CANVAS)),
    )

    // ============== Shared ==============
    private val modrinthEnvironment = listOf(
        FilterOption(R.string.category_env_client, listOf(Category.ENV_CLIENT)),
        FilterOption(R.string.category_env_server, listOf(Category.ENV_SERVER)),
    )

    private val modloaderOptions = listOf(
        FilterOption(R.string.generic_all, listOf()),
        FilterOption(R.string.mod_forge, listOf(), null, ModLoader.FORGE),
        FilterOption(R.string.mod_neoforge, listOf(), null, ModLoader.NEOFORGE),
        FilterOption(R.string.mod_fabric, listOf(), null, ModLoader.FABRIC),
        FilterOption(R.string.mod_quilt, listOf(), null, ModLoader.QUILT),
    )
}
