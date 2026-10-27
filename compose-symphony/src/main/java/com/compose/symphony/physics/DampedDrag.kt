package com.compose.symphony.physics

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlin.math.sign

/**
 * 🌊 阻尼拖拽动画状态
 * 
 * 实现类似 LiquidBottomTabs 的手势跟随效果：
 * - 拖拽时平滑跟随手指
 * - 释放后弹回吸附到最近选项
 * - 支持速度感知的弹性形变
 */
class DampedDragAnimationState(
    initialIndex: Int,
    private val itemCount: Int,
    private val scope: CoroutineScope,
    private val onIndexChanged: (Int) -> Unit
) {
    /** 当前动画值（浮点索引，用于平滑过渡） */
    private val animatable = Animatable(initialIndex.toFloat())
    
    /** 当前动画位置 */
    val value: Float get() = animatable.value
    
    /** 当前速度（用于形变效果） */
    val velocity: Float get() = animatable.velocity
    
    /** 是否正在拖拽 */
    var isDragging by mutableStateOf(false)
        private set
    
    /** 拖拽时的缩放比例 */
    val scale: Float get() = if (isDragging) 1.1f else 1f
    
    /** 目标索引（释放后吸附的目标） */
    private var targetIndex = initialIndex
    
    /** 动画是否正在运行 */
    val isRunning: Boolean get() = animatable.isRunning

    /**
     * 处理拖拽事件
     * @param dragAmountPx 拖拽像素距离
     * @param itemWidthPx 单个项目宽度（像素）
     */
    fun onDrag(dragAmountPx: Float, itemWidthPx: Float) {
        isDragging = true
        // [优化] 橡皮筋阻尼物理：
        // 1. 在有效范围内 (0 ~ itemCount-1) 阻尼为 1.0 (跟手，无滞后)
        // 2. 超出边界时，阻尼急剧增加，产生拉伸橡皮筋的感觉
        
        val currentValue = animatable.value
        val isOverscrolling = currentValue < 0f || currentValue > (itemCount - 1).toFloat()
        
        // [调整] 提升灵敏度系数 (0.6 -> 1.0) 确保完全跟手
        // 如果越界，阻尼系数降低 (0.3)
        val baseResistance = 1.0f 
        val dragResistance = if (isOverscrolling) 0.3f else baseResistance
        
        val deltaIndex = (-dragAmountPx / itemWidthPx) * dragResistance
        
        // 范围 [-0.5f, itemCount - 0.5f] 允许向左/向右各拉出半个身位作为极限
        val newValue = (animatable.value + deltaIndex).coerceIn(-0.5f, (itemCount - 0.5f))
        
        scope.launch {
            animatable.snapTo(newValue)
        }
    }
    
    /**
     * 立即跳转到指定位置（无动画）
     * 用于与外部状态（如 Pager）同步
     */
    fun snapTo(targetValue: Float) {
        // 更新目标索引以防止 offset 累积误差
        targetIndex = targetValue.roundToInt().coerceIn(0, itemCount - 1)
        scope.launch {
            animatable.snapTo(targetValue)
        }
    }

    /**
     * 处理拖拽结束
     */
    fun onDragEnd() {
        isDragging = false
        
        // 计算最近的吸附目标
        val currentValue = animatable.value
        targetIndex = currentValue.roundToInt().coerceIn(0, itemCount - 1)
        
        scope.launch {
            animatable.animateTo(
                targetValue = targetIndex.toFloat(),
                animationSpec = spring(
                    dampingRatio = 0.6f,   // [增强] 更低阻尼 = 更强回弹 (0.7 -> 0.6)
                    stiffness = 350f       // [增强] 略微降低刚度，让回弹幅度感觉更大 (400 -> 350)
                )
            )
            onIndexChanged(targetIndex)
        }
    }
    
    /**
     * 外部更新选中索引（点击选择时）
     */
    fun updateIndex(index: Int) {
        if (index == targetIndex && !isDragging) return
        targetIndex = index
        scope.launch {
            animatable.animateTo(
                targetValue = index.toFloat(),
                animationSpec = spring(
                    dampingRatio = 0.6f,   // [增强] 保持一致的回弹感
                    stiffness = 350f
                )
            )
        }
    }
}

/**
 * 创建并记住阻尼拖拽动画状态
 * 
 * @param initialIndex 初始选中索引
 * @param itemCount 项目数量
 * @param onIndexChanged 索引变化回调
 */
@Composable
fun rememberDampedDragAnimationState(
    initialIndex: Int,
    itemCount: Int,
    onIndexChanged: (Int) -> Unit
): DampedDragAnimationState {
    val scope = rememberCoroutineScope()
    
    return remember(itemCount) {
        DampedDragAnimationState(
            initialIndex = initialIndex,
            itemCount = itemCount,
            scope = scope,
            onIndexChanged = onIndexChanged
        )
    }
}

/**
 * 水平拖拽手势 Modifier
 * 
 * @param dragState 阻尼拖拽动画状态
 * @param itemWidthPx 单个项目宽度（像素）
 */
fun Modifier.horizontalDragGesture(
    dragState: DampedDragAnimationState,
    itemWidthPx: Float
): Modifier = this.pointerInput(dragState, itemWidthPx) {
    detectHorizontalDragGestures(
        onDragStart = { },
        onDragEnd = { dragState.onDragEnd() },
        onDragCancel = { dragState.onDragEnd() },
        onHorizontalDrag = { change, dragAmount ->
            change.consume()
            dragState.onDrag(dragAmount, itemWidthPx)
        }
    )
}
