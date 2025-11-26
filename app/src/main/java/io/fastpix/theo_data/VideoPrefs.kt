package io.fastpix.theo_data

import android.os.Parcelable
import kotlinx.parcelize.Parcelize


val dummyData = listOf<DummyData>(
    DummyData(
        "Big Buck Bunny",
        "https://cdn.theoplayer.com/video/big_buck_bunny/big_buck_bunny_metadata.m3u8",
    ),
    DummyData(
        "Star Wars Episode",
        "https://cdn.theoplayer.com/video/star_wars_episode_vii-the_force_awakens_official_comic-con_2015_reel_(2015)/index.m3u8",
    ),
    DummyData(
        "Tears Of Steel",
        "https://cdn.theoplayer.com/video/tears_of_steel/index.m3u8",
    ),
)


@Parcelize
data class DummyData(
    var id: String,
    var url: String,
) : Parcelable