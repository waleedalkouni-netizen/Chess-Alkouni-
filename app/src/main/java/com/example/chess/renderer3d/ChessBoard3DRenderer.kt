package com.example.chess.renderer3d

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.sp
import com.example.chess.model.*
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sin

data class ProjectedPolygon(
    val points: List<Offset>,
    val averageZ: Float,
    val color: Color,
    val isStroke: Boolean = false,
    val strokeWidth: Float = 1f
)

@Composable
fun ChessBoard3DRenderer(
    modifier: Modifier = Modifier,
    gameState: GameState,
    camera: Camera3D,
    theme: BoardTheme,
    selectedSquare: Square?,
    validMoves: List<Square>,
    lastMove: Move?,
    onSquareSelected: (Square) -> Unit,
    onCameraRotated: (Float, Float) -> Unit,
    onCameraZoomed: (Float) -> Unit
) {
    val textMeasurer = rememberTextMeasurer()

    Canvas(
        modifier = modifier
            .fillMaxSize()
            .pointerInput(camera.isTopDown2D) {
                detectTapGestures { offset ->
                    val square = RayPicker.pickSquare(
                        touchX = offset.x,
                        touchY = offset.y,
                        screenWidth = size.width.toFloat(),
                        screenHeight = size.height.toFloat(),
                        camera = camera
                    )
                    square?.let { onSquareSelected(it) }
                }
            }
            .pointerInput(camera.isTopDown2D) {
                detectTransformGestures { _, pan, zoom, _ ->
                    if (!camera.isTopDown2D) {
                        if (abs(pan.x) > 0.5f || abs(pan.y) > 0.5f) {
                            val sens = 0.4f
                            onCameraRotated(pan.x * sens, -pan.y * sens)
                        }
                        if (zoom != 1.0f) {
                            val zoomFactor = if (zoom > 1.0f) 0.95f else 1.05f
                            onCameraZoomed(zoomFactor)
                        }
                    }
                }
            }
    ) {
        val width = size.width
        val height = size.height
        if (width <= 0 || height <= 0) return@Canvas

        val viewProj = camera.getProjectionMatrix(width, height) * camera.viewMatrix
        val projectedPolygons = mutableListOf<ProjectedPolygon>()

        // Colors from theme
        val lightTileCol = Color(theme.lightSquareColor)
        val darkTileCol = Color(theme.darkSquareColor)
        val frameCol = Color(theme.frameColor)
        val whitePieceCol = Color(theme.whitePieceColor)
        val blackPieceCol = Color(theme.blackPieceColor)
        val whiteAccentCol = Color(theme.whiteAccentColor)
        val blackAccentCol = Color(theme.blackAccentColor)

        // Light direction for 3D shading
        val lightDir = Vector3(0.5f, 1.0f, 0.7f).normalize()
        val eyePos = camera.eyePosition

        // 1. Render Wooden/Marble Frame Base
        renderBoardFrame(
            viewProj = viewProj,
            width = width,
            height = height,
            frameColor = frameCol,
            lightDir = lightDir,
            outList = projectedPolygons
        )

        // 2. Render 64 Board Squares
        renderBoardTiles(
            viewProj = viewProj,
            width = width,
            height = height,
            lightColor = lightTileCol,
            darkColor = darkTileCol,
            lightDir = lightDir,
            selectedSquare = selectedSquare,
            validMoves = validMoves,
            lastMove = lastMove,
            isCheck = gameState.isCheck,
            activeKingSquare = if (gameState.isCheck) findKingSquare(gameState, gameState.activeColor) else null,
            outList = projectedPolygons
        )

        // 3. Render 3D Pieces & Shadows
        for (file in 0..7) {
            for (rank in 0..7) {
                val piece = gameState.board[file][rank] ?: continue
                val worldX = file - 3.5f
                val worldZ = 3.5f - rank

                val isSelected = selectedSquare == Square(file, rank)

                val baseColor = if (piece.color == PieceColor.WHITE) whitePieceCol else blackPieceCol
                val accentColor = if (piece.color == PieceColor.WHITE) whiteAccentCol else blackAccentCol

                // Render Soft Shadow on board plane Y=0
                renderPieceShadow(
                    file = file,
                    rank = rank,
                    pieceType = piece.type,
                    viewProj = viewProj,
                    width = width,
                    height = height,
                    outList = projectedPolygons
                )

                // Render 3D Piece Mesh
                renderPieceMesh3D(
                    file = file,
                    rank = rank,
                    piece = piece,
                    worldX = worldX,
                    worldZ = worldZ,
                    isSelected = isSelected,
                    viewProj = viewProj,
                    width = width,
                    height = height,
                    baseColor = baseColor,
                    accentColor = accentColor,
                    lightDir = lightDir,
                    eyePos = eyePos,
                    outList = projectedPolygons
                )
            }
        }

        // 4. Sort polygons by depth (Painter's Algorithm: back to front)
        projectedPolygons.sortByDescending { it.averageZ }

        // 5. Draw Polygons to Screen
        for (poly in projectedPolygons) {
            if (poly.points.size < 3) continue
            val path = Path().apply {
                moveTo(poly.points[0].x, poly.points[0].y)
                for (i in 1 until poly.points.size) {
                    lineTo(poly.points[i].x, poly.points[i].y)
                }
                close()
            }
            if (poly.isStroke) {
                drawPath(path = path, color = poly.color, style = Stroke(width = poly.strokeWidth))
            } else {
                drawPath(path = path, color = poly.color)
            }
        }

        // 6. Draw Coordinate Rank ('1'-'8') and File ('A'-'H') Labels
        drawBoardLabels(
            viewProj = viewProj,
            width = width,
            height = height,
            camera = camera,
            textMeasurer = textMeasurer,
            textColor = if (theme == BoardTheme.CYBER_NEON) Color(0xFF00F0FF) else Color(0xFFD4AF37)
        )
    }
}

