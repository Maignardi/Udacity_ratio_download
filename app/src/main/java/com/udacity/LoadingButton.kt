package com.udacity

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View
import androidx.core.content.ContextCompat
import kotlin.properties.Delegates

class LoadingButton @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private var widthSize = 0
    private var heightSize = 0
    private var buttonText: String
    private val defaultButtonText = "DOWNLOAD"
    private var progress = 0f
    private val valueAnimator = ValueAnimator.ofFloat(0f, 1f).apply {
        duration = 1000
        repeatMode = ValueAnimator.RESTART
        repeatCount = ValueAnimator.INFINITE
        addUpdateListener {
            progress = it.animatedValue as Float
            invalidate()
        }
    }

    var buttonState: ButtonState by Delegates.observable<ButtonState>(ButtonState.Completed) { _, _, new ->
        when (new) {
            ButtonState.Loading -> {
                valueAnimator.start()
            }
            ButtonState.Completed -> {
                valueAnimator.cancel()
                progress = 0f
            }
            else -> {}
        }
        invalidate()
    }

    private val backgroundPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = ContextCompat.getColor(context, R.color.colorPrimary)
    }

    private val loadingPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = ContextCompat.getColor(context, R.color.colorPrimaryDark)
    }

    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = ContextCompat.getColor(context, R.color.white)
        textSize = 50f
    }

    init {
        context.theme.obtainStyledAttributes(
            attrs,
            R.styleable.LoadingButton,
            0, 0
        ).apply {
            try {
                buttonText = getString(R.styleable.LoadingButton_buttonText) ?: defaultButtonText
                backgroundPaint.color = getColor(R.styleable.LoadingButton_buttonBackground, ContextCompat.getColor(context, R.color.colorPrimary))
            } finally {
                recycle()
            }
        }

        isClickable = true
        setOnClickListener {
            if (buttonState != ButtonState.Loading) {
                buttonState = ButtonState.Loading
            }
        }
    }

    private val circleProgressPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = ContextCompat.getColor(context, R.color.colorAccent)
        style = Paint.Style.FILL
    }

    fun updateDownloadProgress(progress: Float) {
        this.progress = progress
        invalidate()
    }

    fun stopLoading() {
        buttonState = ButtonState.Completed
    }

    override fun onDraw(canvas: Canvas?) {
        super.onDraw(canvas)
        canvas?.let {
            it.drawRect(0f, 0f, widthSize.toFloat(), heightSize.toFloat(), backgroundPaint)
            val progressWidth = progress * widthSize
            it.drawRect(0f, 0f, progressWidth, heightSize.toFloat(), loadingPaint)
            val text = when (buttonState) {
                ButtonState.Loading -> "WE ARE LOADING..."
                else -> buttonText
            }
            val textWidth = textPaint.measureText(text)
            val x = (widthSize - textWidth) / 2
            val y = (heightSize + textPaint.textSize) / 2
            it.drawText(text, x, y, textPaint)

            if (buttonState == ButtonState.Loading) {
                val circleRadius = 30f
                val circleCenterX = x + textWidth + 2 * circleRadius
                val circleCenterY = heightSize / 2f
                val angle = 360 * progress
                it.drawArc(
                    circleCenterX - circleRadius, circleCenterY - circleRadius,
                    circleCenterX + circleRadius, circleCenterY + circleRadius,
                    -90f, angle, true, circleProgressPaint
                )
            }
        }
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val minw: Int = paddingLeft + paddingRight + suggestedMinimumWidth
        val w: Int = resolveSizeAndState(minw, widthMeasureSpec, 1)
        val h: Int = resolveSizeAndState(
            MeasureSpec.getSize(w),
            heightMeasureSpec,
            0
        )
        widthSize = w
        heightSize = h
        setMeasuredDimension(w, h)
    }
}
