package org.firstinspires.ftc.teamcode.opmode.manual

import com.acmerobotics.roadrunner.InstantAction
import com.acmerobotics.roadrunner.Pose2d
import com.acmerobotics.roadrunner.Vector2d
import dev.kingssack.volt.manual.SimpleManualModeWithSpeedModes
import com.qualcomm.robotcore.hardware.Gamepad
import com.qualcomm.robotcore.hardware.HardwareMap
import org.firstinspires.ftc.robotcore.external.Telemetry
import org.firstinspires.ftc.teamcode.attachment.Lift
import org.firstinspires.ftc.teamcode.attachment.Wrist
import org.firstinspires.ftc.teamcode.robot.Steve

/**
 * Whale is a manual mode focused on functionality
 *
 * @param hardwareMap the hardware map
 * @param telemetry the telemetry
 * @param gamepad1 gamepad for movement
 * @param gamepad2 gamepad for actions
 *
 * @property robot the robot
 */
class Whale(
    hardwareMap: HardwareMap,
    telemetry: Telemetry,
    private val gamepad1: Gamepad,
    private val gamepad2: Gamepad,
    private val params: WhaleParams = WhaleParams()
) : SimpleManualModeWithSpeedModes(gamepad1, gamepad2, telemetry) {
    /**
     * ManualParams is a configuration object for manual control.
     *
     * @property initialX the initial x position
     * @property initialY the initial y position
     * @property initialHeading the initial heading
     */
    class WhaleParams(
        val initialX: Double = -48.0,
        val initialY: Double = 64.0,
        val initialHeading: Double = 90.0,

        val liftMultiplier: Int = 12
    )

    override val robot = Steve(hardwareMap, Pose2d(
        Vector2d(params.initialX, params.initialY),
        Math.toRadians(params.initialHeading)
    ))

    private val quickTurnTowardBaskets = Interaction({ isButtonTapped("dpad_left1") },
        { robot.turnTo(Math.toRadians(45.0)) })
    private val quickTurnTowardSubmersible = Interaction({ isButtonTapped("dpad_up1") },
        { robot.turnTo(Math.toRadians(-90.0)) })
    private val quickDepositSample = Interaction(({ isButtonTapped("dpad_right1") }),
        { robot.depositSample(Lift.upperBasketHeight) })

    private val raiseLift = Interaction({ isButtonTapped("right_bumper2") }, { robot.lift.goTo(Lift.upperBasketHeight) })
    private val lowerLift = Interaction({ isButtonTapped("left_bumper2") }, { robot.lift.drop() })
    private val controlLift = Interaction({ true },
        { InstantAction { robot.lift.currentGoal += -getAnalogValue("left_stick_y2").toInt() * params.liftMultiplier } })

    private val openClaw = Interaction({ isButtonTapped("a2") }, { robot.claw.open() })
    private val closeClaw = Interaction({ isButtonTapped("b2") }, { robot.claw.close() })

    private val centerWrist = Interaction({ isButtonTapped("dpad_down2") }, { robot.wrist.goTo(Wrist.centerPosition) })
    private val rotateWristLeft = Interaction({ isButtonPressed("dpad_left2") },
        { robot.wrist.goTo(robot.wrist.getPosition() + 0.005) })
    private val rotateWristRight = Interaction({ isButtonPressed("dpad_right2") },
        { robot.wrist.goTo(robot.wrist.getPosition() - 0.005) })

    private val extendArm = Interaction({ isButtonTapped("x2") }, { robot.extendArm() })
    private val retractArm = Interaction({ isButtonTapped("y2") }, { robot.retractArm() })

    private val controlElbow = Interaction({ true },
        { InstantAction { robot.elbow.setPower(-getAnalogValue("right_stick_y2")) } })

    init {
        interactions.addAll(listOf(
            quickTurnTowardBaskets,
            quickTurnTowardSubmersible,
            quickDepositSample,
            raiseLift,
            lowerLift,
            controlLift,
            openClaw,
            closeClaw,
            centerWrist,
            rotateWristLeft,
            rotateWristRight,
            extendArm,
            retractArm,
            controlElbow
        ))
    }

    override fun tick(telemetry: Telemetry) {
        // Drive
        robot.setDrivePowers(calculatePoseWithGamepad())
        // quickMovements()

        // Control lift and claw
        /* controlLiftWithGamepad(gamepad2)
        controlClawWithGamepad(gamepad2)
        rotateClawWithGamepad(gamepad2)
        extendClawWithGamepad(gamepad2) */

        super.tick(telemetry)
    }

    /* private fun quickMovements() {
        // Quick movements
        if (isButtonTapped("dpad_up1")) runningActions.add(robot.turnTo(Math.toRadians(-90.0)))
        else if (isButtonTapped("dpad_left1")) runningActions.add(robot.turnTo(Math.toRadians(45.0)))
    }

    private fun controlLiftWithGamepad(gamepad: Gamepad) {
        // Control lifters
        if (gamepad.right_bumper) runningActions.add(robot.lift.goTo(Lift.upperBasketHeight))
        else if (gamepad.left_bumper) runningActions.add(robot.lift.drop())
    }

    private fun controlClawWithGamepad(gamepad: Gamepad) {
        // Control claw
        if (gamepad.a) runningActions.add(robot.claw.open())
        if (gamepad.b) runningActions.add(robot.claw.close())
    }

    private fun rotateClawWithGamepad(gamepad: Gamepad) {
        // Control claw rotation
        if (gamepad.dpad_right) runningActions.add(robot.wrist.goTo(0.5))
        else if (gamepad.dpad_up) runningActions.add(robot.wrist.goTo(robot.wrist.getPosition() + 0.05))
        else if (gamepad.dpad_down) runningActions.add(robot.wrist.goTo(robot.wrist.getPosition() - 0.05))
    }

    private fun extendClawWithGamepad(gamepad: Gamepad) {
        // Control shoulder and elbow
        if (gamepad.x) runningActions.add(robot.retractArm())
        else if (gamepad.y) runningActions.add(robot.extendArm())
        else robot.elbow.setPower(-gamepad.right_stick_y.toDouble())
    } */
}