private fun renderBoardFrame(
    viewProj: Matrix4,
    width: Float,
    height: Float,
    frameColor: Color,
    lightDir: Vector3,
    outList: MutableList<ProjectedPolygon>
) {
    val margin = 4.4f
    val depth = 0.5f

    // Top face of outer border frame
    val topQuad = listOf(
        Vector3(-margin, 0f, -margin),
        Vector3(margin, 0f, -margin),
        Vector3(margin, 0f, margin),
        Vector3(-margin, 0f, margin)
    )
    val projTop = projectPoints(topQuad, viewProj, width, height)
    if (projTop != null) {
        val norm = Vector3(0f, 1f, 0f)
        val intensity = (norm.dot(lightDir) * 0.4f + 0.6f).coerceIn(0.2f, 1.0f)
        val col = multiplyColor(frameColor, intensity)
        outList.add(ProjectedPolygon(projTop.first, projTop.second, col))
    }

    // Side bevel faces
    val sideFaces = listOf(
        // Front side (+Z)
        listOf(Vector3(-margin, 0f, margin), Vector3(margin, 0f, margin), Vector3(margin, -depth, margin), Vector3(-margin, -depth, margin)) to Vector3(0f, -0.5f, 0.866f),
        // Right side (+X)
        listOf(Vector3(margin, 0f, -margin), Vector3(margin, 0f, margin), Vector3(margin, -depth, margin), Vector3(margin, -depth, -margin)) to Vector3(0.866f, -0.5f, 0f),
        // Back side (-Z)
        listOf(Vector3(-margin, 0f, -margin), Vector3(margin, 0f, -margin), Vector3(margin, -depth, -margin), Vector3(-margin, -depth, -margin)) to Vector3(0f, -0.5f, -0.866f),
        // Left side (-X)
        listOf(Vector3(-margin, 0f, -margin), Vector3(-margin, 0f, margin), Vector3(-margin, -depth, margin), Vector3(-margin, -depth, -margin)) to Vector3(-0.866f, -0.5f, 0f)
    )

    for ((quad, norm) in sideFaces) {
        val proj = projectPoints(quad, viewProj, width, height) ?: continue
        val intensity = (norm.dot(lightDir) * 0.5f + 0.5f).coerceIn(0.15f, 1.0f)
        val col = multiplyColor(frameColor, intensity * 0.75f)
        outList.add(ProjectedPolygon(proj.first, proj.second, col))
    }
}

