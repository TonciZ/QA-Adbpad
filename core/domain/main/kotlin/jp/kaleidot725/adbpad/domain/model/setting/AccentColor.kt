package jp.kaleidot725.adbpad.domain.model.setting

import androidx.compose.ui.graphics.Color
import jp.kaleidot725.adbpad.domain.model.language.Language

// Light-theme colours are used for link text on white and as button backgrounds under white
// text. Each keeps at least 4.5:1 (WCAG AA, normal text) in every state, including when a
// hover or press layer lightens the button or tints the background behind the text.
enum class AccentColor(
    val lightColor: Color,
    val darkColor: Color,
) {
    BLUE(Color(0xFF356685), Color(0xFF7AAED4)),
    PURPLE(Color(0xFF785390), Color(0xFFB090CC)),
    GREEN(Color(0xFF376B4D), Color(0xFF78B594)),
    ORANGE(Color(0xFF895530), Color(0xFFD4A070)),
    RED(Color(0xFF9B4747), Color(0xFFD48888)),
    TEAL(Color(0xFF2E6A6A), Color(0xFF6CB0B0)),
    INDIGO(Color(0xFF5063A0), Color(0xFF909ECC)),
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
