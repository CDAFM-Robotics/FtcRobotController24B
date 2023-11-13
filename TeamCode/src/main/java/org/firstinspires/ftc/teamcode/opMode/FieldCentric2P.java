package org.firstinspires.ftc.teamcode.opMode;

import com.qualcomm.hardware.rev.RevHubOrientationOnRobot;
import com.qualcomm.robotcore.eventloop.opmode.LinearOpMode;
import com.qualcomm.robotcore.eventloop.opmode.TeleOp;
import com.qualcomm.robotcore.hardware.Blinker;
import com.qualcomm.robotcore.hardware.DcMotor;
import com.qualcomm.robotcore.hardware.DcMotorSimple;
import com.qualcomm.robotcore.hardware.Gamepad;
import com.qualcomm.robotcore.hardware.IMU;
import com.qualcomm.robotcore.hardware.Servo;
import com.qualcomm.robotcore.util.ElapsedTime;

import org.firstinspires.ftc.robotcore.external.navigation.AngleUnit;

@TeleOp (group = "Competition", name = "Field Centric 2Player")

// Next line will prevent code from building and showing up on Control Hub
// @Disabled
// comment

public class FieldCentric2P extends LinearOpMode {
  private Blinker control_Hub;
  private Servo bottomArmServo;
  private IMU imu;
  private Servo topArmServo;
  private Servo wristPanServo;
  private ElapsedTime runtime = new ElapsedTime();
  private DcMotor frontLeftMotor = null; //front left
  private DcMotor frontRightMotor = null; //front right
  private DcMotor backLeftMotor = null; //back left
  private DcMotor backRightMotor = null; //back right
  private DcMotor armmotor = null;
  private Servo droneServo = null;
  private Servo hookServo = null;

  //define all constants
  private int ARM_DRIVE_POSITION = 500;
  private int ARM_DEPLOY_POSITION = 5720;
  private int ARM_MAX_POSITION = 7718;
  private int ARM_PICKUP_POSITION = 175;
  private int ARM_HOOK_POSITION = 3716;
  private double ARM_POWER = 0.6;
  double WRIST_PAN_SERVO_FOLDED = 0.6;
  double WRIST_PAN_SERVO_FLOOR = 0.024;
  double WRIST_PAN_SERVO_SPEED = 0.008;
  double botHeading = 0;
  double DRONE_POSITION_ARMED = 0;
  double DRONE_POSITION_LAUNCH = 0.25; //Early Guess

  // double servoSetPosition = 0.15; // Initial SETUP position 0.15 (on 0-1 scale) install first notch where jaws don't touch
  double HOOK_HIDE_POSITION = 0.0;
  double HOOK_LAUNCH_POSITION = 0.35; //Early Guess
  double bottomServoClose = 0.10; // (close to touch)
  double bottomServoOpen = 0.30;  // (old open distance)
  double topServoClose = 0.10;
  double topServoOpen = 0.30;
  double slow_mode = 0.75;



