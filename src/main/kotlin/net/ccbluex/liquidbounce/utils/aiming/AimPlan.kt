/*
 * This file is part of LiquidBounce (https://github.com/CCBlueX/LiquidBounce)
 *
 * Copyright (c) 2015 - 2024 CCBlueX
 *
 * LiquidBounce is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * LiquidBounce is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with LiquidBounce. If not, see <https://www.gnu.org/licenses/>.
 */
package net.ccbluex.liquidbounce.utils.aiming

import net.ccbluex.liquidbounce.config.NamedChoice
import net.ccbluex.liquidbounce.utils.client.mc
import net.ccbluex.liquidbounce.utils.entity.rotation
import net.ccbluex.liquidbounce.utils.kotlin.random
import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.hypot

/**
 * An aim plan is a plan to aim at a certain rotation.
 * It is being used to calculate the next rotation to aim at.
 *
 * @param rotation The rotation we want to aim at.
 * @param smootherMode The mode of the smoother.
 * @param baseTurnSpeed The base turn speed of the smoother.
 */
class AimPlan(
    val rotation: Rotation,
    smootherMode: SmootherMode,
    baseTurnSpeed: ClosedFloatingPointRange<Float>,
    val ticksUntilReset: Int,
    /**
     * The reset threshold defines the threshold at which we are going to reset the aim plan.
     * The threshold is being calculated by the distance between the current rotation and the rotation we want to aim.
     */
    val resetThreshold: Float,
    /**
     * Consider if the inventory is open or not. If the inventory is open, we might not want to continue updating.
     */
    val considerInventory: Boolean,
    val applyVelocityFix: Boolean,
    val changeLook: Boolean
) {

    val angleSmooth: AngleSmooth = AngleSmooth(smootherMode, baseTurnSpeed)

    /**
     * Calculates the next rotation to aim at.
     * [fromRotation] is the current rotation or rather last rotation we aimed at. It is being used to calculate the
     * next rotation.
     *
     * We might even return null if we do not want to aim at anything yet.
     */
    fun nextRotation(fromRotation: Rotation, isResetting: Boolean): Rotation {
        if (isResetting) {
            return angleSmooth.limitAngleChange(fromRotation, mc.player!!.rotation)
        }

        return angleSmooth.limitAngleChange(fromRotation, rotation)
    }

}

enum class SmootherMode(override val choiceName: String) : NamedChoice {
    LINEAR("Linear"),
    RELATIVE("Relative")
}

/**
 * A smoother is being used to limit the angle change between two rotations.
 */
class AngleSmooth(val mode: SmootherMode, val baseTurnSpeed: ClosedFloatingPointRange<Float>) {

    fun limitAngleChange(currentRotation: Rotation, targetRotation: Rotation): Rotation {
        val yawDifference = RotationManager.angleDifference(targetRotation.yaw, currentRotation.yaw)
        val pitchDifference = RotationManager.angleDifference(targetRotation.pitch, currentRotation.pitch)

        val rotationDifference = hypot(abs(yawDifference), abs(pitchDifference))

        val (factorH, factorV) = when (mode) {
            SmootherMode.LINEAR ->
                baseTurnSpeed.random().toFloat() to baseTurnSpeed.random().toFloat()
            SmootherMode.RELATIVE ->
                computeFactor(abs(yawDifference)) to computeFactor(abs(pitchDifference))
        }

        val straightLineYaw = abs(yawDifference / rotationDifference) * factorH
        val straightLinePitch = abs(pitchDifference / rotationDifference) * factorV

        return Rotation(
            currentRotation.yaw + yawDifference.coerceIn(-straightLineYaw, straightLineYaw),
            currentRotation.pitch + pitchDifference.coerceIn(-straightLinePitch, straightLinePitch)
        )
    }

    private fun computeFactor(rotationDifference: Float): Float {
        val turnSpeed = baseTurnSpeed.random().toFloat()

        // Scale the rotation difference to fit within a reasonable range
        val scaledDifference = rotationDifference / 120f

        val steepness = 10f
        val midpoint = 0.3f

        // Compute the sigmoid function
        val sigmoid = 1 / (1 + exp((-steepness * (scaledDifference - midpoint)).toDouble()))

        // Interpolate sigmoid value to fit within the range of turnSpeed
        val interpolatedSpeed = sigmoid * turnSpeed

        return interpolatedSpeed.toFloat()
    }

}
