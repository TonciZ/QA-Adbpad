package jp.kaleidot725.adbpad.domain.model.setting

import androidx.compose.ui.graphics.Color
import jp.kaleidot725.adbpad.domain.model.language.Language

// Light-theme colours are used for link text on white and as button backgrounds under white
// text, so each one keeps at least 4.5:1 contrast against white (WCAG AA for normal text).
enum class AccentColor(
    val lightColor: Color,
    val darkColor: Color,
) {
    BLUE(Color(0xFF3B7396), Color(0xFF7AAED4)),
    PURPLE(Color(0xFF8B60A8), Color(0xFFA888C4)),
    GREEN(Color(0xFF3F7B59), Color(0xFF78B594)),
    ORANGE(Color(0xFF9E6237), Color(0xFFD4A070)),
    RED(Color(0xFFB85555), Color(0xFFD48888)),
    TEAL(Color(0xFF347979), Color(0xFF6CB0B0)),
    INDIGO(Color(0xFF5063A0), Color(0xFF8896C4)),
    MONO(Color(0xFF000000), Color(0xFFFFFFFF)),
    ;

    fun getColor(isLight: Boolean): Color = if (isLight) lightColor else darkColor

    fun getTitle(): String =
        when (this) {
            BLUE -> Language.accentColorBlue
            PURPLE -> Language.accentColorPurple
            GREEN -> Language.accentColorGreen
            ORANGE -> Language.accentColorOrange
            RED -> Language.accentColorRed
            TEAL -> Language.accentColorTeal
            INDIGO -> Language.accentColorIndigo
            MONO -> Language.accentColorMono
        }
}
