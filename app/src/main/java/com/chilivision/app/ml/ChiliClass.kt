package com.chilivision.app.ml

enum class ChiliClass(val label: String) {
    BACTERIAL("Bacterial"),   // 0
    FUNGAL("Fungal"),         // 1
    HEALTHY("Healthy"),       // 2
    NON_CHILI("NonChili"),    // 3
    PESTS("Pests"),           // 4
    VIRUS("Virus");           // 5

    companion object {
        fun fromOrdinal(ordinal: Int): ChiliClass? {
            return values().getOrNull(ordinal)
        }
    }
}
