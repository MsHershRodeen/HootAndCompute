package robot

import attachment.Claw
import attachment.Extender
import attachment.Lifters
import com.qualcomm.hardware.dfrobot.HuskyLens
import com.qualcomm.robotcore.hardware.*
import org.firstinspires.ftc.robotcore.external.Telemetry
import org.firstinspires.ftc.robotcore.external.navigation.DistanceUnit
import util.Encoder
import kotlin.math.abs
import kotlin.math.pow

class Steve : Robot() {
    // Drive motors
    private lateinit var leftFrontDrive: DcMotor
    private lateinit var rightFrontDrive: DcMotor
    private lateinit var leftRearDrive: DcMotor
    private lateinit var rightRearDrive: DcMotor

    // Sensors
    private lateinit var imu: IMU
    private lateinit var distanceSensor: DistanceSensor
    private lateinit var huskyLens: HuskyLens

    // Attachments
    private lateinit var lifters: Lifters
    private lateinit var claw: Claw
    private lateinit var extender: Extender

    // Other
    private lateinit var robotEncoderDrive: Encoder

    // Drive parameters
    private val countsPerMotorRev: Double = 560.0  // Encoder counts per motor revolution
    private val driveGearReduction: Double = 1.0  // Gear reduction from external gears
    private val wheelDiameterMM: Double = 96.0    // Wheel diameter in mm
    private val wheelBaseWidthMM: Double = 460.0  // Wheelbase width in mm

    // Control parameters
    private val deadzone = 0.05  // Minimum stick movement to register
    private val minPower = 0.05  // Minimum power to move motors
    private val turnScale = 0.8  // Reduce turn sensitivity
    private val inputExp = 2.0   // Input exponential for fine control

    // Speed modes
    private val speedModes = mapOf(
        "TURBO" to 1.0,
        "NORMAL" to 0.8,
        "PRECISE" to 0.4
    )
    private var currentSpeedMode = "NORMAL"

    override fun init(hardwareMap: HardwareMap) {
        // Register hardware
        registerMotors(hardwareMap)
        registerSensors(hardwareMap)
        registerAttachments(hardwareMap)

        // Configure motors for better control
        configureDriveMotors()

        // Reset IMU
        imu.resetYaw()

        // Set huskylens mode
        huskyLens.selectAlgorithm(HuskyLens.Algorithm.OBJECT_RECOGNITION)

        // Initialize encoder drive
        robotEncoderDrive = Encoder(
            listOf(leftFrontDrive, rightFrontDrive, leftRearDrive, rightRearDrive),
            countsPerMotorRev,
            driveGearReduction,
            wheelDiameterMM,
            6
        )
    }

    private fun configureDriveMotors() {
        // Configure all drive motors
        listOf(leftFrontDrive, rightFrontDrive, leftRearDrive, rightRearDrive).forEach { motor ->
            motor.zeroPowerBehavior = DcMotor.ZeroPowerBehavior.BRAKE  // Better stopping
            motor.mode = DcMotor.RunMode.RUN_USING_ENCODER  // Enable encoder feedback
        }
    }

    override fun driveWithGamepad(gamepad: Gamepad) {
        // Handle speed mode changes
        updateSpeedMode(gamepad)

        // Get gamepad input with deadzone and exponential scaling
        val x = processInput(gamepad.left_stick_x.toDouble())
        val y = processInput(-gamepad.left_stick_y.toDouble())  // Inverted Y
        val rx = processInput(gamepad.right_stick_x.toDouble()) * turnScale

        // Calculate mecanum drive powers
        val powers = calculateMecanumPowers(x, y, rx)

        // Apply current speed mode scaling
        val scaledPowers = powers.map { it * speedModes[currentSpeedMode]!! }

        // Set motor powers with a minimum power threshold
        setMotorPowers(scaledPowers)
    }

    fun driveWithEncoder(speed: Double, distanceMM: Double) {
        robotEncoderDrive.startEncoderWithUnits(speed, -distanceMM, Encoder.UnitType.MM)
    }

    fun strafeWithEncoder(speed: Double, distanceMM: Double) {
        robotEncoderDrive.startEncoderWithUnits(speed, -distanceMM, Encoder.UnitType.MM, listOf(0, 1))
    }

    fun spinWithEncoder(speed: Double, angleDegrees: Double) {
        val robotCircumferenceMM = wheelBaseWidthMM * Math.PI
        val distanceMM = (angleDegrees / 360.0) * robotCircumferenceMM
        robotEncoderDrive.startEncoderWithUnits(speed, -distanceMM, Encoder.UnitType.MM, listOf(1, 2))
    }

    private fun processInput(input: Double): Double {
        // Apply deadzone
        if (abs(input) < deadzone) return 0.0

        // Normalize input
        val normalizedInput = (input - deadzone) / (1 - deadzone)

        // Apply exponential scaling for fine control
        return normalizedInput.pow(inputExp) * if (input < 0) -1 else 1
    }

    private fun calculateMecanumPowers(x: Double, y: Double, rx: Double): List<Double> {
        // Calculate raw powers
        val rfPower = y - x - rx
        val lfPower = y + x + rx
        val rrPower = y + x - rx
        val lrPower = y - x + rx

        // Find maximum magnitude
        val maxMagnitude = maxOf(abs(rfPower), abs(lfPower), abs(rrPower), abs(lrPower))

        // Normalize powers
        val normalizationFactor = if (maxMagnitude > 1.0) maxMagnitude else 1.0
        return listOf(rfPower, lfPower, rrPower, lrPower).map { it / normalizationFactor }
    }

