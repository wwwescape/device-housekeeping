package com.wwwescape.devicehousekeeping.ui.navigation

import com.wwwescape.devicehousekeeping.R
import com.wwwescape.devicehousekeeping.data.scan.JunkCategory

/** Every screen reachable from the app's own navigation. [Category] takes the category's enum
 * name as its argument. */
enum class Destination(val route: String, val titleRes: Int) {
    Home("home", R.string.app_name),
    Category("category/{category}", R.string.app_name),
    AppCaches("apps", R.string.title_app_caches),
    History("history", R.string.title_history),
    Settings("settings", R.string.title_settings),
    Exclusions("exclusions", R.string.title_exclusions),
    Licenses("licenses", R.string.section_open_source_licenses);

    companion object {
        const val CATEGORY_ARG = "category"

        fun fromRoute(route: String?): Destination = entries.find { it.route == route } ?: Home

        fun categoryRoute(category: JunkCategory): String = "category/${category.name}"
    }
}
