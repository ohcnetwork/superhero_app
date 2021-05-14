package kerala.superhero.app.data

data class AssetGroup(
    val id: Int,
    val title: String,
    val mobility: Boolean,
    val security_level: Int,
    val categoryList: List<AssetCategory>
)

data class AssetCategory(
    val id: Int,
    val title: String,
    val mobility: Boolean,
    val security_level: Int,
    val group: Int
)

data class District(val code: String, val name: String)
data class Panchayath(val name: String, val ward_count: Int)


/**
 * Modified to show only State - Nitheesh Ag
 * 24/08/2020
 */
data class States(val id: String, val state: String)