    private fun setMotorPowers(powers: List<Double>) {
        // Apply minimum power threshold and set motors
        rightFrontDrive.power = applyMinPower(powers[0])
        leftFrontDrive.power = applyMinPower(powers[1])
        rightRearDrive.power = applyMinPower(powers[2])
        leftRearDrive.power = applyMinPower(powers[3])
    }

    private fun applyMinPower(power: Double): Double {
        return when {
            power > minPower -> power
            power < -minPower -> power
            else -> 0.0
        }
    }

    private fun updateSpeedMode(gamepad: Gamepad) {
        when {
            gamepad.y -> {
                currentSpeedMode = "TURBO"
                gamepad.rumble(1.0, 1.0, 50)
            }
            gamepad.b -> {
                currentSpeedMode = "NORMAL"
                gamepad.rumble(1.0, 1.0, 50)
            }
            gamepad.a -> {
                currentSpeedMode = "PRECISE"
                gamepad.rumble(1.0, 1.0, 50)
            }
        }
    }

    override fun halt() {
        // Stop all drive motors
        setMotorPowers(listOf(0.0, 0.0, 0.0, 0.0))
    }

    fun controlLiftersWithGamepad(gamepad: Gamepad, telemetry: Telemetry) {
        // Control lifters
        for (position in lifters.currentPositions())
            telemetry.addData("Extender current position", "%d", position)
        for (position in lifters.targetPositions())
            telemetry.addData("Extender target position", "%d", position)
        telemetry.addData("Extender encoder moving", "%b", lifters.moving())
        if (gamepad.y) {
            gamepad.rumble(1.0, 1.0, 50)
            lifters.lift(0.86)
            return
        }
        if (!lifters.moving()) {
            lifters.stopEncoder()
            val power = gamepad.left_trigger.toDouble() - gamepad.right_trigger.toDouble()
            gamepad.rumble(power, power, 10)
            lifters.setPower(power)
        }
        else {
            lifters.checkEncoderTimeout()
        }
    }

    fun controlClawWithGamepad(gamepad: Gamepad) {
        // Control arm
        if (gamepad.a) claw.setPower(claw.maxPower)
        else if (gamepad.b) claw.setPower(-claw.maxPower)
        else claw.setPower(0.0)
    }

    fun controlExtenderWithGamepad(gamepad: Gamepad, telemetry: Telemetry) {
        // Control extender
        val position = extender.currentPosition
        telemetry.addData("Extender position", "%5.2f", position)
        if (!gamepad.x)
            return
        gamepad.rumble(1.0, 1.0, 50)
        if (position == 0.0) extender.extend()
        else if (position == 1.0) extender.retract()
    }

    private fun registerMotors(hardwareMap: HardwareMap) {
        // Register motors
        leftFrontDrive = hardwareMap.get(DcMotor::class.java, "lf")
        rightFrontDrive = hardwareMap.get(DcMotor::class.java, "rf")
        leftRearDrive = hardwareMap.get(DcMotor::class.java, "lr")
        rightRearDrive = hardwareMap.get(DcMotor::class.java, "rr")

        // Set motor directions
        leftFrontDrive.direction = DcMotorSimple.Direction.FORWARD
        rightFrontDrive.direction = DcMotorSimple.Direction.REVERSE
        leftRearDrive.direction = DcMotorSimple.Direction.FORWARD
        rightRearDrive.direction = DcMotorSimple.Direction.REVERSE
    }

    private fun registerAttachments(hardwareMap: HardwareMap) {
        // Register attachments
        lifters = Lifters(hardwareMap, "liftr", "liftl")
        claw = Claw(hardwareMap, "claw")
        extender = Extender(hardwareMap, "extend")

        // Reset lifters encoder
        lifters.resetEncoder()
    }

    private fun registerSensors(hardwareMap: HardwareMap) {
        // Register sensors
        imu = hardwareMap.get(IMU::class.java, "imu")
        distanceSensor = hardwareMap.get(DistanceSensor::class.java, "lidar")
        huskyLens = hardwareMap.get(HuskyLens::class.java, "lens")
    }

    fun getDetectedObjects(telemetry: Telemetry): Array<out HuskyLens.Block>? {
        // Get objects
        val blocks = huskyLens.blocks()
        telemetry.addData("Block count", blocks.size)
        for (block in blocks) {
            telemetry.addData("Block", block.toString())
        }
        return blocks
    }

    fun getDistanceToObstacle(telemetry: Telemetry): Double {
        // Get distance
        val distance = distanceSensor.getDistance(DistanceUnit.MM)
        telemetry.addData("range", "%.01f mm".format(distance))
        return distance
    }

    fun driving(): Boolean {
        // Check if the robot is driving
        val motorsBusy = listOf(rightFrontDrive, leftFrontDrive, rightRearDrive, leftRearDrive).any { it.isBusy }
        if (motorsBusy) {
            // Check for encoder timeout
            if (robotEncoderDrive.shouldTimeout()) {
                robotEncoderDrive.stopEncoder()
                return false
            }
            return true
        }
        // Stop the encoder
        robotEncoderDrive.stopEncoder()
        return false
    }

    fun liftLifters(power: Double) {
        // Lift
        lifters.lift(power)
    }

    fun liftersMoving(): Boolean {
        // Check if the lifters are moving
        return lifters.moving()
    }

    fun extendExtender() : Boolean {
        // Extend
        return extender.extend()
    }

    fun retractExtender() : Boolean {
        // Retract
        return extender.retract()
    }

    fun openCloseClaw() : Boolean {
        // Open or close the claw
        return claw.openClose()
    }
}