  @Override
  public void runOpMode() {
    //read hardware configurations
    control_Hub = hardwareMap.get(Blinker.class, "Control Hub");
    bottomArmServo = hardwareMap.get(Servo.class, "bottomArmServo");
    topArmServo = hardwareMap.get(Servo.class, "topArmServo");
    wristPanServo = hardwareMap.get(Servo.class, "wristPanServo");
    frontLeftMotor = hardwareMap.get(DcMotor.class, "motor1");
    frontRightMotor = hardwareMap.get(DcMotor.class, "motor2");
    backLeftMotor = hardwareMap.get(DcMotor.class, "motor3");
    backRightMotor = hardwareMap.get(DcMotor.class, "motor4");
    armmotor = hardwareMap.get(DcMotor.class, "armcontrol");
    droneServo = hardwareMap.get(Servo.class, "droneServo");
    hookServo = hardwareMap.get(Servo.class, "hookServo");
    imu = hardwareMap.get(IMU.class, "imu");

    //define initial values for variables
    double lTrigger;
    double rTrigger;
    double lStickX;
    double lStickY;
    double rStickX;
    double lStickY2; //Gamepad 2 left stick
    double rStickY2; //Gamepad 2 right stick
    boolean left_Stick_Button = false;
    boolean bottomFingerServoOpen = false;
    boolean topFingerServoOpen = false;
    boolean robotHanging = false;
    //Wrist initial position is folded
    double wristPanPos = WRIST_PAN_SERVO_FOLDED;
    // By setting these values to new Gamepad(), they will default to all
    // boolean values as false and all float values as 0
    Gamepad currentGamepad1 = new Gamepad();
    Gamepad currentGamepad2 = new Gamepad();

    Gamepad previousGamepad1 = new Gamepad();
    Gamepad previousGamepad2 = new Gamepad();


    telemetry.addData("Status", "Initializing...");
    telemetry.update();

    //Initialized the motors
    frontLeftMotor.setPower(0);
    frontLeftMotor.setMode(DcMotor.RunMode.RUN_WITHOUT_ENCODER);
    frontLeftMotor.setZeroPowerBehavior(DcMotor.ZeroPowerBehavior.BRAKE);

    frontRightMotor.setPower(0);
    frontRightMotor.setMode(DcMotor.RunMode.RUN_WITHOUT_ENCODER);
    frontRightMotor.setZeroPowerBehavior(DcMotor.ZeroPowerBehavior.BRAKE);
    frontRightMotor.setDirection(DcMotorSimple.Direction.REVERSE);

    backLeftMotor.setPower(0);
    backLeftMotor.setMode(DcMotor.RunMode.RUN_WITHOUT_ENCODER);
    backLeftMotor.setZeroPowerBehavior(DcMotor.ZeroPowerBehavior.BRAKE);

    backRightMotor.setPower(0);
    backRightMotor.setMode(DcMotor.RunMode.RUN_WITHOUT_ENCODER);
    backRightMotor.setZeroPowerBehavior(DcMotor.ZeroPowerBehavior.BRAKE);
    backRightMotor.setDirection(DcMotorSimple.Direction.REVERSE);

    armmotor.setZeroPowerBehavior(DcMotor.ZeroPowerBehavior.BRAKE);
    armmotor.setMode(DcMotor.RunMode.STOP_AND_RESET_ENCODER); // jw test

    //Initialize arm motor
    armmotor.setTargetPosition(ARM_PICKUP_POSITION);
    armmotor.setMode(DcMotor.RunMode.RUN_TO_POSITION);
    armmotor.setPower(1);

    // armmotor.setMode(DcMotor.RunMode.STOP_AND_RESET_ENCODER);
    //armmotor.setMode(DcMotor.RunMode.RUN_USING_ENCODER);
    //armmotor.setMode(DcMotor.RunMode.RUN_TO_POSITION);
    telemetry.addData("Arm Motor init", "Arm Motor decoder: %d", armmotor.getCurrentPosition());
    telemetry.addData("Arm Motor init", "run mode: %s", armmotor.getMode().toString());


    //initialize wristPanServo and drone servo
    wristPanServo.setPosition(WRIST_PAN_SERVO_FOLDED);
    droneServo.setPosition(DRONE_POSITION_ARMED);
    hookServo.setPosition(HOOK_HIDE_POSITION);
    telemetry.addData("Wrist servo Position:", "%f", wristPanServo.getPosition());
    telemetry.addData("Dronw servo Position", "%f", droneServo.getPosition());
    telemetry.addData("Hook servo Position", "%f", hookServo.getPosition());

    //initialize both hand servos
    // Reverse Top Servo
    topArmServo.setDirection(Servo.Direction.REVERSE);
    topArmServo.setPosition(topServoClose);
    bottomArmServo.setPosition(bottomServoClose);
    telemetry.addData("top finger servo Position", "%f", topArmServo.getPosition());
    telemetry.addData("bottom finger servo Position", "%f", bottomArmServo.getPosition());

    //init imu
    IMU.Parameters parameters = new IMU.Parameters(new RevHubOrientationOnRobot(
      RevHubOrientationOnRobot.LogoFacingDirection.LEFT,
      RevHubOrientationOnRobot.UsbFacingDirection.UP
    ));
    imu.initialize(parameters);

    imu.resetYaw();

    telemetry.addData("Status", "Initialized");
    telemetry.update();

    // Wait for the game to start (driver presses PLAY)
    waitForStart();

    if (isStopRequested()) {
      return;
    }

    // run until the end of the match (driver presses STOP)
    while (opModeIsActive()) {

      previousGamepad1.copy(currentGamepad1);
      previousGamepad2.copy(currentGamepad2);

      currentGamepad1.copy(gamepad1);
      currentGamepad2.copy(gamepad2);

      //if (currentGamepad1.a && !previousGamepad1.a) {

        if (!robotHanging) {
        //Driving control from Gamepad 1
        //mecanum drive train
        lStickX = slow_mode * (gamepad1.left_stick_x * Math.abs(gamepad1.left_stick_x));
        lStickY = slow_mode * (-gamepad1.left_stick_y * Math.abs(gamepad1.left_stick_y));
        rStickX = slow_mode * (gamepad1.right_stick_x * Math.abs(gamepad1.right_stick_x));

        //If the field centric drive lost direction, push Back button to reset heading
        if (gamepad1.back) {
          imu.resetYaw();
        }

        botHeading = imu.getRobotYawPitchRollAngles().getYaw(AngleUnit.RADIANS);

        telemetry.addData("Stick Powers", ":lStickX: %.2f, lStickY: %.2f, RStickX:%.2f", lStickX, lStickY, rStickX);
        telemetry.addData("imu", "yaw: %.2f", botHeading);

        setMotorPowers(lStickX, lStickY, rStickX, botHeading);

        // Gamepad 2 controls everything but driving

        // Robot arm is controlled by the left stick y on Gamepad 2

        lStickY2 = -gamepad2.left_stick_y * Math.abs(gamepad2.left_stick_y);
        telemetry.addData("StickY2", "%.5f",lStickY2);
        if (lStickY2 < 0) {
          armmotor.setTargetPosition(ARM_PICKUP_POSITION);
          armmotor.setPower(Math.abs(lStickY2));
        } else if (lStickY2 > 0) {
          armmotor.setTargetPosition(ARM_MAX_POSITION);
          armmotor.setPower(Math.abs(lStickY2));
        }
        else {
          armmotor.setTargetPosition(armmotor.getCurrentPosition());
        }


        telemetry.addData("Arm Motor Position", "Arm Motor encoder: %d", armmotor.getCurrentPosition());
        telemetry.addData("Arm Motor Position", "run mode: %s", armmotor.getMode().toString());

        // dpad up set the arm back at 60 degree to the ground
        // dpad left set the arm at driving height
        // dpad down set the arm at pick up position
        /*if (armmotor.isBusy() && lStickY2 == 0) {
          telemetry.addData("Arm Motor test0", "Arm Motor decoder: %d", armmotor.getCurrentPosition());
          telemetry.addData("Arm Motor test0", "run mode: %s", armmotor.getMode().toString());
        } else if (armmotor.isBusy() && lStickY2 != 0) {
          armmotor.setMode(DcMotor.RunMode.RUN_WITHOUT_ENCODER);
          armmotor.setPower(lStickY2);
          telemetry.addData("Arm Motor test1", "Arm Motor decoder: %d", armmotor.getCurrentPosition());
          telemetry.addData("Arm Motor test1", "run mode: %s", armmotor.getMode().toString());
        } else if (!armmotor.isBusy()) {
          armmotor.setMode(DcMotor.RunMode.RUN_WITHOUT_ENCODER);
          armmotor.setPower(lStickY2);
          telemetry.addData("Arm Motor test2", "Arm Motor decoder: %d", armmotor.getCurrentPosition());
          telemetry.addData("Arm Motor test2", "run mode: %s", armmotor.getMode().toString());

        } else {
          //should never run here
          telemetry.addData("Arm Motor test3", "Arm Motor decoder: %d", armmotor.getCurrentPosition());
          telemetry.addData("Arm Motor test3", "run mode: %s", armmotor.getMode().toString());
        }*/

      /*if(gamepad2.dpad_up){
        setArmPosition(ARM_DEPLOY_POSITION, ARM_POWER);d
      }

      if(gamepad2.dpad_left) {
        setArmPosition(ARM_DRIVE_POSITION, ARM_POWER);
      }

      if(gamepad2.dpad_down){
        setArmPosition(ARM_PICKUP_POSITION, ARM_POWER);
      }*/

        //servo slot 0 & 1 are for the finger controls
        if (currentGamepad2.right_bumper && !previousGamepad2.right_bumper) {
          if (!topFingerServoOpen) {
            topArmServo.setPosition(topServoOpen);
            topFingerServoOpen = true;
          }
          else {
            topArmServo.setPosition(topServoClose);
            topFingerServoOpen = false;
          }
        }

        if (currentGamepad2.right_trigger > 0 && !(previousGamepad2.right_trigger > 0)) {
          if (!bottomFingerServoOpen) {
            bottomArmServo.setPosition(bottomServoOpen);
            bottomFingerServoOpen = true;
          }
          else {
            bottomArmServo.setPosition(bottomServoClose);
            bottomFingerServoOpen = false;
          }
        }

        if (currentGamepad2.left_bumper && !previousGamepad2.left_bumper) {
          if (!topFingerServoOpen) {
            topArmServo.setPosition(topServoOpen);
            topFingerServoOpen = true;
            bottomArmServo.setPosition(bottomServoOpen);
            bottomFingerServoOpen = true;
          } else {
            topArmServo.setPosition(topServoClose);
            topFingerServoOpen = false;
            bottomArmServo.setPosition(bottomServoClose);
            bottomFingerServoOpen = false;
          }
        }
        telemetry.addData("top finger servo Position", "%f", topArmServo.getPosition());
        telemetry.addData("bottom finger servo Position", "%f", bottomArmServo.getPosition());

        //wrist servo is in slot 2
        rStickY2 = -gamepad2.right_stick_y * Math.abs(gamepad2.right_stick_y);
        if (rStickY2 > 0) {
          wristPanPos += WRIST_PAN_SERVO_SPEED;
          if (wristPanPos > WRIST_PAN_SERVO_FOLDED) {
            wristPanPos = WRIST_PAN_SERVO_FOLDED;
          }
        }
        else if (rStickY2 < 0 ) {
          wristPanPos -= WRIST_PAN_SERVO_SPEED;
          if (wristPanPos < WRIST_PAN_SERVO_FLOOR) {
            wristPanPos = WRIST_PAN_SERVO_FLOOR;
          }
        }
        else {
          //do nothing when the stick is at 0 position
        }

        wristPanServo.setPosition(wristPanPos);
        telemetry.addData("Wrist Servos", "wristPanPos %.3f", wristPanServo.getPosition());

        //camera is servo in slot 3. Not implemented in TeleOp

        //airplane launcher servo is in slot 4
        if (currentGamepad2.y && !previousGamepad2.y) {
          if (droneServo.getPosition() == DRONE_POSITION_ARMED)
            droneServo.setPosition(DRONE_POSITION_LAUNCH);
          else
            droneServo.setPosition(DRONE_POSITION_ARMED);
        }
        telemetry.addData("Drone Servos", "%.3f", droneServo.getPosition());

        //hook servo is in slot 5
        if (currentGamepad2.x && !previousGamepad2.x) {
          if (hookServo.getPosition() == HOOK_HIDE_POSITION)
            hookServo.setPosition(HOOK_LAUNCH_POSITION);
          else
            hookServo.setPosition(HOOK_HIDE_POSITION);
        }
        telemetry.addData("Hook servo Position", ":%f", hookServo.getPosition());

        if (currentGamepad2.start && !previousGamepad2.start) {
          robotHanging = true;
          hookServo.getController().pwmDisable();
          armmotor.setTargetPosition(ARM_PICKUP_POSITION);
          armmotor.setMode(DcMotor.RunMode.RUN_TO_POSITION);
          armmotor.setPower(ARM_POWER);

          for (int i=0; i<5; i++) {
            while (armmotor.isBusy()) {
              sleep(10);
            }
          }
        }
        telemetry.addData("Servo Power", ":%s", hookServo.getController().getPwmStatus().toString());
        telemetry.update();
      }
      else {
        //robot is hanging
        if (currentGamepad2.start && !previousGamepad2.start) {
          robotHanging = false;
          hookServo.getController().pwmEnable();
          armmotor.setMode(DcMotor.RunMode.RUN_USING_ENCODER);
          armmotor.setPower(0);
        }
        telemetry.addData("Servo Power", ":%s", hookServo.getController().getPwmStatus().toString());
        telemetry.update();

      }
    }
  }
  public void setMotorPowers(double x, double y, double rx, double heading) {
    double rotX = x * Math.cos(-heading) - y * Math.sin(-heading);
    double rotY = x * Math.sin(-heading) + y * Math.cos(-heading);

    // put strafing factors here
    rotX = rotX * 1;
    rotY = rotY * 1;

    double denominator = Math.max(Math.abs(rotY) + Math.abs(rotX) + Math.abs(rx), 1);

    double frontLeftPower = (rotY + rotX + rx)/denominator;
    double backLeftPower = (rotY - rotX + rx)/denominator;
    double frontRightPower = (rotY - rotX - rx)/denominator;
    double backRightPower = (rotY + rotX - rx)/denominator;

    frontLeftMotor.setPower(frontLeftPower);
    backLeftMotor.setPower(backLeftPower);
    frontRightMotor.setPower(frontRightPower);
    backRightMotor.setPower(backRightPower);
  }
  public void setArmPosition(int position, double speed) {
    armmotor.setTargetPosition(position);
    armmotor.setMode(DcMotor.RunMode.RUN_TO_POSITION);
    armmotor.setPower(speed);

    for (int i=0; i<5; i++) {
      while (armmotor.isBusy()) {
        sleep(10);
      }
    }
  }

}
