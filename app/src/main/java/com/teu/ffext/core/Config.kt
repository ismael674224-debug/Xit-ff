package com.teu.ffext.core

data class Config(
    var aimEnabled: Boolean = false,
    var aimMode: AimMode = AimMode.NORMAL,
    var aimTarget: AimTarget = AimTarget.HEAD,
    var fovRadius: Int = 220,
    var smooth: Float = 0.35f,
    var gaussError: Float = 6f,
    var reactionMs: Int = 120,
    var autoFire: Boolean = false,
    var priority: Priority = Priority.CENTER,
    var humanize: Boolean = true,

    var espEnabled: Boolean = true,
    var espBox: EspBox = EspBox.HEAD,
    var espLine: Boolean = true,
    var espInfo: Boolean = true,
    var espFovCircle: Boolean = true,
    var espOpacity: Int = 180,
    var espThickness: Float = 3f,
    var espOnlyOnScreen: Boolean = true,

    var spinEnabled: Boolean = false,
    var spinMode: SpinMode = SpinMode.CONTINUO,
    var spinRadius: Int = 160,
    var spinStepGraus: Float = 4f,
    var spinIntervalMs: Long = 12,
    var spinSnapAngle: Float = 25f,
    var spinSnapStrength: Float = 0.35f,
    var spinJitter: Float = 6f
)

enum class AimMode { OFF, NORMAL, R360 }
enum class AimTarget { HEAD, BODY, NECK }
enum class Priority { CENTER, CURSOR, NEAREST }
enum class EspBox { HEAD, BODY, FULL }
enum class SpinMode { CONTINUO, ON_TARGET, SNAP_SPIN }
