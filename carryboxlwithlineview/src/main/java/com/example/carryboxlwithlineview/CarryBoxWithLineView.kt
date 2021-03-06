package com.example.carryboxlwithlineview

import android.view.View
import android.view.MotionEvent
import android.app.Activity
import android.graphics.Paint
import android.graphics.Color
import android.graphics.Canvas
import android.graphics.RectF
import android.content.Context
import android.util.Log

val colors : Array<Int> = arrayOf(
    "#f44336",
    "#673AB7",
    "#00C853",
    "#304FFE",
    "#BF360C"
).map {
    Color.parseColor(it)
}.toTypedArray()
val strokeFactor : Float = 90f
val sizeFactor : Float = 4.9f
val delay : Long = 20
val backColor : Int = Color.parseColor("#BDBDBD")
val parts : Int = 6
val scGap : Float = 0.02f / parts

fun Int.inverse() : Float = 1f / this
fun Float.maxScale(i : Int, n : Int) : Float = Math.max(0f, this - i * n.inverse())
fun Float.divideScale(i : Int, n : Int) : Float = Math.min(n.inverse(), maxScale(i, n)) * n
fun Float.sinify() : Float = Math.sin(this * Math.PI).toFloat()

fun Canvas.drawCarryBoxWithLine(scale : Float, w : Float, h : Float, paint : Paint) {
    val size : Float = Math.min(w, h) / sizeFactor
    val sf : Float = scale.sinify()
    val sf1 : Float = sf.divideScale(0, parts)
    val sf2 : Float = sf.divideScale(1, parts)
    val sf3 : Float = sf.divideScale(2, parts)
    val sf4 : Float = sf.divideScale(3, parts)
    val sf5 : Float = sf.divideScale(4, parts)
    Log.d("scale", "$sf1, $sf2, $sf3, $sf4")
    save()
    drawCircle(size / 2, size / 8 + (h - size / 3 - size / 4) * sf5, (size  / 8) * sf1, paint)
    save()
    translate(0f, (h - size / 3) * sf2)
    save()
    translate((w - size) * (1 - sf4), 0f)
    drawRect(RectF(size - size * sf1, 0f, size, size / 3), paint)
    restore()
    for (j in 0..1) {
        save()
        translate(0f, size / 10 + (size / 3 - size / 5) * j)
        if (sf3 > 0) {
            drawLine(0f, 0f, (w - size) * (sf3 - sf4), 0f, paint)
        }
        restore()
    }
    restore()
    restore()
}

fun Canvas.drawCBWLNode(i : Int, scale : Float, paint : Paint) {
    val w : Float = width.toFloat()
    val h : Float = height.toFloat()
    paint.color = colors[i]
    paint.strokeCap = Paint.Cap.ROUND
    paint.strokeWidth = Math.min(w, h) / strokeFactor
    drawCarryBoxWithLine(scale, w, h, paint)
}

class CarryBoxWithLineView(ctx : Context) : View(ctx) {

    private val renderer : Renderer = Renderer(this)

    override fun onDraw(canvas : Canvas) {
        renderer.render(canvas)
    }

    override fun onTouchEvent(event : MotionEvent) : Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                renderer.handleTap()
            }
        }
        return true
    }

    data class State(var scale : Float = 0f, var dir : Float = 0f, var prevScale : Float = 0f) {

        fun update(cb : (Float) -> Unit) {
            scale += scGap * dir
            if (Math.abs(scale - prevScale) > 1) {
                scale = prevScale + dir
                dir = 0f
                prevScale = scale
                cb(prevScale)
            }
        }

        fun startUpdating(cb : () -> Unit) {
            if (dir == 0f) {
                dir = 1f - 2 * prevScale
                cb()
            }
        }
    }

    data class Animator(var view : View, var animated : Boolean = false) {

        fun animate(cb : () -> Unit) {
            if (animated) {
                cb()
                try {
                    Thread.sleep(delay)
                    view.invalidate()
                } catch(ex : Exception) {

                }
            }
        }

        fun start() {
            if (!animated) {
                animated = true
                view.postInvalidate()
            }
        }

        fun stop() {
            if (animated) {
                animated = false
            }
        }
    }

    data class CBWLNode(var i : Int, val state : State = State()) {

        private var next : CBWLNode? = null
        private var prev : CBWLNode? = null

        init {
            addNeighbor()
        }

        fun addNeighbor() {
            if (i < colors.size - 1) {
                next = CBWLNode(i + 1)
                next?.prev = this
            }
        }

        fun draw(canvas : Canvas, paint : Paint) {
            canvas.drawCBWLNode(i, state.scale, paint)
        }

        fun update(cb : (Float) -> Unit) {
            state.update(cb)
        }

        fun startUpdating(cb : () -> Unit) {
            state.startUpdating(cb)
        }

        fun getNext(dir : Int, cb : () -> Unit) : CBWLNode {
            var curr : CBWLNode? = prev
            if (dir == 1) {
                curr = next
            }
            if (curr != null) {
                return curr
            }
            cb()
            return this
        }
    }

    data class CarryBoxWithLine(var i : Int) {

        private var curr : CBWLNode = CBWLNode(0)
        private var dir : Int = 1

        fun draw(canvas : Canvas, paint : Paint) {
            curr.draw(canvas, paint)
        }

        fun update(cb : (Float) -> Unit) {
            curr.update {
                curr = curr.getNext(dir) {
                    dir *= -1
                }
                cb(it)
            }
        }

        fun startUpdating(cb : () -> Unit) {
            curr.startUpdating(cb)
        }
    }

    data class Renderer(var view : CarryBoxWithLineView) {

        private val animator : Animator = Animator(view)
        private val cbwl : CarryBoxWithLine = CarryBoxWithLine(0)
        private val paint : Paint = Paint(Paint.ANTI_ALIAS_FLAG)

        fun render(canvas : Canvas) {
            canvas.drawColor(backColor)
            cbwl.draw(canvas, paint)
            animator.animate {
                cbwl.update {
                    animator.stop()
                }
            }
        }

        fun handleTap() {
            cbwl.startUpdating {
                animator.start()
            }
        }
    }

    companion object {

        fun create(activity : Activity) : CarryBoxWithLineView {
            val view : CarryBoxWithLineView = CarryBoxWithLineView(activity)
            activity.setContentView(view)
            return view
        }
    }
}