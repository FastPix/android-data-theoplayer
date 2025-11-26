package io.fastpix.theo_player_data

import io.fastpix.data.domain.model.CustomDataDetails
import io.fastpix.data.domain.model.PlayerDataDetails
import io.fastpix.data.domain.model.VideoDataDetails

data class CustomerData(
    val workSpaceId: String,
    val beaconUrl: String? = null,
    val videoDetails: VideoDataDetails? = null,
    val playerData: PlayerDataDetails? = PlayerDataDetails("theo-player", "5+"),
    val customDataDetails: CustomDataDetails? = null
)
