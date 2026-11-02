package com.example.chess.renderer3d

import kotlin.math.cos
import kotlin.math.sin

data class Camera3D(
    var yawDeg: Float = 0f,       // Azimuth angle in degrees (0 = White side, 180 = Black side)
    var pitchDeg: Float = 42f,    // Elevation angle in degrees (15 = low angle, 85 = top down)
    var distance: Float = 14f,    // Orbit radius distance from center
    var target: Vector3 = Vector3(0f, 0f, 0f),
    var isTopDown2D: Boolean = false
) {
    val eyePosition: Vector3
        get() {
            if (isTopDown2D) {
                return Vector3(target.x, distance * 1.1f, target.z + 0.01f)
            }
            val radYaw = Math.toRadians(yawDeg.toDouble()).toFloat()
            val radPitch = Math.toRadians(pitchDeg.toDouble()).toFloat()

            val x = target.x + distance * cos(radPitch) * sin(radYaw)
            val y = target.y + distance * sin(radPitch)
            val z = target.z + distance * cos(radPitch) * cos(radYaw)
            return Vector3(x, y, z)
        }

    val viewMatrix: Matrix4
        get() {
            val up = if (isTopDown2D) Vector3(0f, 0f, -1f) else Vector3(0f, 1f, 0f)
            return Matrix4.lookAt(eyePosition, target, up)
        }

    fun getProjectionMatrix(width: Float, height: Float): Matrix4 {
        val aspect = if (height > 0) width / height else 1.0f
        return Matrix4.perspective(45f, aspect, 0.1f, 100f)
    }

    fun rotate(deltaYaw: Float, deltaPitch: Float) {
        if (isTopDown2D) return
        yawDeg = (yawDeg + deltaYaw) % 360f
        if (yawDeg < 0) yawDeg += 360f
        pitchDeg = (pitchDeg + deltaPitch).coerceIn(15f, 85f)
    }

    fun zoom(factor: Float) {
        distance = (distance * factor).coerceIn(7f, 25f)
    }

    fun resetToWhite() {
        yawDeg = 0f
        pitchDeg = 42f
        distance = 14f
        isTopDown2D = false
    }

    fun resetToBlack() {
        yawDeg = 180f
        pitchDeg = 42f
        distance = 14f
        isTopDown2D = false
    }

    fun toggle2D() {
        isTopDown2D = !isTopDown2D
    }
}
