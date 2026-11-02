package com.example.chess.renderer3d

import com.example.chess.model.PieceType
import kotlin.math.cos
import kotlin.math.sin

data class Vertex3D(
    val position: Vector3,
    val normal: Vector3
)

data class Polygon3D(
    val vertices: List<Vertex3D>
)

object Piece3DMesh {

    private val meshCache = mutableMapOf<PieceType, List<Polygon3D>>()

    fun getMesh(type: PieceType): List<Polygon3D> {
        return meshCache.getOrPut(type) { generateMeshForType(type) }
    }

    private fun generateMeshForType(type: PieceType): List<Polygon3D> {
        val polygons = mutableListOf<Polygon3D>()
        val segments = 12 // Radial resolution for 3D smoothness

        when (type) {
            PieceType.PAWN -> {
                // Base, stem, neck collar, head sphere
                val profile = listOf(
                    0.40f to 0.00f,
                    0.38f to 0.08f,
                    0.28f to 0.16f,
                    0.18f to 0.40f,
                    0.22f to 0.50f,
                    0.15f to 0.54f,
                    0.25f to 0.70f,
                    0.22f to 0.85f,
                    0.00f to 0.95f
                )
                generateRevolvedMesh(profile, segments, polygons)
            }
            PieceType.KNIGHT -> {
                // Base & stem
                val baseProfile = listOf(
                    0.42f to 0.00f,
                    0.40f to 0.08f,
                    0.30f to 0.16f,
                    0.25f to 0.35f,
                    0.28f to 0.42f
                )
                generateRevolvedMesh(baseProfile, segments, polygons)
                // Knight Horse head geometry
                generateHorseHead(polygons)
            }
            PieceType.BISHOP -> {
                val profile = listOf(
                    0.42f to 0.00f,
                    0.40f to 0.08f,
                    0.30f to 0.16f,
                    0.20f to 0.45f,
                    0.26f to 0.55f,
                    0.18f to 0.60f,
                    0.24f to 0.82f,
                    0.18f to 0.95f,
                    0.05f to 1.05f,
                    0.00f to 1.10f
                )
                generateRevolvedMesh(profile, segments, polygons)
            }
            PieceType.ROOK -> {
                val profile = listOf(
                    0.45f to 0.00f,
                    0.42f to 0.08f,
                    0.32f to 0.16f,
                    0.28f to 0.65f,
                    0.36f to 0.72f,
                    0.36f to 0.95f
                )
                generateRevolvedMesh(profile, segments, polygons)
                // Castle crenellations top cap
                generateRookCrenellations(polygons, segments)
            }
            PieceType.QUEEN -> {
                val profile = listOf(
                    0.46f to 0.00f,
                    0.44f to 0.08f,
                    0.34f to 0.16f,
                    0.22f to 0.60f,
                    0.30f to 0.70f,
                    0.20f to 0.76f,
                    0.32f to 1.05f,
                    0.22f to 1.15f,
                    0.06f to 1.25f,
                    0.00f to 1.30f
                )
                generateRevolvedMesh(profile, segments, polygons)
            }
            PieceType.KING -> {
                val profile = listOf(
                    0.48f to 0.00f,
                    0.45f to 0.08f,
                    0.35f to 0.16f,
                    0.24f to 0.65f,
                    0.32f to 0.75f,
                    0.22f to 0.82f,
                    0.34f to 1.10f,
                    0.24f to 1.22f,
                    0.10f to 1.30f
                )
                generateRevolvedMesh(profile, segments, polygons)
                // 3D Cross on top of King
                generateKingCross(polygons)
            }
        }

        return polygons
    }

    private fun generateRevolvedMesh(
        profile: List<Pair<Float, Float>>,
        segments: Int,
        outPolygons: MutableList<Polygon3D>
    ) {
        val angleStep = (2.0 * Math.PI / segments).toFloat()

        for (i in 0 until profile.size - 1) {
            val (r1, y1) = profile[i]
            val (r2, y2) = profile[i + 1]

            // Calculate profile segment normal
            val dr = r2 - r1
            val dy = y2 - y1
            val normalY = dr
            val normalR = -dy

            for (seg in 0 until segments) {
                val a1 = seg * angleStep
                val a2 = (seg + 1) * angleStep

                val cos1 = cos(a1); val sin1 = sin(a1)
                val cos2 = cos(a2); val sin2 = sin(a2)

                val v1 = Vector3(r1 * cos1, y1, r1 * sin1)
                val v2 = Vector3(r2 * cos1, y2, r2 * sin1)
                val v3 = Vector3(r2 * cos2, y2, r2 * sin2)
                val v4 = Vector3(r1 * cos2, y1, r1 * sin2)

                val n1 = Vector3(normalR * cos1, normalY, normalR * sin1).normalize()
                val n2 = Vector3(normalR * cos2, normalY, normalR * sin2).normalize()

                outPolygons.add(
                    Polygon3D(
                        listOf(
                            Vertex3D(v1, n1),
                            Vertex3D(v2, n1),
                            Vertex3D(v3, n2),
                            Vertex3D(v4, n2)
                        )
                    )
                )
            }
        }
    }

