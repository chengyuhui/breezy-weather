package org.breezyweather.remoteviews.presenters

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.view.View
import android.widget.RemoteViews
import androidx.annotation.LayoutRes
import breezyweather.domain.location.model.Location
import org.breezyweather.R
import org.breezyweather.background.receiver.widget.WidgetComplicationCurrentProvider
import org.breezyweather.common.basic.models.options.NotificationTextColor
import org.breezyweather.domain.location.model.isDaylight
import org.breezyweather.settings.SettingsManager
import org.breezyweather.theme.resource.ResourceHelper
import org.breezyweather.theme.resource.ResourcesProviderFactory

class ComplicationCurrentWidgetIMP: AbstractRemoteViewsPresenter() {

    companion object {

        fun isEnabled(context: Context): Boolean {
            return AppWidgetManager.getInstance(context).getAppWidgetIds(
                ComponentName(context, WidgetComplicationCurrentProvider::class.java)
            ).isNotEmpty()
        }

        fun updateWidgetView(context: Context, location: Location?) {
            AppWidgetManager.getInstance(context).updateAppWidget(
                ComponentName(context, WidgetComplicationCurrentProvider::class.java),
                buildRemoteViews(context, location, R.layout.widget_complication_current)
            )
        }
    }
}

private fun buildRemoteViews(
    context: Context,
    location: Location?,
    @LayoutRes layoutId: Int,
): RemoteViews {

    val views = RemoteViews(context.packageName, layoutId)

    val weather = location?.weather ?: return views

    val provider = ResourcesProviderFactory.newInstance

    val settings = SettingsManager.getInstance(context)
    val temperatureUnit = settings.temperatureUnit

    // current.
    weather.current?.weatherCode?.let {
        views.setViewVisibility(R.id.widget_complication_current_currentIcon, View.VISIBLE)
        views.setImageViewUri(
            R.id.widget_complication_current_currentIcon,
            ResourceHelper.getWidgetNotificationIconUri(
                provider,
                it,
                location.isDaylight,
                false,
                NotificationTextColor.LIGHT
            )
        )
    } ?: views.setViewVisibility(R.id.widget_complication_current_currentIcon, View.INVISIBLE)

    views.setTextViewText(
        R.id.widget_complication_current_currentTemperature,
        weather.current?.temperature?.temperature?.let {
            temperatureUnit.getShortValueText(context, it)
        }
    )

    return views
}