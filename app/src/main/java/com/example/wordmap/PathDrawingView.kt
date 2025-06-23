package com.example.wordmap

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PointF
import android.util.AttributeSet
import android.view.View
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

/**
 * fragment_completion에서 경로를 그려서 출력하기 위한 커스텀 뷰
 */

class  PathDrawingView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private var solvedProblems: List<SolvedProblemData> = emptyList()
    private val pathPoints = mutableListOf<PointF>() // 각 단어의 위치 (점)
    private val drawnPath = Path() // 그려질 전체 경로

    // 점을 그릴때 사용할 객체. 색 : 파랑
    private val pointPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.BLUE
        style = Paint.Style.FILL
    }

    // 선을 그릴때 사용할 객체 색 : 파랑, 두깨: 5f
    private val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.BLUE
        strokeWidth = 5f
        style = Paint.Style.STROKE
    }

    // 단어 택스트를 그릴때 사용할 객체 색 : 검정, 크기 : 30f
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

    private fun calculatePathPoints() {
        pathPoints.clear()
        drawnPath.reset()

        if (width == 0 || height == 0 || solvedProblems.isEmpty()) {
            invalidate()
            return
        }

        //시작점 설정
        var currentX = width / 2f
        var currentY = height / 2f
        var currentAngleRad = 0.0

        // 첫 번째 점 추가
        pathPoints.add(PointF(currentX, currentY))
        drawnPath.moveTo(currentX, currentY) // 시작점은 경로에 포함

        solvedProblems.forEachIndexed { index, problem ->
            if (index == 0 && problem.direction == ProblemGroupFragment.LAST_PROBLEM_DIRECTION) {
                return@forEachIndexed
            }

            // 마지막 문제가 아닌 경우에만 선을 그리고 좌표를 이동/계산.
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
            } else if (index > 0) {
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