package kerala.superhero.app.data

data class UserProfile(
    val id: String,
    val activeServiceDetails: ServiceRequest?,
    val current_service: String?,
    val service_history_ids: List<String>,
    val assetDetails: AssetDetail
)

data class AssetDetail(val categoryDetails: AssetCategory)