# FastPix Resumable Uploads SDK - Documentation PR

## Documentation Changes

### What Changed
- [ ] New documentation added
- [ ] Existing documentation updated
- [ ] Documentation errors fixed
- [ ] Code examples updated
- [ ] Links and references updated

### Files Modified
- [ ] README.md
- [ ] docs/ files
- [ ] USAGE.md
- [ ] CONTRIBUTING.md
- [ ] Other: _______________

### Summary
**Brief description of changes:**

<!-- What documentation was added, updated, or fixed? -->

### Code Examples
```kotlin 
// If you added/updated code examples, include them here
private var theoPlayerData: FastPixBaseTheoPlayer? = null
val videoDataDetails = VideoDataDetails(
    videoId = UUID.randomUUID().toString(),
    videoTitle = videoModel?.id,
    videoSeries = "Video Series",
    videoProducer = "Video Producer",
    videoContentType = "Video Content Type",
    videoVariant = "Video Variant",
    videoLanguage = "Video Language",
    videoDrmType = "Drm Type"
)
val customerData = CustomerData(
    workSpaceId = "workspace-id",
    videoDetails = videoDataDetails,
    playerData = PlayerDataDetails("Theo-player", "5+"),
    customDataDetails = CustomDataDetails(
        customField1 = "field1",
        customField2 = "field2",
        // Add more custom fields as needed
    )
)
theoPlayerData = FastPixBaseTheoPlayer(
    this, binding.theoPlayerView,
    customerData = customerData,
    enableLogging = true
)
```

### Testing
- [ ] All code examples tested
- [ ] Links verified
- [ ] Grammar checked
- [ ] Formatting consistent

### Review Checklist
- [ ] Content is accurate
- [ ] Code examples work
- [ ] Links are working
- [ ] Grammar is correct
- [ ] Formatting is consistent

---

**Ready for review!**
