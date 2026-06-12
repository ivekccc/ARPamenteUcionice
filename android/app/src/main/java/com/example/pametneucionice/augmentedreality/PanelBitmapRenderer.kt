package com.example.pametneucionice.augmentedreality

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Bitmap
import android.graphics.Canvas
import android.view.LayoutInflater
import android.view.View
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.example.pametneucionice.R
import com.example.pametneucionice.databinding.ViewRoomPanelBinding
import com.example.pametneucionice.model.RoomResponse
import com.example.pametneucionice.model.SensorStatus
import java.util.Locale

class PanelBitmapRenderer {

    fun render(context: Context, room: RoomResponse): Bitmap {
        val binding = ViewRoomPanelBinding.inflate(LayoutInflater.from(context))

        binding.panelRoomName.text = room.roomName

        val occupiedUntil = room.occupiedUntil
        val currentClassName = room.currentClassName
        if (room.occupied && occupiedUntil != null && currentClassName != null) {
            binding.panelOccupancyLine.text = "Zauzeta do $occupiedUntil — $currentClassName"
            binding.panelOccupancyLine.setTextColor(
                ContextCompat.getColor(context, R.color.panelOccupancyText)
            )
        } else {
            binding.panelOccupancyLine.text = "Slobodna"
            binding.panelOccupancyLine.setTextColor(
                ContextCompat.getColor(context, R.color.panelFreeText)
            )
        }

        binding.panelTemperatureValue.text =
            String.format(Locale.US, "%.1f", room.temperatureCelsius)
        applyStatus(context, binding.panelTemperatureDot, binding.panelTemperatureValue, room.temperatureStatus)

        binding.panelNoiseValue.text = String.format(Locale.US, "%.0f", room.noiseDecibels)
        applyStatus(context, binding.panelNoiseDot, binding.panelNoiseValue, room.noiseStatus)

        binding.panelCarbonDioxideValue.text =
            String.format(Locale.US, "%.0f", room.carbonDioxidePpm)
        applyStatus(context, binding.panelCarbonDioxideDot, binding.panelCarbonDioxideValue, room.airQualityStatus)

        binding.panelRecommendation.text = room.recommendation
        val anyCritical = room.temperatureStatus == SensorStatus.CRITICAL ||
                room.noiseStatus == SensorStatus.CRITICAL ||
                room.airQualityStatus == SensorStatus.CRITICAL
        val recommendationColor = if (anyCritical) {
            R.color.panelCriticalRecommendationText
        } else {
            R.color.panelLabelText
        }
        binding.panelRecommendation.setTextColor(
            ContextCompat.getColor(context, recommendationColor)
        )

        val panelView = binding.root
        panelView.measure(
            View.MeasureSpec.makeMeasureSpec(BITMAP_WIDTH, View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(BITMAP_HEIGHT, View.MeasureSpec.EXACTLY)
        )
        panelView.layout(0, 0, BITMAP_WIDTH, BITMAP_HEIGHT)

        val bitmap = Bitmap.createBitmap(BITMAP_WIDTH, BITMAP_HEIGHT, Bitmap.Config.ARGB_8888)
        panelView.draw(Canvas(bitmap))
        return bitmap
    }

    private fun applyStatus(
        context: Context,
        dot: View,
        value: TextView,
        status: SensorStatus
    ) {
        val dotColor = when (status) {
            SensorStatus.OK -> R.color.statusOk
            SensorStatus.WARNING -> R.color.statusWarning
            SensorStatus.CRITICAL -> R.color.statusCritical
        }
        val valueColor = when (status) {
            SensorStatus.OK -> R.color.panelValueOk
            SensorStatus.WARNING -> R.color.panelValueWarning
            SensorStatus.CRITICAL -> R.color.panelValueCritical
        }
        dot.backgroundTintList =
            ColorStateList.valueOf(ContextCompat.getColor(context, dotColor))
        value.setTextColor(ContextCompat.getColor(context, valueColor))
    }

    companion object {
        const val BITMAP_WIDTH = 1024
        const val BITMAP_HEIGHT = 768
    }
}
