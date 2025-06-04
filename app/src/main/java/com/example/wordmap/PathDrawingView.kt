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

    private fun calculatePathPoints() {
        pathPoints.clear()
        drawnPath.reset()
        if (solvedProblems.isEmpty()) return

        var currentX = width / 2f
        var currentY = height / 2f
        var currentAngleRad = 0.0 // 현재 각도 (라디안 단위)

        // 첫 번째 점 (중앙)
        pathPoints.add(PointF(currentX, currentY))
        drawnPath.moveTo(currentX, currentY)

        // 나머지 점들 계산
        solvedProblems.forEachIndexed { index, problem ->
            if (index == 0 && problem.direction == ProblemGroupFragment.LAST_PROBLEM_DIRECTION) {
                // 첫 번째 문제가 마지막 문제인 경우는 거의 없겠지만, 예외 처리
                return@forEachIndexed
            }
            if (problem.direction == ProblemGroupFragment.LAST_PROBLEM_DIRECTION && index > 0) {
                // 마지막 문제는 방향이 없으므로, 이전 지점에서 끝냄
                return@forEachIndexed
            }


            // 다음 이동 전에 방향을 10~20도 랜덤하게 축을 돌림
            // (첫 번째 단어 이후부터 적용)
            if (index > 0 || solvedProblems.size > 1 && index == 0) { // 첫 이동부터 회전 적용 가능
                val randomRotationDeg = Random.nextDouble(10.0, 20.0) * (if (Random.nextBoolean()) 1 else -1) // -20 ~ -10 또는 10 ~ 20도
                currentAngleRad += Math.toRadians(randomRotationDeg)
            }


            var nextX = currentX
            var nextY = currentY

            // 방향에 따라 다음 좌표 계산 및 각도 조정
            // 여기서의 각도는 일반적인 수학적 각도 (x축 양의 방향이 0도)
            when (problem.direction) {
                "상단" -> {
                    // 현재 각도에서 위로 (수학적으로는 90도 또는 PI/2)
                    nextX += (stepDistance * cos(currentAngleRad - Math.PI / 2)).toFloat()
                    nextY += (stepDistance * sin(currentAngleRad - Math.PI / 2)).toFloat()
                }
                "좌측" -> {
                    // 현재 각도에서 왼쪽으로 (수학적으로는 180도 또는 PI)
                    nextX += (stepDistance * cos(currentAngleRad + Math.PI)).toFloat()
                    nextY += (stepDistance * sin(currentAngleRad + Math.PI)).toFloat()
                }
                "우측" -> {
                    // 현재 각도에서 오른쪽으로 (수학적으로는 0도)
                    nextX += (stepDistance * cos(currentAngleRad)).toFloat()
                    nextY += (stepDistance * sin(currentAngleRad)).toFloat()
                }
                // "하단" 방향이 있다면 추가
                "하단" -> {
                    nextX += (stepDistance * cos(currentAngleRad + Math.PI / 2)).toFloat()
                    nextY += (stepDistance * sin(currentAngleRad + Math.PI / 2)).toFloat()
                }
            }

            // 뷰 경계를 벗어나지 않도록 간단한 처리 (더 정교한 로직 필요할 수 있음)
//            nextX = nextX.coerceIn(pointRadius, width - pointRadius)
//            nextY = nextY.coerceIn(pointRadius + textPaint.textSize, height - pointRadius - textOffset)


            drawnPath.lineTo(nextX, nextY)
            pathPoints.add(PointF(nextX, nextY))

            currentX = nextX
            currentY = nextY


            // 다음 방향을 위한 각도 업데이트 (이동한 방향으로 기본 축 설정)
            // 예를 들어 "상단"으로 이동했으면, 다음 이동은 현재 위치에서 위쪽을 기준으로 회전이 적용됨
            // 이 부분은 디자인에 따라 다양하게 구현 가능
            // 여기서는 매번 랜덤 회전을 적용하므로, 특정 방향으로의 기본 축 변경은 생략 가능
        }
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