private fun renderBoardTiles(
    viewProj: Matrix4,
    width: Float,
    height: Float,
    lightColor: Color,
    darkColor: Color,
    lightDir: Vector3,
    selectedSquare: Square?,
    validMoves: List<Square>,
    lastMove: Move?,
    isCheck: Boolean,
    activeKingSquare: Square?,
    outList: MutableList<ProjectedPolygon>
) {
    for (file in 0..7) {
        for (rank in 0..7) {
            val sq = Square(file, rank)
            val x1 = file - 4.0f
            val x2 = file - 3.0f
            val z1 = 4.0f - rank
            val z2 = 3.0f - rank

            val quad = listOf(
                Vector3(x1, 0f, z1),
                Vector3(x2, 0f, z1),
                Vector3(x2, 0f, z2),
                Vector3(x1, 0f, z2)
            )

            val proj = projectPoints(quad, viewProj, width, height) ?: continue

            val isLight = (file + rank) % 2 != 0
            var baseCol = if (isLight) lightColor else darkColor

            // Highlights
            if (selectedSquare == sq) {
                baseCol = Color(0xFFFFD700) // Gold active highlight
            } else if (lastMove?.from == sq || lastMove?.to == sq) {
                baseCol = blendColor(baseCol, Color(0xFF38BDF8), 0.45f) // Cyan move trail
            } else if (isCheck && activeKingSquare == sq) {
                baseCol = Color(0xFFEF4444) // Bright Red Check Warning
            } else if (validMoves.contains(sq)) {
                baseCol = blendColor(baseCol, Color(0xFF22C55E), 0.5f) // Emerald valid target
            }

            val norm = Vector3(0f, 1f, 0f)
            val intensity = (norm.dot(lightDir) * 0.35f + 0.65f).coerceIn(0.3f, 1.0f)
            val finalCol = multiplyColor(baseCol, intensity)

            outList.add(ProjectedPolygon(proj.first, proj.second, finalCol))

            // Tile Border outline
            val borderCol = Color.Black.copy(alpha = 0.15f)
            outList.add(ProjectedPolygon(proj.first, proj.second - 0.001f, borderCol, isStroke = true, strokeWidth = 1.5f))
        }
    }
}

private fun renderPieceShadow(
    file: Int,
    rank: Int,
    pieceType: PieceType,
    viewProj: Matrix4,
    width: Float,
    height: Float,
    outList: MutableList<ProjectedPolygon>
) {
    val x = file - 3.5f
    val z = 3.5f - rank
    val radius = when (pieceType) {
        PieceType.PAWN -> 0.28f
        PieceType.KNIGHT, PieceType.BISHOP -> 0.32f
        PieceType.ROOK -> 0.35f
        PieceType.QUEEN, PieceType.KING -> 0.38f
    }

    val shadowQuad = listOf(
        Vector3(x - radius, 0.002f, z - radius),
        Vector3(x + radius, 0.002f, z - radius),
        Vector3(x + radius, 0.002f, z + radius),
        Vector3(x - radius, 0.002f, z + radius)
    )

    val proj = projectPoints(shadowQuad, viewProj, width, height) ?: return
    val shadowCol = Color.Black.copy(alpha = 0.35f)
    outList.add(ProjectedPolygon(proj.first, proj.second - 0.005f, shadowCol))
}

