package com.tmstudio.obol.domain

import com.tmstudio.obol.data.db.entity.Category

/**
 * Boja monograma za ručno unesenu pretplatu, po kategoriji. Sprema se u
 * pretplatu kao hex, isto kao boja iz kataloga. Vrijednosti prate
 * `CategoryColors` u temi; kategorije bez vlastite boje dijele boju alata.
 */
fun Category.defaultColorHex(): String = when (this) {
    Category.ENTERTAINMENT -> "#FF8168"
    Category.MUSIC -> "#2FD39B"
    Category.GAMING -> "#7FB0FF"
    Category.STORAGE -> "#E8C766"
    Category.TOOLS,
    Category.EDUCATION,
    Category.FITNESS,
    Category.NEWS,
    Category.OTHER -> "#8FB8FF"
}
