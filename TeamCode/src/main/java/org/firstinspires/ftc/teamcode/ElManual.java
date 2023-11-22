package org.firstinspires.ftc.teamcode;

import com.qualcomm.robotcore.eventloop.opmode.OpMode;
import com.qualcomm.robotcore.eventloop.opmode.TeleOp;
import com.qualcomm.robotcore.hardware.*;

@TeleOp(name = "El Manual", group = "Competition")
public class ElManual extends OpMode {

   // Connected Devices
   private Blinker debug_light;
   private HardwareDevice robot;
   private IMU imu;

   // Motors
   DcMotor leftFrontDrive;
   DcMotor rightFrontDrive;
   DcMotor leftRearDrive;
   DcMotor rightRearDrive;
   DcMotor linearActuator;

   // Booleans
   boolean slowDown = false;
   boolean intakeOn = true;

   // Servos
   CRServo intake;
   CRServo launcher;

   // Initialize
   @Override
   public void init() {
       rightFrontDrive = hardwareMap.get(DcMotor.class, "MotorRF");
       leftFrontDrive = hardwareMap.get(DcMotor.class, "MotorLF");
       rightRearDrive = hardwareMap.get(DcMotor.class, "MotorRR");
       leftRearDrive = hardwareMap.get(DcMotor.class, "MotorLR");

       intake = hardwareMap.get(CRServo.class, "Sweeper");
       launcher = hardwareMap.get(CRServo.class, "Launcher");

       linearActuator = hardwareMap.get(DcMotor.class, "LinearActuator");
   }

   // Robot Loop
   @Override
   public void loop() {
       double rightTurn = gamepad1.left_stick_y-gamepad1.right_stick_x;
       double leftTurn = -gamepad1.left_stick_y-gamepad1.right_stick_x;

       double rightFrontPower = rightTurn+gamepad1.left_stick_x;
       double leftFrontPower = leftTurn+gamepad1.left_stick_x;
       double rightRearPower = rightTurn-gamepad1.left_stick_x;
       double leftRearPower = leftTurn-gamepad1.left_stick_x;

       if (gamepad1.left_bumper) {
           slowDown = true;
       } else {
           slowDown = false;
       }

       if (gamepad2.a) {
           launcher.setPower(1);
       }

       if (gamepad2.dpad_down) {
           intakeOn = !intakeOn;
       }

       if (gamepad2.b) {
           linearActuator.setPower(1);
       }

       if (slowDown) {
           rightFrontPower /= 3;
           leftFrontPower /= 3;
           rightRearPower /= 3;
           leftRearPower /= 3;
       }

       rightFrontDrive.setPower(rightFrontPower);
       leftFrontDrive.setPower(leftFrontPower);
       rightRearDrive.setPower(rightRearPower);
       leftRearDrive.setPower(leftRearPower);

       intake.setPower(intakeOn ? -1 : 0);

       telemetry.addData("Movement", "RF %5.2f, LF %5.2f, RR %5.2f, LR %5.2f",
               rightFrontPower,
               leftFrontPower,
               rightRearPower,
               leftRearPower);
       telemetry.update();
   }
}