    private fun generateHorseHead(outPolygons: MutableList<Polygon3D>) {
        // Detailed 3D polygonal horse head
        val headY = 0.42f
        val headH = 0.55f
        val width = 0.22f
        val depth = 0.35f

        // Box & snout geometry
        val p1 = Vector3(-width, headY, -depth * 0.3f)
        val p2 = Vector3(width, headY, -depth * 0.3f)
        val p3 = Vector3(width, headY + headH, -depth * 0.5f)
        val p4 = Vector3(-width, headY + headH, -depth * 0.5f)

        val snoutFrontLower = Vector3(0f, headY + 0.2f, depth * 0.6f)
        val snoutFrontUpper = Vector3(0f, headY + 0.38f, depth * 0.5f)

        val nForward = Vector3(0f, 0.3f, 0.95f).normalize()
        val nLeft = Vector3(-1f, 0f, 0f)
        val nRight = Vector3(1f, 0f, 0f)
        val nUp = Vector3(0f, 1f, 0f)

        // Snout faces
        outPolygons.add(Polygon3D(listOf(Vertex3D(p1, nLeft), Vertex3D(snoutFrontLower, nLeft), Vertex3D(snoutFrontUpper, nLeft), Vertex3D(p4, nLeft))))
        outPolygons.add(Polygon3D(listOf(Vertex3D(p2, nRight), Vertex3D(p3, nRight), Vertex3D(snoutFrontUpper, nRight), Vertex3D(snoutFrontLower, nRight))))

        // Top mane crest
        val maneTop = Vector3(0f, headY + headH + 0.15f, -depth * 0.4f)
        outPolygons.add(Polygon3D(listOf(Vertex3D(p4, nUp), Vertex3D(p3, nUp), Vertex3D(maneTop, nUp))))
    }

    private fun generateRookCrenellations(outPolygons: MutableList<Polygon3D>, segments: Int) {
        val yBase = 0.95f
        val yTop = 1.10f
        val r = 0.36f
        val angleStep = (2.0 * Math.PI / segments).toFloat()

        for (seg in 0 until segments step 2) {
            val a1 = seg * angleStep
            val a2 = (seg + 1) * angleStep

            val c1 = cos(a1); val s1 = sin(a1)
            val c2 = cos(a2); val s2 = sin(a2)

            val v1 = Vector3(r * c1, yBase, r * s1)
            val v2 = Vector3(r * c1, yTop, r * s1)
            val v3 = Vector3(r * c2, yTop, r * s2)
            val v4 = Vector3(r * c2, yBase, r * s2)

            val norm = Vector3((c1 + c2) / 2f, 0f, (s1 + s2) / 2f).normalize()
            outPolygons.add(Polygon3D(listOf(Vertex3D(v1, norm), Vertex3D(v2, norm), Vertex3D(v3, norm), Vertex3D(v4, norm))))
        }
    }

    private fun generateKingCross(outPolygons: MutableList<Polygon3D>) {
        val yBase = 1.30f
        val crossH = 0.22f
        val crossW = 0.12f
        val thick = 0.04f

        val nFront = Vector3(0f, 0f, 1f)

        // Vertical bar
        val v1 = Vector3(-thick, yBase, 0f)
        val v2 = Vector3(-thick, yBase + crossH, 0f)
        val v3 = Vector3(thick, yBase + crossH, 0f)
        val v4 = Vector3(thick, yBase, 0f)
        outPolygons.add(Polygon3D(listOf(Vertex3D(v1, nFront), Vertex3D(v2, nFront), Vertex3D(v3, nFront), Vertex3D(v4, nFront))))

        // Horizontal bar
        val h1 = Vector3(-crossW, yBase + crossH * 0.6f, 0f)
        val h2 = Vector3(-crossW, yBase + crossH * 0.8f, 0f)
        val h3 = Vector3(crossW, yBase + crossH * 0.8f, 0f)
        val h4 = Vector3(crossW, yBase + crossH * 0.6f, 0f)
        outPolygons.add(Polygon3D(listOf(Vertex3D(h1, nFront), Vertex3D(h2, nFront), Vertex3D(h3, nFront), Vertex3D(h4, nFront))))
    }
}
