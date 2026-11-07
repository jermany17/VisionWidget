package app.visionov.visionwidget.widget

import android.content.Context
import android.graphics.Bitmap

/**
 * A dressed card drawn for somewhere that is not a widget.
 *
 * The setting screen the launcher opens has to show what each choice will actually look
 * like, and the only honest way to do that is to draw the real card with the user's real
 * words — the same renderer, the same style, the same data the widget would read. A
 * mocked-up picture would drift from the card the moment Studio was touched.
 *
 * Null when the size has nothing of that kind to draw, which never happens today: all
 * three cards exist at all three sizes.
 */
internal suspend fun renderCardPreview(
    context: Context,
    slot: Slot,
    size: WidgetSize,
    widthPx: Int,
    heightPx: Int
): Bitmap? {
    val sidePx = minOf(widthPx, heightPx)

    return when (slot) {
        Slot.Vision -> visionPayload(context, size).let { card ->
            when (size) {
                WidgetSize.Small -> renderVisionCardSmall(
                    context, sidePx, card.style, card.photo, card.goal, card.weeksLine
                )

                WidgetSize.Medium -> renderVisionCard(
                    context, widthPx, heightPx, card.style, card.photo,
                    card.goal, card.why, card.targetLine, card.weeksLine
                )

                WidgetSize.Large -> renderVisionCardLarge(
                    context, widthPx, heightPx, card.style, card.photo,
                    card.goal, card.why, card.milestones, card.targetLine, card.weeksLine
                )
            }
        }

        Slot.TopThree -> topThreePayload(context, size).let { card ->
            when (size) {
                WidgetSize.Small -> renderTopThreeCardSmall(
                    context, sidePx, card.style, card.photo, card.tasks, card.checked
                )

                WidgetSize.Medium -> renderTopThreeCard(
                    context, widthPx, heightPx, card.style, card.photo, card.tasks, card.checked
                )

                WidgetSize.Large -> renderTopThreeCardLarge(
                    context, widthPx, heightPx, card.style, card.photo, card.tasks, card.checked,
                    card.streakLine.orEmpty(), card.weekLine.orEmpty()
                )
            }
        }

        Slot.Wisdom -> wisdomPayload(context).let { card ->
            when (size) {
                WidgetSize.Small -> renderWisdomCardSmall(
                    context, sidePx, card.style, card.photo, card.quote, card.category
                )

                WidgetSize.Medium -> renderWisdomCard(
                    context, widthPx, heightPx, card.style, card.photo, card.quote, card.category
                )

                WidgetSize.Large -> renderWisdomCardLarge(
                    context, widthPx, heightPx, card.style, card.photo, card.quote, card.category
                )
            }
        }

        // A Gallery design is drawn by the shelf's own renderer, not this one.
        is Slot.Design -> null
    }
}
