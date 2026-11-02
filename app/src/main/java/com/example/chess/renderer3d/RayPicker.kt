package com.example.chess.renderer3d

import com.example.chess.model.Square
import kotlin.math.abs
import kotlin.math.floor

object RayPicker {

    fun pickSquare(
        touchX: Float,
        touchY: Float,
        screenWidth: Float,
        screenHeight: Float,
        camera: Camera3D
    ): Square? {
        if (screenWidth <= 0 || screenHeight <= 0) return null

        // 1. Convert Screen space to Normalized Device Coordinates (NDC) [-1, 1]
        val ndcX = (2.0f * touchX / screenWidth) - 1.0f
        val ndcY = 1.0f - (2.0f * touchY / screenHeight)

        val viewMatrix = camera.viewMatrix
        val projMatrix = camera.getProjectionMatrix(screenWidth, screenHeight)

        // Combine View and Projection
        val viewProj = projMatrix * viewMatrix

        // Unproject near point (NDC z = -1) and far point (NDC z = 1)
        val nearPointWorld = unproject(ndcX, ndcY, -1.0f, viewProj)
        val farPointWorld = unproject(ndcX, ndcY, 1.0f, viewProj)

        val rayDir = (farPointWorld - nearPointWorld).normalize()
        val rayOrigin = nearPointWorld

        // Plane Y = 0 (Board level)
        // rayOrigin.y + t * rayDir.y = 0 => t = -rayOrigin.y / rayDir.y
        if (abs(rayDir.y) < 0.0001f) return null
        val t = -rayOrigin.y / rayDir.y
        if (t < 0) return null // behind camera

        val hitWorld = rayOrigin + (rayDir * t)

        // Board extends from X = -4.0 to +4.0, Z = -4.0 to +4.0
        // Square (0,0) is file 0, rank 0 (White bottom-left: a1).
        // File 0 is X in [-4, -3], Rank 0 is Z in [3, 4] or [-4, -3].
        // Let's establish world coordinates:
        // Center of board is (0,0,0).
        // File f (0..7): X = (f - 3.5f)
        // Rank r (0..7): Z = (3.5f - r)
        val fileF = hitWorld.x + 4.0f
        val rankF = 4.0f - hitWorld.z

        val file = floor(fileF).toInt()
        val rank = floor(rankF).toInt()

        return if (file in 0..7 && rank in 0..7) {
            Square(file, rank)
        } else {
            null
        }
    }

    private fun unproject(ndcX: Float, ndcY: Float, ndcZ: Float, viewProj: Matrix4): Vector3 {
        // Inverse matrix estimation
        val inv = invertMatrix(viewProj)
        val p = inv.transformPoint(Vector3(ndcX, ndcY, ndcZ))
        return p
    }

    private fun invertMatrix(mat: Matrix4): Matrix4 {
        val m = mat.m
        val inv = Matrix4()
        val out = inv.m

        out[0] = m[5]  * m[10] * m[15] - m[5]  * m[11] * m[14] - m[9]  * m[6]  * m[15] + m[9]  * m[7]  * m[14] + m[13] * m[6]  * m[11] - m[13] * m[7]  * m[10]
        out[4] = -m[4]  * m[10] * m[15] + m[4]  * m[11] * m[14] + m[8]  * m[6]  * m[15] - m[8]  * m[7]  * m[14] - m[12] * m[6]  * m[11] + m[12] * m[7]  * m[10]
        out[8] = m[4]  * m[9]  * m[15] - m[4]  * m[11] * m[13] - m[8]  * m[5]  * m[15] + m[8]  * m[7]  * m[13] + m[12] * m[5]  * m[11] - m[12] * m[7]  * m[9]
        out[12] = -m[4]  * m[9]  * m[14] + m[4]  * m[10] * m[13] + m[8]  * m[5]  * m[14] - m[8]  * m[6]  * m[13] - m[12] * m[5]  * m[10] + m[12] * m[6]  * m[9]

        out[1] = -m[1]  * m[10] * m[15] + m[1]  * m[11] * m[14] + m[9]  * m[2]  * m[15] - m[9]  * m[3]  * m[14] - m[13] * m[2]  * m[11] + m[13] * m[3]  * m[10]
        out[5] = m[0]  * m[10] * m[15] - m[0]  * m[11] * m[14] - m[8]  * m[2]  * m[15] + m[8]  * m[3]  * m[14] + m[12] * m[2]  * m[11] - m[12] * m[3]  * m[10]
        out[9] = -m[0]  * m[9]  * m[15] + m[0]  * m[11] * m[13] + m[8]  * m[1]  * m[15] - m[8]  * m[3]  * m[13] - m[12] * m[1]  * m[11] + m[12] * m[3]  * m[9]
        out[13] = m[0]  * m[9]  * m[14] - m[0]  * m[10] * m[13] - m[8]  * m[1]  * m[14] + m[8]  * m[2]  * m[13] + m[12] * m[1]  * m[10] - m[12] * m[2]  * m[9]

        out[2] = m[1]  * m[6]  * m[15] - m[1]  * m[7]  * m[14] - m[5]  * m[2]  * m[15] + m[5]  * m[3]  * m[14] + m[13] * m[2]  * m[7]  - m[13] * m[3]  * m[6]
        out[6] = -m[0]  * m[6]  * m[15] + m[0]  * m[7]  * m[14] + m[4]  * m[2]  * m[15] - m[4]  * m[3]  * m[14] - m[12] * m[2]  * m[7]  + m[12] * m[3]  * m[6]
        out[10] = m[0]  * m[5]  * m[15] - m[0]  * m[7]  * m[13] - m[4]  * m[1]  * m[15] + m[4]  * m[3]  * m[13] + m[12] * m[1]  * m[7]  - m[12] * m[3]  * m[5]
        out[14] = -m[0]  * m[5]  * m[14] + m[0]  * m[6]  * m[13] + m[4]  * m[1]  * m[14] - m[4]  * m[2]  * m[13] - m[12] * m[1]  * m[6]  + m[12] * m[2]  * m[5]

        out[3] = -m[1]  * m[6]  * m[11] + m[1]  * m[7]  * m[10] + m[5]  * m[2]  * m[11] - m[5]  * m[3]  * m[10] - m[9]  * m[2]  * m[7]  + m[9]  * m[3]  * m[6]
        out[7] = m[0]  * m[6]  * m[11] - m[0]  * m[7]  * m[10] - m[4]  * m[2]  * m[11] + m[4]  * m[3]  * m[10] + m[8]  * m[2]  * m[7]  - m[8]  * m[3]  * m[6]
        out[11] = -m[0]  * m[5]  * m[11] + m[0]  * m[7]  * m[9]  + m[4]  * m[1]  * m[11] - m[4]  * m[3]  * m[9]  - m[8]  * m[1]  * m[7]  + m[8]  * m[3]  * m[5]
        out[15] = m[0]  * m[5]  * m[10] - m[0]  * m[6]  * m[9]  - m[4]  * m[1]  * m[10] + m[4]  * m[2]  * m[9]  + m[8]  * m[1]  * m[6]  - m[8]  * m[2]  * m[5]

        var det = m[0] * out[0] + m[1] * out[4] + m[2] * out[8] + m[3] * out[12]
        if (det == 0f) det = 1.0f
        val invDet = 1.0f / det
        for (i in 0..15) out[i] *= invDet

        return inv
    }
}
