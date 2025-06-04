package com.example.wordmap

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path // 복잡한 경로를 위해 필요할 수 있음
import android.graphics.PointF
import android.util.AttributeSet
import android.view.View
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

class PathDrawingView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private var solvedProblems: List<SolvedProblemData> = emptyList()
    private val pathPoints = mutableListOf<PointF>() // 각 단어의 위치 (점)
    private val drawnPath = Path() // 그려질 전체 경로

    private val pointPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.BLUE
        style = Paint.Style.FILL
    }

    private val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.BLUE
        strokeWidth = 5f
        style = Paint.Style.STROKE
    }

    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.BLACK
        textSize = 30f // 작은 글씨 크기
        textAlign = Paint.Align.CENTER // 텍스트를 점 중앙 기준으로 정렬
    }

    private val textOffset = 40f // 점과 텍스트 사이의 간격
    private val stepDistance = 150f // 각 이동 거리
    private val pointRadius = 10f   // 점의 반지름

    fun setSolvedProblems(problems: List<SolvedProblemData>) {
        this.solvedProblems = problems
        calculatePathPoints()
        invalidate() // 뷰를 다시 그리도록 요청
    }

// PathDrawingView.kt

    private fun calculatePathPoints() {
        pathPoints.clear()
        drawnPath.reset()

        if (width == 0 || height == 0 || solvedProblems.isEmpty()) {
            invalidate()
            return
        }

        var currentX = width / 2f
        var currentY = height / 2f
        var currentAngleRad = 0.0

        // 첫 번째 점 추가
        pathPoints.add(PointF(currentX, currentY))
        drawnPath.moveTo(currentX, currentY) // 시작점은 경로에 포함

        solvedProblems.forEachIndexed { index, problem ->
            // 첫 번째 문제가 마지막 문제인 경우는 거의 없겠지만, 예외 처리 (점을 찍어야 하므로 로직 변경)
            if (index == 0 && problem.direction == ProblemGroupFragment.LAST_PROBLEM_DIRECTION) {
                // 첫 번째 점은 이미 위에서 추가되었으므로, 여기서는 특별한 처리가 필요 없을 수 있습니다.
                // 혹은, 첫 번째이자 마지막 문제라면, 여기서 아무것도 안하고 루프를 종료하거나,
                // 아니면 첫 점에 대한 단어만 그리도록 pathPoints에 하나만 있도록 보장합니다.
                // 현재 로직상 첫 점은 무조건 pathPoints에 들어가므로, 이 조건은 큰 영향이 없을 수 있습니다.
                return@forEachIndexed // 혹은 다른 처리
            }

            // 마지막 문제가 아닌 경우에만 선을 그리고 좌표를 이동/계산합니다.
            if (problem.direction != ProblemGroupFragment.LAST_PROBLEM_DIRECTION) {
                // 다음 이동 전에 방향을 10~20도 랜덤하게 축을 돌림
                if (index > 0 || (solvedProblems.size > 1 && index == 0)) {
                    val randomRotationDeg = Random.nextDouble(10.0, 20.0) * (if (Random.nextBoolean()) 1 else -1)
                    currentAngleRad += Math.toRadians(randomRotationDeg)
                }

                var nextX = currentX
                var nextY = currentY

                when (problem.direction) {
                    "상단" -> {
                        nextX += (stepDistance * cos(currentAngleRad - Math.PI / 2)).toFloat()
                        nextY += (stepDistance * sin(currentAngleRad - Math.PI / 2)).toFloat()
                    }
                    "좌측" -> {
                        nextX += (stepDistance * cos(currentAngleRad + Math.PI)).toFloat()
                        nextY += (stepDistance * sin(currentAngleRad + Math.PI)).toFloat()
                    }
                    "우측" -> {
                        nextX += (stepDistance * cos(currentAngleRad)).toFloat()
                        nextY += (stepDistance * sin(currentAngleRad)).toFloat()
                    }
                    "하단" -> {
                        nextX += (stepDistance * cos(currentAngleRad + Math.PI / 2)).toFloat()
                        nextY += (stepDistance * sin(currentAngleRad + Math.PI / 2)).toFloat()
                    }
                }

                val minAllowableX = pointRadius
                val maxAllowableX = width - pointRadius
                val minAllowableY = pointRadius + textPaint.textSize
                val maxAllowableY = height - pointRadius - textOffset

                val safeMaxX = if (minAllowableX > maxAllowableX) minAllowableX else maxAllowableX
                val safeMaxY = if (minAllowableY > maxAllowableY) minAllowableY else maxAllowableY

                nextX = nextX.coerceIn(minAllowableX, safeMaxX)
                nextY = nextY.coerceIn(minAllowableY, safeMaxY)

                drawnPath.lineTo(nextX, nextY) // 선 그리기
                pathPoints.add(PointF(nextX, nextY)) // 다음 점의 위치 추가

                currentX = nextX
                currentY = nextY
            } else if (index > 0) { // 마지막 문제이고, 첫 번째 문제가 아닌 경우
                // 마지막 문제는 선은 그리지 않지만, 점은 이전 위치(currentX, currentY)에 그대로 있어야 합니다.
                // solvedProblems 리스트에는 마지막 문제의 단어가 포함되어 있고,
                // onDraw에서는 solvedProblems 리스트의 크기만큼 단어를 그리려고 시도할 것입니다.
                // 따라서 pathPoints에도 마지막 단어가 그려질 위치 정보가 필요합니다.
                // 이미 이전 문제까지의 pathPoints.add가 끝난 상태이므로,
                // 마지막 문제의 점은 이전 문제의 점과 동일한 위치에 그려지게 됩니다 (선을 그리지 않았으므로).
                // 만약 마지막 문제의 점을 위한 별도의 (약간 떨어진) 위치를 원한다면 여기서 계산해야 합니다.
                // 하지만 현재 요구사항은 "마지막 문제의 단어가 안 나온다" 이므로,
                // pathPoints에 마지막 단어를 위한 점이 들어가도록 하는 것이 중요합니다.

                // 현재 currentX, currentY는 마지막 "이전" 문제의 위치입니다.
                // solvedProblems 리스트에는 마지막 문제의 정보가 있지만,
                // pathPoints에는 아직 마지막 문제에 해당하는 점이 없습니다.
                // 마지막 문제의 점을 추가해줍니다. (이전 문제와 같은 위치가 됨)
                // 만약 마지막 문제가 항상 이전 문제에서 약간 떨어진 위치에 표시되어야 한다면,
                // 여기서 nextX, nextY를 다시 계산해야 합니다 (방향 없이, 예를 들어 살짝 아래에).
                // 지금은 이전 문제와 같은 위치에 점을 추가합니다.
                if (pathPoints.size < solvedProblems.size) { // solvedProblems에는 마지막 문제가 포함되어 있음
                    // 이 조건은 사실상 필요 없을 수 있습니다. forEachIndexed가 solvedProblems를 기준으로 돌기 때문입니다.
                    // 그냥 pathPoints에 마지막 점을 추가하면 됩니다.
                    // pathPoints.add(PointF(currentX, currentY)) // 마지막 점을 (이전과 동일한) 위치에 추가
                }
                // 위의 조건은 이미 루프가 solvedProblems를 기준으로 돌고 있고,
                // onDraw에서 solvedProblems.forEachIndexed와 pathPoints.size를 비교하므로,
                // calculatePathPoints에서 pathPoints의 개수를 solvedProblems의 개수와 맞춰주는 것이 핵심입니다.

                // 만약 solvedProblems가 10개라면, pathPoints도 10개가 되어야 모든 단어가 그려집니다.
                // 첫 번째 점은 루프 전에 추가됩니다.
                // 루프는 0부터 solvedProblems.size - 1 까지 돕니다.
                // index 0 ~ (마지막 문제 인덱스 - 1) 까지는 위에서 pathPoints.add(PointF(nextX, nextY))가 실행됩니다.
                // 마지막 문제 인덱스에서 이 'else if' 블록으로 들어옵니다.
                // 따라서 pathPoints에는 마지막 문제의 점이 아직 없습니다.
                // 여기서 추가해줘야 합니다.
                if (pathPoints.size < solvedProblems.size) {
                    pathPoints.add(PointF(currentX, currentY)) // 마지막 문제의 점을 이전 문제와 동일한 위치에 추가
                }
            }
        }
        invalidate()
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        // 뷰 크기가 변경되면 점 위치를 다시 계산
        if (solvedProblems.isNotEmpty()) {
            calculatePathPoints()
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (pathPoints.isEmpty()) return


        // 경로 그리기 (선)
        canvas.drawPath(drawnPath, linePaint)


        // 점 및 단어 텍스트 그리기
        solvedProblems.forEachIndexed { index, problemData ->
            if (index < pathPoints.size) {
                val point = pathPoints[index]
                canvas.drawCircle(point.x, point.y, pointRadius, pointPaint)


                // 텍스트 위치는 점 아래 또는 위 등 적절히 조절
                canvas.drawText(problemData.word, point.x, point.y + textOffset + textPaint.textSize / 2, textPaint)
            }
        }
    }
}