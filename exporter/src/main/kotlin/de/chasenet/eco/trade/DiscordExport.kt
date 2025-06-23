package de.chasenet.eco.trade

import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable
import java.time.OffsetDateTime

@Serializable
data class DiscordExport(
    val guild: DiscordGuild,
    val channel: DiscordChannel,
    val messages: List<DiscordMessage>,
    @Contextual val exportedAt: OffsetDateTime,
    val messageCount: Int,
)

@Serializable
data class DiscordGuild(
    val id: String,
    val name: String,
    val icon: String? = null,
)

@Serializable
data class DiscordChannel(
    val id: String,
    val type: String,
    val categoryId: String? = null,
    val category: String? = null,
    val name: String,
    val topic: String? = null,
)

@Serializable
data class DiscordMessage(
    val id: String,
    val type: String,
    @Contextual val timestamp: OffsetDateTime,
    val content: String,
    val author: DiscordAuthor,
    val embeds: List<DiscordEmbed> = emptyList(),
)

@Serializable
data class DiscordAuthor(
    val id: String,
    val name: String,
    val discriminator: String,
    val nickname: String? = null,
    val color: String? = null,
    val isBot: Boolean,
    val avatarUrl: String? = null,
)

@Serializable
data class DiscordEmbed(
    val title: String? = null,
    val url: String? = null,
    val timestamp: String? = null,
    val description: String? = null,
    val color: String? = null,
    val images: List<String> = emptyList(),
    val fields: List<DiscordEmbedField> = emptyList(),
)

@Serializable
data class DiscordEmbedField(
    val name: String,
    val value: String,
    val isInline: Boolean,
)
