package com.example.androidproject1.feature.inventory.presentation.component

/**
 * The key the inventory row and the item detail share: one string per element, built from
 * the id, so a row and the screen it opens name the same thing and no other row does. The
 * catalog's own pattern.
 *
 * Not composables, so not a component — but here rather than in a screen file, because a screen
 * file holds the screen and its previews and nothing else.
 *
 * **The owner's avatar, not a picture.** The brief asked for the row's picture to travel; the row
 * has none — its leading is `AppAvatar(item.owner)`, the owner's initials, and only the detail
 * shows `item.imageUrl`. What both screens genuinely have is that avatar, so that is what moves,
 * and it grows from a row's leading to the detail's owner line.
 */
fun itemOwnerKey(id: String): String = "item/$id/owner"
