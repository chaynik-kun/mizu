package chaynik.mizu.domain.models.settings

enum class AppIconVariant(
	val activityName: String,
	val iconResourceName: String
) {
	Original("MainActivityOriginal", "ic_launcher"),
	Monochrome("MainActivityMonochrome", "ic_launcher_monochrome"),
	Black("MainActivityBlack", "ic_launcher_black"),
	White("MainActivityWhite", "ic_launcher_white"),
	AppleMusic("MainActivityAppleMusic", "ic_launcher_apple_music"),
	Purple("MainActivityPurple", "ic_launcher_purple"),
	Yellow("MainActivityYellow", "ic_launcher_yellow"),
	Green("MainActivityGreen", "ic_launcher_green"),
	Brown("MainActivityBrown", "ic_launcher_brown")
}