private fun renderPieceMesh3D(
    file: Int,
    rank: Int,
    piece: ChessPiece,
    worldX: Float,
    worldZ: Float,
    isSelected: Boolean,
    viewProj: Matrix4,
    width: Float,
    height: Float,
    baseColor: Color,
    accentColor: Color,
    lightDir: Vector3,
    eyePos: Vector3,
    outList: MutableList<ProjectedPolygon>
) {
    val mesh = Piece3DMesh.getMesh(piece.type)
    val liftY = if (isSelected) 0.35f else 0f

    // Piece orientation: White faces north (-Z), Black faces south (+Z)
    val rotY = if (piece.color == PieceColor.WHITE) 0f else Math.PI.toFloat()
    val cosRot = cos(rotY); val sinRot = sin(rotY)

    for (poly in mesh) {
        val worldVertices = mutableListOf<Vector3>()
        val worldNormals = mutableListOf<Vector3>()

        for (v in poly.vertices) {
            // Rotate local vertex
            val rx = v.position.x * cosRot - v.position.z * sinRot
            val rz = v.position.x * sinRot + v.position.z * cosRot
            val pos = Vector3(worldX + rx, v.position.y + liftY, worldZ + rz)

            // Rotate normal
            val rnx = v.normal.x * cosRot - v.normal.z * sinRot
            val rnz = v.normal.x * sinRot + v.normal.z * cosRot
            val norm = Vector3(rnx, v.normal.y, rnz).normalize()

            worldVertices.add(pos)
            worldNormals.add(norm)
        }

        val proj = projectPoints(worldVertices, viewProj, width, height) ?: continue

        // Phong Lighting: Ambient + Diffuse + Specular
        val faceNormal = (worldNormals[0] + worldNormals[min(1, worldNormals.size - 1)]).normalize()
        val diffuse = max(0f, faceNormal.dot(lightDir))

        // View direction for Specular highlight
        val viewDir = (eyePos - worldVertices[0]).normalize()
        val reflectDir = (lightDir * -1f) - (faceNormal * (2f * (lightDir * -1f).dot(faceNormal)))
        val specular = max(0f, viewDir.dot(reflectDir)).pow(16f)

        val ambient = 0.35f
        val lightIntensity = (ambient + diffuse * 0.65f).coerceIn(0.2f, 1.0f)

        var finalColor = multiplyColor(if (isSelected) accentColor else baseColor, lightIntensity)

        // Add specular gold / white sheen
        if (specular > 0.4f) {
            finalColor = blendColor(finalColor, Color.White, specular * 0.5f)
        }

        outList.add(ProjectedPolygon(proj.first, proj.second, finalColor))
    }
}

private fun projectPoints(
    points: List<Vector3>,
    viewProj: Matrix4,
    width: Float,
    height: Float
): Pair<List<Offset>, Float>? {
    val projected = mutableListOf<Offset>()
    var zSum = 0f

    for (v in points) {
        // Matrix multiplication
        val p = viewProj.transformPoint(v)

        // Screen mapping
        val sx = (p.x + 1f) * 0.5f * width
        val sy = (1f - p.y) * 0.5f * height

        projected.add(Offset(sx, sy))
        zSum += p.z
    }

    val avgZ = zSum / points.size
    return Pair(projected, avgZ)
}

private fun DrawScope.drawBoardLabels(
    viewProj: Matrix4,
    width: Float,
    height: Float,
    camera: Camera3D,
    textMeasurer: androidx.compose.ui.text.TextMeasurer,
    textColor: Color
) {
    // Rank 1-8 labels along left & right edges
    val ranks = listOf("1", "2", "3", "4", "5", "6", "7", "8")
    val files = listOf("A", "B", "C", "D", "E", "F", "G", "H")

    val textStyle = TextStyle(color = textColor, fontSize = 12.sp, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)

    for (r in 0..7) {
        val pos = Vector3(-4.25f, 0.05f, 3.5f - r)
        val p = viewProj.transformPoint(pos)
        val sx = (p.x + 1f) * 0.5f * width
        val sy = (1f - p.y) * 0.5f * height
        drawText(textMeasurer = textMeasurer, text = ranks[r], topLeft = Offset(sx, sy), style = textStyle)
    }

    for (f in 0..7) {
        val pos = Vector3(f - 3.5f, 0.05f, 4.25f)
        val p = viewProj.transformPoint(pos)
        val sx = (p.x + 1f) * 0.5f * width
        val sy = (1f - p.y) * 0.5f * height
        drawText(textMeasurer = textMeasurer, text = files[f], topLeft = Offset(sx, sy), style = textStyle)
    }
}

private fun findKingSquare(state: GameState, color: PieceColor): Square? {
    for (f in 0..7) {
        for (r in 0..7) {
            val p = state.board[f][r]
            if (p != null && p.type == PieceType.KING && p.color == color) {
                return Square(f, r)
            }
        }
    }
    return null
}

private fun multiplyColor(color: Color, factor: Float): Color {
    val f = factor.coerceIn(0f, 1.5f)
    return Color(
        red = (color.red * f).coerceIn(0f, 1f),
        green = (color.green * f).coerceIn(0f, 1f),
        blue = (color.blue * f).coerceIn(0f, 1f),
        alpha = color.alpha
    )
}

private fun blendColor(base: Color, overlay: Color, weight: Float): Color {
    val w = weight.coerceIn(0f, 1f)
    return Color(
        red = base.red * (1f - w) + overlay.red * w,
        green = base.green * (1f - w) + overlay.green * w,
        blue = base.blue * (1f - w) + overlay.blue * w,
        alpha = base.alpha
    )
}
