package com.example.chess.renderer3d

import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.tan

class Matrix4 {
    val m = FloatArray(16)

    init {
        setIdentity()
    }

    fun setIdentity() {
        for (i in 0..15) m[i] = 0f
        m[0] = 1f; m[5] = 1f; m[10] = 1f; m[15] = 1f
    }

    operator fun times(o: Matrix4): Matrix4 {
        val res = Matrix4()
        for (row in 0..3) {
            for (col in 0..3) {
                var sum = 0f
                for (k in 0..3) {
                    sum += m[row * 4 + k] * o.m[k * 4 + col]
                }
                res.m[row * 4 + col] = sum
            }
        }
        return res
    }

    fun transformPoint(v: Vector3): Vector3 {
        val x = v.x * m[0] + v.y * m[1] + v.z * m[2] + m[3]
        val y = v.x * m[4] + v.y * m[5] + v.z * m[6] + m[7]
        val z = v.x * m[8] + v.y * m[9] + v.z * m[10] + m[11]
        val w = v.x * m[12] + v.y * m[13] + v.z * m[14] + m[15]
        return if (w != 0f && w != 1f) Vector3(x / w, y / w, z / w) else Vector3(x, y, z)
    }

    fun transformVector(v: Vector3): Vector3 {
        val x = v.x * m[0] + v.y * m[1] + v.z * m[2]
        val y = v.x * m[4] + v.y * m[5] + v.z * m[6]
        val z = v.x * m[8] + v.y * m[9] + v.z * m[10]
        return Vector3(x, y, z)
    }

    companion object {
        fun identity(): Matrix4 = Matrix4()

        fun translation(tx: Float, ty: Float, tz: Float): Matrix4 {
            val res = Matrix4()
            res.m[3] = tx
            res.m[7] = ty
            res.m[11] = tz
            return res
        }

        fun scale(sx: Float, sy: Float, sz: Float): Matrix4 {
            val res = Matrix4()
            res.m[0] = sx
            res.m[5] = sy
            res.m[10] = sz
            return res
        }

        fun rotationY(angleRad: Float): Matrix4 {
            val res = Matrix4()
            val c = cos(angleRad)
            val s = sin(angleRad)
            res.m[0] = c; res.m[2] = s
            res.m[8] = -s; res.m[10] = c
            return res
        }

        fun rotationX(angleRad: Float): Matrix4 {
            val res = Matrix4()
            val c = cos(angleRad)
            val s = sin(angleRad)
            res.m[5] = c; res.m[6] = -s
            res.m[9] = s; res.m[10] = c
            return res
        }

        fun rotationZ(angleRad: Float): Matrix4 {
            val res = Matrix4()
            val c = cos(angleRad)
            val s = sin(angleRad)
            res.m[0] = c; res.m[1] = -s
            res.m[4] = s; res.m[5] = c
            return res
        }

        fun lookAt(eye: Vector3, target: Vector3, up: Vector3): Matrix4 {
            val zAxis = (eye - target).normalize()
            val xAxis = up.cross(zAxis).normalize()
            val yAxis = zAxis.cross(xAxis).normalize()

            val res = Matrix4()
            res.m[0] = xAxis.x; res.m[1] = xAxis.y; res.m[2] = xAxis.z; res.m[3] = -xAxis.dot(eye)
            res.m[4] = yAxis.x; res.m[5] = yAxis.y; res.m[6] = yAxis.z; res.m[7] = -yAxis.dot(eye)
            res.m[8] = zAxis.x; res.m[9] = zAxis.y; res.m[10] = zAxis.z; res.m[11] = -zAxis.dot(eye)
            res.m[12] = 0f;      res.m[13] = 0f;      res.m[14] = 0f;      res.m[15] = 1f
            return res
        }

        fun perspective(fovYDeg: Float, aspect: Float, near: Float, far: Float): Matrix4 {
            val f = (1.0 / tan(Math.toRadians(fovYDeg.toDouble() / 2.0))).toFloat()
            val res = Matrix4()
            res.m[0] = f / aspect
            res.m[5] = f
            res.m[10] = (far + near) / (near - far)
            res.m[11] = (2 * far * near) / (near - far)
            res.m[14] = -1f
            res.m[15] = 0f
            return res
        }
    }
}
