package com.sabah.bikhushue

import android.content.Context
import android.graphics.Color
import android.media.AudioManager
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.view.HapticFeedbackConstants
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.card.MaterialCardView

class AzkarAdapter(
    private val context: Context,
    private val azkarList: List<AzkarItem>,
    private val txtColor: Int,
    private val cardBgColor: Int,
    private val strokeColor: Int
) : RecyclerView.Adapter<AzkarAdapter.AzkarViewHolder>() {

    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
        vibratorManager.defaultVibrator
    } else {
        @Suppress("DEPRECATION")
        context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
    }

    inner class AzkarViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val cardAzkarRoot: MaterialCardView = view.findViewById(R.id.cardAzkarRoot)
        val tvAzkarTitle: TextView = view.findViewById(R.id.tvAzkarTitle)
        val tvAzkarText: TextView = view.findViewById(R.id.tvAzkarText)
        val tvAzkarVirtue: TextView = view.findViewById(R.id.tvAzkarVirtue)
        val tvAzkarCount: TextView = view.findViewById(R.id.tvAzkarCount)
        val tvAzkarDone: TextView = view.findViewById(R.id.tvAzkarDone)
        val flCountContainer: View = view.findViewById(R.id.flCountContainer)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AzkarViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_azkar, parent, false)
        return AzkarViewHolder(view)
    }

    override fun onBindViewHolder(holder: AzkarViewHolder, position: Int) {
        val item = azkarList[position]

        // Apply theme colors
        holder.cardAzkarRoot.setCardBackgroundColor(cardBgColor)
        holder.cardAzkarRoot.strokeColor = strokeColor
        holder.cardAzkarRoot.strokeWidth = 2
        
        holder.tvAzkarTitle.setTextColor(txtColor)
        holder.tvAzkarText.setTextColor(txtColor)
        
        // Populate text
        holder.tvAzkarTitle.text = item.title
        holder.tvAzkarText.text = item.text

        if (item.virtues.isNotEmpty()) {
            holder.tvAzkarVirtue.visibility = View.VISIBLE
            holder.tvAzkarVirtue.text = item.virtues
            holder.tvAzkarVirtue.setTextColor(strokeColor) // Use the accent color for virtue
        } else {
            holder.tvAzkarVirtue.visibility = View.GONE
        }

        fun updateCountUI() {
            if (item.currentCount > 0) {
                holder.tvAzkarCount.visibility = View.VISIBLE
                holder.tvAzkarDone.visibility = View.GONE
                holder.tvAzkarCount.text = item.currentCount.toString()
                holder.cardAzkarRoot.alpha = 1.0f
            } else {
                holder.tvAzkarCount.visibility = View.GONE
                holder.tvAzkarDone.visibility = View.VISIBLE
                holder.cardAzkarRoot.alpha = 0.6f // Dim when done
            }
        }
        
        updateCountUI()

        holder.cardAzkarRoot.setOnClickListener {
            if (item.currentCount > 0) {
                // Pulse animation
                holder.cardAzkarRoot.animate()
                    .scaleX(1.03f).scaleY(1.03f)
                    .setDuration(100)
                    .withEndAction {
                        holder.cardAzkarRoot.animate().scaleX(1f).scaleY(1f).setDuration(100).start()
                    }.start()

                // Image highlight
                holder.flCountContainer.animate()
                    .alpha(1.0f)
                    .setDuration(100)
                    .withEndAction {
                        holder.flCountContainer.animate().alpha(0.6f).setDuration(100).start()
                    }.start()

                item.currentCount--
                
                // Haptic feedback (wooden feel)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    vibrator.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_TICK))
                } else {
                    holder.cardAzkarRoot.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                }

                if (item.currentCount == 0) {
                    // Done sound
                    audioManager.playSoundEffect(AudioManager.FX_KEY_CLICK, 1.0f)
                    // Double vibrate for finish
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        vibrator.vibrate(VibrationEffect.createWaveform(longArrayOf(0, 30, 50, 30), -1))
                    }
                } else {
                    // Normal click sound
                    audioManager.playSoundEffect(AudioManager.FX_KEY_CLICK, 0.3f)
                }
                
                updateCountUI()
            }
        }
    }

    override fun getItemCount() = azkarList.size
}
