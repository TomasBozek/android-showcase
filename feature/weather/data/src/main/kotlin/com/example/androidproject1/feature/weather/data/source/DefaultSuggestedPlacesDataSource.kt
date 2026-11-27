package com.example.androidproject1.feature.weather.data.source

import com.example.androidproject1.feature.weather.domain.Place

/**
 * Thirty places the screen offers before anyone has typed anything.
 *
 * Bundled Kotlin rather than a raw JSON resource: a typed row is the compiler's to check and a
 * JSON row is nobody's, thirty rows need no parser, and there is no way for this to fail at
 * launch. The id, the coordinates and the timezone are the dev fixture's, so a suggestion tapped
 * offline resolves to the same forecast the geocoder would have found for it.
 *
 * The order is deliberate — the Czech towns, then the near neighbours, then the far capitals —
 * because it is the order they are drawn in, and a list nobody scrolls past the tenth row of
 * should have the likely ones at the top. Where a name is shared with a place abroad (Paris,
 * London, Praha), only the first is here: a list to pick from is not the place to disambiguate,
 * and the search is.
 */
class DefaultSuggestedPlacesDataSource : SuggestedPlacesDataSource {

    override fun suggestedPlaces(): List<Place> = PLACES

    private companion object {

        val PLACES = listOf(
            Place(3067696, "Praha", "Česko", "Praha", 50.08804, 14.42076, "Europe/Prague"),
            Place(3078610, "Brno", "Česko", "Jihomoravský kraj", 49.19522, 16.60796, "Europe/Prague"),
            Place(3068799, "Ostrava", "Česko", "Moravskoslezský kraj", 49.83465, 18.28204, "Europe/Prague"),
            Place(3068160, "Plzeň", "Česko", "Plzeňský kraj", 49.74747, 13.37759, "Europe/Prague"),
            Place(3069011, "Olomouc", "Česko", "Olomoucký kraj", 49.59552, 17.25175, "Europe/Prague"),
            Place(3069935, "Liberec", "Česko", "Liberecký kraj", 50.76711, 15.05619, "Europe/Prague"),
            Place(3077916, "České Budějovice", "Česko", "Jihočeský kraj", 48.97447, 14.47434, "Europe/Prague"),
            Place(3073803, "Hradec Králové", "Česko", "Královéhradecký kraj", 50.20923, 15.83277, "Europe/Prague"),
            Place(3063739, "Ústí nad Labem", "Česko", "Ústecký kraj", 50.66078, 14.03227, "Europe/Prague"),
            Place(3068582, "Pardubice", "Česko", "Pardubický kraj", 50.04075, 15.77659, "Europe/Prague"),
            Place(3061344, "Zlín", "Česko", "Zlínský kraj", 49.22645, 17.66556, "Europe/Prague"),
            Place(3074199, "Karlovy Vary", "Česko", "Karlovarský kraj", 50.23271, 12.87117, "Europe/Prague"),
            Place(3060972, "Bratislava", "Slovensko", "Bratislavský kraj", 48.14816, 17.10674, "Europe/Bratislava"),
            Place(723819, "Košice", "Slovensko", "Košický kraj", 48.71395, 21.25808, "Europe/Bratislava"),
            Place(2761369, "Wien", "Österreich", "Wien", 48.20849, 16.37208, "Europe/Vienna"),
            Place(2950159, "Berlin", "Deutschland", "Berlin", 52.52437, 13.41053, "Europe/Berlin"),
            Place(2867714, "München", "Deutschland", "Bayern", 48.13743, 11.57549, "Europe/Berlin"),
            Place(3094802, "Kraków", "Polska", "Małopolskie", 50.06143, 19.93658, "Europe/Warsaw"),
            Place(756135, "Warszawa", "Polska", "Mazowieckie", 52.22977, 21.01178, "Europe/Warsaw"),
            Place(3054643, "Budapest", "Magyarország", "Budapest", 47.49801, 19.03991, "Europe/Budapest"),
            Place(2657896, "Zürich", "Schweiz", "Zürich", 47.36667, 8.55, "Europe/Zurich"),
            Place(2759794, "Amsterdam", "Nederland", "Noord-Holland", 52.37403, 4.88969, "Europe/Amsterdam"),
            Place(2988507, "Paris", "France", "Île-de-France", 48.85341, 2.3488, "Europe/Paris"),
            Place(2643743, "London", "United Kingdom", "England", 51.50853, -0.12574, "Europe/London"),
            Place(3169070, "Roma", "Italia", "Lazio", 41.89193, 12.51133, "Europe/Rome"),
            Place(3117735, "Madrid", "España", "Comunidad de Madrid", 40.4165, -3.70256, "Europe/Madrid"),
            Place(2267057, "Lisboa", "Portugal", "Lisboa", 38.71667, -9.13333, "Europe/Lisbon"),
            Place(2618425, "København", "Danmark", "Hovedstaden", 55.67594, 12.56553, "Europe/Copenhagen"),
            Place(2673730, "Stockholm", "Sverige", "Stockholms län", 59.33258, 18.0649, "Europe/Stockholm"),
            Place(5128581, "New York", "United States", "New York", 40.71427, -74.00597, "America/New_York"),
        )
    }
}
